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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
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
import org.apache.directory.scim.AttributeNotFoundException;
import org.apache.directory.scim.ComplexAttribute;
import org.apache.directory.scim.GroupResource;
import org.apache.directory.scim.ListResponse;
import org.apache.directory.scim.MissingParameterException;
import org.apache.directory.scim.MultiValAttribute;
import org.apache.directory.scim.ProviderService;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.ResourceConflictException;
import org.apache.directory.scim.ResourceNotFoundException;
import org.apache.directory.scim.ScimUtil;
import org.apache.directory.scim.ServerResource;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.SimpleAttributeGroup;
import org.apache.directory.scim.UserResource;
import org.apache.directory.scim.ldap.handlers.LdapAttributeHandler;
import org.apache.directory.scim.ldap.schema.ComplexType;
import org.apache.directory.scim.ldap.schema.GroupSchema;
import org.apache.directory.scim.ldap.schema.MultiValType;
import org.apache.directory.scim.ldap.schema.ResourceSchema;
import org.apache.directory.scim.ldap.schema.SimpleType;
import org.apache.directory.scim.ldap.schema.SimpleTypeGroup;
import org.apache.directory.scim.ldap.schema.UserSchema;
import org.apache.directory.scim.schema.BaseType;
import org.apache.directory.scim.schema.JsonSchema;
import org.apache.directory.scim.schema.SchemaUtil;
import org.apache.directory.scim.search.FilterNode;
import org.apache.directory.scim.search.FilterParser;
import org.apache.directory.scim.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
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
    
    private Map<String,JsonSchema> schemas = new HashMap<String, JsonSchema>();

    private static final Logger LOG = LoggerFactory.getLogger( LdapResourceProvider.class );

    private static final String ENTRYDN_HEADER = "X-ENTRYDN";

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
        
        try
        {
            List<URL> urls = SchemaUtil.getDefaultSchemas();
            for( URL u : urls )
            {
                JsonSchema json = SchemaUtil.getSchemaJson( u );
                schemas.put( json.getId(), json );
            }
            
            // TODO load custom schemas
        }
        catch( Exception e )
        {
            RuntimeException re = new RuntimeException( "Failed to load the default schemas" );
            re.initCause( e );
            throw re;
        }
        
        if ( connection == null )
        {
            createConnection();
        }

        if ( connection instanceof LdapNetworkConnection )
        {
            ( ( LdapNetworkConnection ) connection ).loadSchema( new JarLdifSchemaLoader() );
        }

        ldapSchema = connection.getSchemaManager();
        
        Map<String,JsonSchema> jsonSchemaCopy = new HashMap<String, JsonSchema>( schemas );
        schemaMapper = new LdapSchemaMapper( jsonSchemaCopy, ldapSchema );
        schemaMapper.loadMappings();
        userSchema = schemaMapper.getUserSchema();
        groupSchema = schemaMapper.getGroupSchema();
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


    public AttributeType getLdapType( String scimAtName, ResourceSchema schema )
    {
        scimAtName = scimAtName.trim();
        int colonPos = scimAtName.lastIndexOf( ":" );
        if( colonPos > 0 )
        {
            scimAtName = scimAtName.substring( colonPos + 1 );
        }

        
//        String schemaUri = scimAtName.substring( 0, colonPos );
//        if( schema == null )
//        {
//            throw new IllegalArgumentException( "No resource schema exists with the URI " + schemaUri );
//        }
        
        BaseType bt = schema.getAttribute( scimAtName );
        
        if ( bt instanceof SimpleType  )
        {
            SimpleType st = ( SimpleType ) bt;
            
            if( Strings.isNotEmpty( st.getMappedTo() ) )
            {
                return ldapSchema.getAttributeType( st.getMappedTo() );
            }
            else if ( st.getAtHandlerName() != null )
            {
                LdapAttributeHandler atHandler = ( LdapAttributeHandler ) schema.getHandler( st.getAtHandlerName() );
                return atHandler.getLdapAtType( st, "", schema, ldapSchema );
            }
        }
        else // a complex or multivalued attribute with a handler
        {
            int pos = scimAtName.indexOf( '.' );
            
            if( pos <= 0 )
            {
                return null;
            }
            
            bt = schema.getAttribute( scimAtName.substring( 0, pos ) );
            
            if( bt == null )
            {
                return null;
            }
            
            LdapAttributeHandler atHandler = ( LdapAttributeHandler ) schema.getHandler( bt.getAtHandlerName() );
            
            if( atHandler != null )
            {
                String remainingScimAttributePath = scimAtName.substring( pos + 1 );
                return atHandler.getLdapAtType( bt, remainingScimAttributePath, schema, ldapSchema );
            }
        }
        
        return null;
    }
    
    
    public ListResponse search( String scimFilter, String attributes, RequestContext ctx ) throws Exception
    {
        FilterNode filter = FilterParser.parse( scimFilter );
        
        String path = ctx.getUriInfo().getPath();
        String uri = ScimUtil.CORE_USER_URI;
        if( path.endsWith( "/Groups" ) )
        {
            uri = ScimUtil.CORE_GROUP_URI;
        }
        
        ResourceSchema scimSchema = schemaMapper.getSchemaWithUri( uri );
        
        ExprNode ldapFilter = LdapUtil._scimToLdapFilter( filter, scimSchema, ldapSchema, this );
        LOG.debug( "LDAP filter {}", ldapFilter );
        
        SearchRequest sr = new SearchRequestImpl();
        sr.setBase( new Dn( scimSchema.getBaseDn() ) );
        sr.setFilter( ldapFilter );
        sr.setScope( SearchScope.SUBTREE );
        
        List<String> ldapAtNames = new ArrayList<String>();
        ldapAtNames.add( SchemaConstants.ENTRY_UUID_AT );
        
        if( Strings.isNotEmpty( attributes ) )
        {
            String[] names = attributes.split( "," );
            for( String n : names )
            {
                AttributeType at = getLdapType( n, scimSchema );
                if( at != null )
                {
                    ldapAtNames.add( at.getName() );
                }
            }
        }
        
        sr.addAttributes( ldapAtNames.toArray( new String[1] ) );
        
        SearchCursor cursor = connection.search( sr );
        
        ListResponse lr = new ListResponse();
        
        while( cursor.next() )
        {
            Entry entry = cursor.getEntry();
            
            ServerResource res = null;
            
            if( uri.equals( ScimUtil.CORE_USER_URI ) )
            {
                res = new UserResource();
            }
            else
            {
                res = new GroupResource();
            }
            
            ctx.setCoreResource( res );

            _loadCoreResource( ctx, entry, scimSchema );

            lr.addResource( res );
        }
        
        cursor.close();
        
        return lr;
    }


    public UserResource getUser( RequestContext ctx, String id ) throws ResourceNotFoundException
    {
        Entry entry = fetchEntryById( id, userSchema );

        if ( entry == null )
        {
            throw new ResourceNotFoundException( "No UserResource resource found with the ID " + id );
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


    public UserResource putUser( String userId, String jsonData, RequestContext ctx ) throws Exception
    {
        return ( UserResource ) replaceResource( userId, jsonData, ctx, userSchema );
    }


    public GroupResource putGroup( String groupId, String jsonData, RequestContext ctx ) throws Exception
    {
        return ( GroupResource ) replaceResource( groupId, jsonData, ctx, groupSchema );
    }


    public UserResource patchUser( String userId, String jsonData, RequestContext ctx ) throws Exception
    {
        return ( UserResource ) patchResource( userId, jsonData, ctx, userSchema );
    }


    public GroupResource patchGroup( String groupId, String jsonData, RequestContext ctx ) throws Exception
    {
        return ( GroupResource ) patchResource( groupId, jsonData, ctx, groupSchema );
    }

    
    public InputStream getUserPhoto( String id, String atName ) throws MissingParameterException
    {
        if ( Strings.isEmpty( id ) )
        {
            throw new MissingParameterException( "parameter 'id' cannot be null or empty" );
        }

        if ( Strings.isEmpty( atName ) )
        {
            throw new MissingParameterException( "parameter 'atName' cannot be null or empty" );
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


    public GroupResource getGroup( RequestContext ctx, String groupId ) throws ResourceNotFoundException
    {
        Entry entry = fetchEntryById( groupId, groupSchema );

        if ( entry == null )
        {
            throw new ResourceNotFoundException( "No GroupResource resource found with the ID " + groupId );
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

    
    private void addAttributes( Entry entry, JsonObject obj, RequestContext ctx, ResourceSchema resourceSchema ) throws Exception
    {
        //obj.remove( "schemas" );
        
        for( java.util.Map.Entry<String, JsonElement> e : obj.entrySet() )
        {
            String name = e.getKey();
            
            if( name.startsWith( "urn:scim:schemas:" ) )
            {
                continue;
            }
            
            BaseType bt = resourceSchema.getAttribute( name );
            
            if( bt == null )
            {
                LOG.debug( "Unknown attribute name "  + name + " is present in the JSON payload that has no corresponding mapping in the escimo-ldap-mapping.xml file" );
                continue;
            }
            
            if( bt.isReadOnly() )
            {
                continue;
            }

            AttributeHandler handler = resourceSchema.getHandler( bt.getAtHandlerName() );
            
            if( handler != null )
            {
                handler.write( bt, e.getValue(), entry, ctx );
            }
            else
            {
                LdapUtil.scimToLdapAttribute( bt, e.getValue(), entry, ctx );
            }
        }
    }
    
    
    public UserResource addUser( String json, RequestContext ctx ) throws Exception
    {
        String userName = null;
        
        try
        {
            JsonParser parser = new JsonParser();
            JsonObject obj = ( JsonObject ) parser.parse( json );
            
            Entry entry = new DefaultEntry( ldapSchema );
         
            SimpleType st = ( SimpleType ) userSchema.getCoreAttribute( "userName" );
            String userIdName = st.getMappedTo();

            String dn = ctx.getHeaderValue( ENTRYDN_HEADER );
            
            if( Strings.isEmpty( dn ) )
            {
                dn = null;
            }
            
            if( dn == null )
            {
                userName = obj.get( "userName" ).getAsString();
                
                dn = userIdName + "=" + userName + "," + userSchema.getBaseDn();
            }
            
            _resourceToEntry( entry, obj, ctx, userSchema );
            
            entry.setDn( dn );
            connection.add( entry );

            entry = connection.lookup( entry.getDn(), SchemaConstants.ALL_ATTRIBUTES_ARRAY );

            UserResource addedUser = new UserResource();

            ctx.setCoreResource( addedUser );

            _loadCoreResource( ctx, entry, userSchema );
            
            return addedUser;

        }
        catch( LdapEntryAlreadyExistsException e )
        {
            String message = "Resource already exists, conflicting attribute userName : " + userName;
            throw new ResourceConflictException( message );
        }
        catch( Exception e )
        {
            LOG.warn( "Failed to create User resource", e );
            throw e;
        }
    }
    
    
    public GroupResource addGroup( String jsonData, RequestContext ctx ) throws Exception
    {
        String groupName = null;
        
        try
        {
            JsonParser parser = new JsonParser();
            JsonObject obj = ( JsonObject ) parser.parse( jsonData );
            
            Entry entry = new DefaultEntry( ldapSchema );
         
            SimpleType st = ( SimpleType ) groupSchema.getCoreAttribute( "displayName" );
            String groupNameAt = st.getMappedTo();

            String dn = ctx.getHeaderValue( ENTRYDN_HEADER );
            
            if( Strings.isEmpty( dn ) )
            {
                dn = null;
            }
            
            if( dn == null )
            {
                groupName = obj.get( "displayName" ).getAsString();
                
                dn = groupNameAt + "=" + groupName + "," + groupSchema.getBaseDn();
            }
            
            _resourceToEntry( entry, obj, ctx, groupSchema );
            
            entry.setDn( dn );
            
            connection.add( entry );
            
            entry = connection.lookup( entry.getDn(), SchemaConstants.ALL_ATTRIBUTES_ARRAY );

            GroupResource addedGroup = new GroupResource();

            ctx.setCoreResource( addedGroup );

            _loadCoreResource( ctx, entry, groupSchema );
            
            return addedGroup;

        }
        catch( LdapEntryAlreadyExistsException e )
        {
            String message = "Resource already exists, conflicting attribute displayName : " + groupName;
            throw new ResourceConflictException( message );
        }
        catch( Exception e )
        {
            LOG.warn( "Failed to create Group resource", e );
            throw e;
        }
    }


    private void _resourceToEntry( Entry entry, JsonObject obj, RequestContext ctx, ResourceSchema resourceSchema ) throws Exception
    {

        // add the objectClasses first so a handler will get a chance to
        // inspect what attributes can the entry hold
        // e.x it is useful for handling Groups, where the handler can
        // find if the attribute name is 'member' or 'uniqueMember'
        for( String oc : resourceSchema.getObjectClasses() )
        {
            entry.add( SchemaConstants.OBJECT_CLASS, oc );
        }

        // process the core attributes first
        addAttributes( entry, obj, ctx, resourceSchema );
        
        List<String> uris = resourceSchema.getUris();
        
        for( String u : uris )
        {
            JsonObject userAtObj = ( JsonObject ) obj.get( u );
            if( userAtObj != null )
            {
                addAttributes( entry, userAtObj, ctx, resourceSchema );
            }
        }

    }
    
    
    // TODO can userName be changed for a user?? likewise displayName for a Group
    public ServerResource replaceResource( String resourceId, String jsonData, RequestContext ctx, ResourceSchema resourceSchema ) throws Exception
    {
        JsonParser parser = new JsonParser();
        JsonObject obj = ( JsonObject ) parser.parse( jsonData );
        
        Entry entry = new DefaultEntry( ldapSchema );
        
        _resourceToEntry( entry, obj, ctx, resourceSchema );
        
        Entry existingEntry = fetchEntryById( resourceId, resourceSchema );
        
        // save a reference to the existing password attribute
        Attribute existingPwdAt = existingEntry.get( SchemaConstants.USER_PASSWORD_AT );
        Attribute newPwdAt = entry.get( SchemaConstants.USER_PASSWORD_AT );
        
        if( existingPwdAt != null )
        {
            existingEntry.remove( existingPwdAt );
        }
        
        if( newPwdAt != null )
        {
            entry.remove( newPwdAt );
        }
        
        Attribute existingUserNameAt = null;
        Attribute newUserNameAt = null;
        SimpleType st = null;
        
        if( resourceSchema == userSchema )
        {
            st = ( SimpleType ) resourceSchema.getAttribute( "userName" );
        }
        else if( resourceSchema == groupSchema )
        {
            st = ( SimpleType ) resourceSchema.getAttribute( "displayName" );
        }
        
        if( st != null )
        {
            existingUserNameAt = existingEntry.get( st.getMappedTo() );
            
            if( existingUserNameAt != null )
            {
                existingEntry.remove( existingUserNameAt );
            }
            
            newUserNameAt = entry.get( st.getMappedTo() );
            
            if( newUserNameAt != null )
            {
                entry.remove( newUserNameAt );
            }
        }
        
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( existingEntry.getDn() );
        
        Iterator<Attribute> itr = existingEntry.iterator();
        while( itr.hasNext() )
        {
            Attribute ldapAt = itr.next();
            
            AttributeType type = ldapAt.getAttributeType();
            
            if( !type.isUserModifiable() )
            {
                continue;
            }
            
            if( entry.containsAttribute( type ) )
            {
                ldapAt = entry.get( type );
                entry.removeAttributes( type );
                modReq.replace( ldapAt );
            }
            else
            {
                modReq.remove( ldapAt );
            }
        }
        
        // iterate over the remaining attributes of new entry and add them to modlist as 'add' modifications
        
        for( Attribute newAt : entry )
        {
            modReq.add( newAt );
        }
        
        if( newPwdAt != null )
        {
            if( existingPwdAt != null )
            {
                modReq.replace( newPwdAt );
            }
            else
            {
                modReq.add( newPwdAt );
            }
        }
        
        ModifyResponse modResp = connection.modify( modReq );
        
        if( modResp.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS )
        {
            throw new Exception( "Failed to replace the resource " + modResp.getLdapResult().getDiagnosticMessage() );
        }
        
        if( newUserNameAt != null )
        {
            if( !existingUserNameAt.contains( newUserNameAt.getString() ) )
            {
                // a modDN needs to be performed
                connection.rename( existingEntry.getDn().getName(), newUserNameAt.getUpId() + "=" + newUserNameAt.getString(), true );
            }
        }
        
        entry = fetchEntryById( resourceId, resourceSchema );
        
        ServerResource resource = null;
        
        if( resourceSchema == userSchema )
        {
            resource = new UserResource();
        }
        else
        {
            resource = new GroupResource();
        }

        ctx.setCoreResource( resource );

        _loadCoreResource( ctx, entry, resourceSchema );
        
        return resource;
    }
    
    
    public ServerResource patchResource( String resourceId, String jsonData, RequestContext ctx, ResourceSchema resourceSchema ) throws Exception
    {
        JsonParser parser = new JsonParser();
        JsonObject obj = ( JsonObject ) parser.parse( jsonData );

        Entry existingEntry = fetchEntryById( resourceId, resourceSchema );
        
        if( existingEntry == null )
        {
            throw new ResourceNotFoundException( "No resource found with the id " + resourceId );
        }
        
        
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( existingEntry.getDn() );
        
        boolean hasAttributesInMeta = false;
        
        JsonObject metaObj = ( JsonObject ) obj.get( "meta" );
        if( metaObj != null )
        {
            JsonArray metaAtNames = ( JsonArray ) metaObj.get( "attributes" );
            if( metaAtNames != null )
            {
                hasAttributesInMeta = true;
                
                for( JsonElement e : metaAtNames )
                {
                    String name = e.getAsString();
                    BaseType bt = resourceSchema.getAttribute( name );
                    
                    if( bt == null )
                    {
                        throw new AttributeNotFoundException( "No definition found for the attribute " + name );
                    }
                    
                    AttributeHandler handler = resourceSchema.getHandler( bt.getAtHandlerName() );
                    if( handler != null )
                    {
                        handler.deleteAttribute( bt, existingEntry, ctx, modReq );
                        continue;
                    }
                    
                    LdapUtil.deleteAttribute( bt, existingEntry, modReq );
                }
            }
        }

        try
        {
            LdapUtil.patchAttributes( existingEntry, obj, ctx, resourceSchema, modReq );
            
            ModifyResponse modResp = connection.modify( modReq );
            
            LdapResult result = modResp.getLdapResult();
            if( result.getResultCode() != ResultCodeEnum.SUCCESS )
            {
                throw new Exception( result.getDiagnosticMessage() );
            }
            
            if( hasAttributesInMeta )
            {
                Entry entry = fetchEntryById( resourceId, resourceSchema );
                
                ServerResource resource = null;
                
                if( resourceSchema == userSchema )
                {
                    resource = new UserResource();
                }
                else
                {
                    resource = new GroupResource();
                }

                ctx.setCoreResource( resource );

                _loadCoreResource( ctx, entry, resourceSchema );
                
                return resource;
            }
            
            return null;
        }
        catch( Exception e )
        {
            LOG.warn( "Failed to patch the resource with ID {}", resourceId, e );
            throw e;
        }
    }
    
    public UserResource toUser( RequestContext ctx, Entry entry ) throws Exception
    {
        UserResource user = new UserResource();

        ctx.setCoreResource( user );

        _loadCoreResource( ctx, entry, userSchema );

        return user;
    }

    
    public void deleteUser( String id ) throws Exception
    {
        deleteResource( id, userSchema );
    }
    
    public void deleteGroup( String id ) throws Exception
    {
        deleteResource( id, groupSchema );
    }
    
    
    private void deleteResource( String id, ResourceSchema schema ) throws LdapException
    {
        Entry entry = fetchEntryById( id, schema );
        connection.delete( entry.getDn() );
    }

    public GroupResource toGroup( RequestContext ctx, Entry entry ) throws Exception
    {
        GroupResource group = new GroupResource();
        ctx.setCoreResource( group );

        _loadCoreResource( ctx, entry, groupSchema );

        return group;
    }


    private void _loadCoreResource( RequestContext ctx, Entry entry, ResourceSchema resourceSchema ) throws Exception
    {
        ServerResource resource = ctx.getCoreResource();

        // first fill in the id, we need this for deriving location
        SimpleType idType = ( SimpleType ) resourceSchema.getCoreAttribute( "id" );
        SimpleAttribute idAttribute = getValueForSimpleType( idType, entry, ctx );
        resource.addAttribute( idType.getUri(), idAttribute );

        resource.setId( ( String ) idAttribute.getValue() );

        _loadAttributes( ctx, entry, resourceSchema.getCoreTypes(), idType );
        _loadAttributes( ctx, entry, resourceSchema.getExtendedTypes(), idType );
    }


    private void _loadAttributes( RequestContext ctx, Entry entry, Collection<BaseType> types, SimpleType idType ) throws Exception
    {
        ServerResource user = ctx.getCoreResource();

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
                    handler.read( ct, entry, ctx );
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
                    handler.read( bt, entry, ctx );
                    continue;
                }

                SimpleTypeGroup stg = mt.getAtGroup();
                if ( stg != null )
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


    private List<SimpleAttributeGroup> getValuesFor( SimpleTypeGroup stg, Entry entry )
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

        List<SimpleType> types = new ArrayList<SimpleType>( stg.getSubTypes() );
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


    public SimpleAttribute getValueForSimpleType( SimpleType st, Entry entry, RequestContext ctx ) throws Exception
    {
        String atHandler = st.getAtHandlerName();

        if ( atHandler != null )
        {
            AttributeHandler handler = userSchema.getHandler( atHandler );
            handler.read( st, entry, ctx );
            return null;
        }
        else
        {
            return getValueForSimpleType( st, entry );
        }
    }


    public SimpleAttribute getValueForSimpleType( SimpleType st, Entry entry )
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


    private Object getScimValFrom( Attribute at )
    {
        return getScimValFrom( at.get() );
    }


    private Object getScimValFrom( Value<?> ldapValue )
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
                return ResourceUtil.toScimDate( ldapValue.getString() );
            }
        }

        return ldapValue.getString();
    }


    public List<SimpleAttribute> getValuesInto( SimpleTypeGroup stg, Entry entry )
    {
        List<SimpleAttribute> lstAts = new ArrayList<SimpleAttribute>();

        for ( SimpleType st : stg.getSubTypes() )
        {
            SimpleAttribute at = getValueForSimpleType( st, entry );
            if ( at != null )
            {
                lstAts.add( at );
            }

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

    
    public JsonSchema getSchema( String uri )
    {
        return schemas.get( uri );
    }

    
    /**
     * @return the ldapSchema
     */
    public SchemaManager getLdapSchema()
    {
        return ldapSchema;
    }


    /**
     * @return the userSchema
     */
    public UserSchema getUserSchema()
    {
        return userSchema;
    }


    /**
     * @return the groupSchema
     */
    public GroupSchema getGroupSchema()
    {
        return groupSchema;
    }
    
    public static void main( String[] args ) throws Exception
    {
        LdapResourceProvider provider = new LdapResourceProvider();
        
        try
        {
            provider.init();
            
            FilterNode scimFilter = FilterParser.parse( "(userName eq x and ((userName gt xx-yy ) or (id eq y))) or userName eq \"true\"" );
            System.out.println("SCIM filter: " + scimFilter);
            
            ExprNode ldapFilter = LdapUtil._scimToLdapFilter( scimFilter, provider.getUserSchema(), provider.getLdapSchema(), provider );
            System.out.println("LDAP filter: " + ldapFilter);
        }
        finally
        {
            provider.connection.close();
        }
    }
}
