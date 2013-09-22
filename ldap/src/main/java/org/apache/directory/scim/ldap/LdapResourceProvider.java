/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.scim.ldap;


import static org.apache.directory.api.ldap.model.constants.SchemaConstants.ALL_ATTRIBUTES_ARRAY;
import static org.apache.directory.api.ldap.model.message.SearchScope.SUBTREE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaITImpl;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.LdapSyntax;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.SyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.GeneralizedTimeSyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.IntegerSyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.JavaByteSyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.JavaIntegerSyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.JavaLongSyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.JavaShortSyntaxChecker;
import org.apache.directory.api.ldap.schemaloader.JarLdifSchemaLoader;
import org.apache.directory.api.util.Base64;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.scim.AttributeHandler;
import org.apache.directory.scim.ComplexAttribute;
import org.apache.directory.scim.CoreResource;
import org.apache.directory.scim.Group;
import org.apache.directory.scim.MissingParameterException;
import org.apache.directory.scim.MultiValAttribute;
import org.apache.directory.scim.ProviderService;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.ResourceNotFoundException;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.SimpleAttributeGroup;
import org.apache.directory.scim.User;
import org.apache.directory.scim.ldap.schema.ComplexType;
import org.apache.directory.scim.ldap.schema.GroupSchema;
import org.apache.directory.scim.ldap.schema.MultiValType;
import org.apache.directory.scim.ldap.schema.ResourceSchema;
import org.apache.directory.scim.ldap.schema.SimpleType;
import org.apache.directory.scim.ldap.schema.SimpleTypeGroup;
import org.apache.directory.scim.ldap.schema.TypedType;
import org.apache.directory.scim.ldap.schema.UserSchema;
import org.apache.directory.scim.schema.BaseType;
import org.apache.directory.scim.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * TODO LdapResourceProvider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapResourceProvider implements ProviderService
{
    private LdapConnection connection;

    private LdapSchemaMapper schemaMapper;

    private UserSchema userSchema;

    private GroupSchema groupSchema;

    private SchemaManager ldapSchema;
    
    private static final Logger LOG = LoggerFactory.getLogger( LdapResourceProvider.class );


    public LdapResourceProvider()
    {
    }


    public LdapResourceProvider( LdapConnection connection )
    {
        this.connection = connection;
    }


    public void init() throws Exception
    {
        LOG.info( "Initializing LDAP resource provider" );
        if ( connection == null )
        {
            createConnection();
        }

        if ( connection instanceof LdapNetworkConnection )
        {
            ( ( LdapNetworkConnection ) connection ).loadSchema( new JarLdifSchemaLoader() );
        }

        schemaMapper = new LdapSchemaMapper();
        schemaMapper.loadMappings();
        userSchema = schemaMapper.getUserSchema();
        groupSchema = schemaMapper.getGroupSchema();
        ldapSchema = connection.getSchemaManager();
    }


    public void stop()
    {
        LOG.info( "Closing the LDAP server connection" );

        if ( connection != null )
        {
            try
            {
                connection.close();
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to close the LDAP server connection", e );
            }
        }
    }


    private void createConnection() throws IOException, LdapException
    {
        LOG.info( "Creating LDAP server connection" );

        InputStream in = this.getClass().getClassLoader().getResourceAsStream( "ldap-server.properties" );
        Properties prop = new Properties();
        prop.load( in );

        String host = prop.getProperty( "escimo.ldap.server.host" );
        String portVal = prop.getProperty( "escimo.ldap.server.port" );
        int port = Integer.parseInt( portVal );
        String user = prop.getProperty( "escimo.ldap.server.user" );
        String password = prop.getProperty( "escimo.ldap.server.password" );
        String tlsVal = prop.getProperty( "escimo.ldap.server.useTls" );

        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( host );
        config.setLdapPort( port );
        config.setUseTls( Boolean.parseBoolean( tlsVal ) );
        config.setName( user );
        config.setCredentials( password );

        connection = new LdapNetworkConnection( config );
        connection.bind();
    }


    public User getUser( RequestContext ctx, String id ) throws ResourceNotFoundException
    {
        Entry entry = fetchEntryById( id, userSchema );

        if ( entry == null )
        {
            throw new ResourceNotFoundException( "No User resource found with the ID " + id );
        }

        try
        {
            return toUser( ctx, entry );
        }
        catch ( Exception e )
        {
            throw new ResourceNotFoundException( e );
        }
    }


    @Override
    public InputStream getUserPhoto( String id, String atName ) throws MissingParameterException
    {
        if ( Strings.isEmpty( id ) )
        {
            throw new MissingParameterException( "id cannot be null or empty" );
        }

        if ( Strings.isEmpty( atName ) )
        {
            throw new MissingParameterException( "atName cannot be null or empty" );
        }

        Entry entry = fetchEntryById( id, userSchema );

        if ( entry == null )
        {
            return null;
        }

        Attribute phtoAt = entry.get( atName );

        if ( phtoAt == null )
        {
            return null;
        }

        ByteArrayInputStream bin = new ByteArrayInputStream( phtoAt.get().getBytes() );

        return bin;
    }


    @Override
    public Group getGroup( RequestContext ctx, String groupId ) throws ResourceNotFoundException
    {
        Entry entry = fetchEntryById( groupId, groupSchema );

        if ( entry == null )
        {
            throw new ResourceNotFoundException( "No Group resource found with the ID " + groupId );
        }

        try
        {
            return toGroup( ctx, entry );
        }
        catch ( Exception e )
        {
            throw new ResourceNotFoundException( e );
        }

    }

    
    public void addUser( String json )
    {
        JsonParser parser = new JsonParser();
        JsonObject obj = ( JsonObject ) parser.parse( json );
        
        List<String> uris = userSchema.getUris();
        for( String u : uris )
        {
            JsonObject userAtObj = ( JsonObject ) obj.get( u );
        }
    }

    private void addAttributes( Entry entry, JsonObject obj )
    {
        for( java.util.Map.Entry<String, JsonElement> e : obj.entrySet() )
        {
            String name = e.getKey();
            
            BaseType bt = userSchema.getAttribute( name );
            
            if( bt == null )
            {
                throw new IllegalArgumentException( "Unknown attribute name "  + name + " is present in the JSON payload" );
            }
            
            String value = e.getValue().getAsString();
        }
    }
    
    
    private void processAttributeData( BaseType bt, JsonElement el, Entry entry ) throws LdapException
    {
        if( bt instanceof SimpleType )
        {
            SimpleType st = ( SimpleType ) bt;
            String ldapAtName = st.getMappedTo();
            if( Strings.isEmpty( ldapAtName ) )
            {
                throw new IllegalArgumentException( "Attribute " + bt.getName() + " is not mapped to any LDAP attribute in the config" );
            }
            
            AttributeType ldapType = ldapSchema.getAttributeType( ldapAtName );
            
            Attribute ldapAt = entry.get( ldapType );
            if( ldapAt == null )
            {
                ldapAt = new DefaultAttribute( ldapAtName );
            }
            
            if( !ldapType.getSyntax().isHumanReadable() )
            {
                byte[] value = Base64.decode( el.getAsString().toCharArray() );
                ldapAt.add( value );
            }
            else
            {
                ldapAt.add( el.getAsString() );
            }
        }
    }
    
    public void addUser( User user, RequestContext ctx )
    {
        try
        {
            Entry entry = new DefaultEntry();
            
            for( String oc : userSchema.getObjectClasses() )
            {
                entry.add( SchemaConstants.OBJECT_CLASS, oc );
            }
            
            String dn = null;
            
            SimpleAttribute at = ( SimpleAttribute ) user.get( "userDn" );
            if( at != null )
            {
                dn = String.valueOf( at.getValue() );
                if( Strings.isEmpty( dn ) )
                {
                    dn = null;
                }
            }
            
            if( dn == null )
            {
                SimpleType st = ( SimpleType ) userSchema.getCoreAttribute( "userName" );
                String userIdName = st.getMappedTo();

                dn = userIdName + "=" + String.valueOf( user.getVal( "userName" ) ) + "," + userSchema.getBaseDn();
            }
            
            if( dn != null )
            {
                entry.setDn( dn );
            }
            
            
        }
        catch( LdapException e )
        {
            e.printStackTrace();
        }
    }
    
    public User toUser( RequestContext ctx, Entry entry ) throws Exception
    {
        User user = new User();

        ctx.setCoreResource( user );

        _loadCoreResource( ctx, entry, userSchema );

        return user;
    }


    public Group toGroup( RequestContext ctx, Entry entry ) throws Exception
    {
        Group group = new Group();
        ctx.setCoreResource( group );

        _loadCoreResource( ctx, entry, groupSchema );

        return group;
    }


    private void _loadCoreResource( RequestContext ctx, Entry entry, ResourceSchema resourceSchema ) throws Exception
    {
        CoreResource resource = ctx.getCoreResource();

        // first fill in the id, we need this for deriving location
        SimpleType idType = ( SimpleType ) resourceSchema.getCoreAttribute( "id" );
        SimpleAttribute idAttribute = getValueForSimpleType( idType, entry, ctx );
        resource.addAttribute( idType.getUri(), idAttribute );

        resource.setId( ( String ) idAttribute.getValue() );

        _loadAttributes( ctx, entry, resourceSchema.getCoreTypes(), idType );
        _loadAttributes( ctx, entry, resourceSchema.getExtendedTypes(), idType );
    }


    private void _loadAttributes( RequestContext ctx, Entry entry, Collection<BaseType> types, SimpleType idType )
        throws Exception
    {
        CoreResource user = ctx.getCoreResource();

        for ( BaseType bt : types )
        {
            if ( bt instanceof SimpleType )
            {
                SimpleType st = ( SimpleType ) bt;

                if ( !st.isShow() )
                {
                    continue;
                }

                // skip id attribute, it was already added above
                if ( st == idType )
                {
                    continue;
                }

                SimpleAttribute at = getValueForSimpleType( st, entry, ctx );
                if ( at != null )
                {
                    user.addAttribute( bt.getUri(), at );
                }
            }
            else if ( bt instanceof ComplexType )
            {
                ComplexType ct = ( ComplexType ) bt;

                if ( !ct.isShow() )
                {
                    continue;
                }

                String atHandler = ct.getAtHandlerName();

                if ( atHandler != null )
                {
                    AttributeHandler handler = userSchema.getHandler( atHandler );
                    handler.handle( ct, entry, ctx );
                    continue;
                }

                List<SimpleAttribute> lstAts = getValuesInto( ct.getAtGroup(), entry );

                if ( !lstAts.isEmpty() )
                {
                    ComplexAttribute cAt = new ComplexAttribute( ct.getName(), lstAts );
                    user.addAttribute( bt.getUri(), cAt );
                }
            }
            else if ( bt instanceof MultiValType )
            {
                MultiValType mt = ( MultiValType ) bt;

                if ( !mt.isShow() )
                {
                    continue;
                }

                String atHandler = bt.getAtHandlerName();

                if ( atHandler != null )
                {
                    AttributeHandler handler = userSchema.getHandler( atHandler );
                    handler.handle( bt, entry, ctx );
                    continue;
                }

                List<TypedType> typedList = mt.getTypedList();
                SimpleTypeGroup stg = mt.getStGroup();

                if ( typedList != null )
                {
                    MultiValAttribute mv = new MultiValAttribute( mt.getName() );

                    for ( TypedType tt : typedList )
                    {
                        SimpleTypeGroup typeStg = tt.getAtGroup();
                        List<SimpleAttribute> lstAts = getValuesInto( typeStg, entry );

                        if ( !lstAts.isEmpty() )
                        {
                            lstAts.add( new SimpleAttribute( "type", tt.getName() ) );
                            if ( tt.isPrimary() )
                            {
                                lstAts.add( new SimpleAttribute( "primary", true ) );
                            }
                            mv.addAtGroup( new SimpleAttributeGroup( lstAts ) );
                        }
                    }

                    if ( mv.getAtGroupList() != null )
                    {
                        user.addAttribute( bt.getUri(), mv );
                    }
                }
                else if ( stg != null )
                {
                    List<SimpleAttributeGroup> atGroupList = getValuesFor( stg, entry );

                    if ( atGroupList != null )
                    {
                        MultiValAttribute mv = new MultiValAttribute( mt.getName(), atGroupList );
                        user.addAttribute( bt.getUri(), mv );
                    }
                }
            }
        }
    }


    private List<SimpleAttributeGroup> getValuesFor( SimpleTypeGroup stg, Entry entry ) throws LdapException
    {
        if ( stg == null )
        {
            return null;
        }

        SimpleType valType = stg.getValueType();

        if ( valType == null )
        {
            return null;
        }

        List<SimpleType> types = new ArrayList<SimpleType>( stg.getLstSTypes() );
        types.remove( valType );

        Attribute ldapAt = entry.get( valType.getMappedTo() );

        if ( ldapAt == null )
        {
            return null;
        }

        List<SimpleAttributeGroup> lst = new ArrayList<SimpleAttributeGroup>();

        Iterator<Value<?>> itr = ldapAt.iterator();

        while ( itr.hasNext() )
        {
            Value<?> val = itr.next();

            Object scimVal = getScimValFrom( val );
            SimpleAttributeGroup sg = new SimpleAttributeGroup();
            sg.addAttribute( new SimpleAttribute( valType.getName(), scimVal ) );

            for ( SimpleType type : types )
            {
                SimpleAttribute st = getValueForSimpleType( type, entry );

                if ( st != null )
                {
                    sg.addAttribute( st );
                }
            }

            lst.add( sg );
        }

        if ( lst.isEmpty() )
        {
            return null;
        }

        return lst;
    }


    public SimpleAttribute getValueForSimpleType( SimpleType st, Entry entry, RequestContext ctx ) throws LdapException
    {
        String atHandler = st.getAtHandlerName();

        if ( atHandler != null )
        {
            AttributeHandler handler = userSchema.getHandler( atHandler );
            handler.handle( st, entry, ctx );
            return null;
        }
        else
        {
            return getValueForSimpleType( st, entry );
        }
    }


    public SimpleAttribute getValueForSimpleType( SimpleType st, Entry entry ) throws LdapException
    {
        String name = st.getName();
        Attribute at = entry.get( st.getMappedTo() );
        if ( at != null )
        {
            LOG.debug( "processing attribute {}", name );
            Object value = getScimValFrom( at );
            return new SimpleAttribute( name, value );
        }

        return null;
    }


    private Object getScimValFrom( Attribute at ) throws LdapException
    {
        return getScimValFrom( at.get() );
    }


    private Object getScimValFrom( Value<?> ldapValue ) throws LdapException
    {
        if ( !ldapValue.isHumanReadable() )
        {
            return new String( Base64.encode( ldapValue.getBytes() ) );
        }

        LdapSyntax syntax = ldapValue.getAttributeType().getSyntax();
        if ( syntax != null )
        {
            SyntaxChecker sc = syntax.getSyntaxChecker();
            if ( sc instanceof IntegerSyntaxChecker ||
                sc instanceof JavaByteSyntaxChecker ||
                sc instanceof JavaIntegerSyntaxChecker ||
                sc instanceof JavaLongSyntaxChecker ||
                sc instanceof JavaShortSyntaxChecker )
            {
                return Long.parseLong( ldapValue.getString() );
            }
            else if ( sc instanceof GeneralizedTimeSyntaxChecker )
            {
                return ResourceUtil.formatDate( ldapValue.getString() );
            }
        }

        return ldapValue.getString();
    }


    //    public List<SimpleAttribute> getValuesInto( SimpleTypeGroup stg, RequestContext ctx ) throws LdapException
    //    {
    //        
    //    }

    public List<SimpleAttribute> getValuesInto( SimpleTypeGroup stg, Entry entry ) throws LdapException
    {
        List<SimpleAttribute> lstAts = new ArrayList<SimpleAttribute>();

        // format="$givenName $familyName"
        boolean hasFormat = !Strings.isEmpty( stg.getFormat() );

        String format = stg.getFormat();

        for ( SimpleType st : stg.getLstSTypes() )
        {
            SimpleAttribute at = getValueForSimpleType( st, entry );
            if ( at != null )
            {
                lstAts.add( at );

                if ( hasFormat )
                {
                    format = format.replaceAll( "\\$" + st.getName(), String.valueOf( at.getValue() ) );
                }
            }

        }

        if ( hasFormat )
        {
            SimpleAttribute atFormat = new SimpleAttribute( "formatted", format );
            lstAts.add( atFormat );
        }

        return lstAts;

    }


    public LdapConnection getConnection()
    {
        return connection;
    }


    public Entry fetchEntryByDn( String dn )
    {
        try
        {
            return connection.lookup( dn, ALL_ATTRIBUTES_ARRAY );
        }
        catch ( LdapException e )
        {
            LOG.debug( "Couldn't find the entry with dn {}", dn, e );
        }

        return null;
    }


    public Entry fetchEntryById( String id, ResourceSchema resourceSchema )
    {
        EntryCursor cursor = null;

        SimpleType st = ( SimpleType ) resourceSchema.getCoreAttribute( "id" );
        String resourceIdName = st.getMappedTo();

        String filter = "(" + resourceIdName + "=" + id + ")";

        Entry entry = null;

        try
        {
            cursor = connection.search( resourceSchema.getBaseDn(), filter, SUBTREE, ALL_ATTRIBUTES_ARRAY );

            if ( cursor.next() )
            {
                entry = cursor.get();
            }
        }
        catch ( Exception e )
        {
            LOG.debug( "Failed while fetching the entry by id {}", id, e );
        }
        finally
        {
            cursor.close();
        }

        return entry;
    }


    public static void main( String[] args ) throws Exception
    {
//        System.setProperty( StandaloneLdapApiService.CONTROLS_LIST,
//            "org.apache.directory.api.ldap.codec.controls.cascade.CascadeFactory," +
//                "org.apache.directory.api.ldap.codec.controls.manageDsaIT.ManageDsaITFactory," +
//                "org.apache.directory.api.ldap.codec.controls.search.entryChange.EntryChangeFactory," +
//                "org.apache.directory.api.ldap.codec.controls.search.pagedSearch.PagedResultsFactory," +
//                "org.apache.directory.api.ldap.codec.controls.search.persistentSearch.PersistentSearchFactory," +
//                "org.apache.directory.api.ldap.codec.controls.search.subentries.SubentriesFactory" );

        LdapNetworkConnection c = new LdapNetworkConnection( "localhost", 10389 );
        c.setTimeOut( Long.MAX_VALUE );
        c.bind( "cn=mta,dc=example,dc=com", "secret" );
        c.loadSchema();
        //c.loadSchema( new JarLdifSchemaLoader() );

        ManageDsaITImpl managedsa = new ManageDsaITImpl();
        SearchRequest req = new SearchRequestImpl();
        req.addControl( managedsa );

        //EntryCursor cursor = c.search( "", "(entryUUID=7ca31977-ba2d-4cdc-a86d-ba9fba06cd15)", SearchScope.SUBTREE, "*" );
        EntryCursor cursor = c.search( "dc=example,dc=com", "(objectClass=*)", SearchScope.SUBTREE, "*" );
        System.out.println("searching");

        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            System.out.println( entry );
        }

        cursor.close();
        c.close();
    }
}
