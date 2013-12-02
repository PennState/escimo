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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
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
import org.apache.directory.scim.UnauthorizedException;
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

    private LdapConnection adminConnection;

    private LdapSchemaMapper schemaMapper;

    private UserSchema userSchema;

    private GroupSchema groupSchema;

    private SchemaManager ldapSchema;
    
    private LdapConnectionConfig config;
    
    private Map<String,JsonSchema> schemas = new HashMap<String, JsonSchema>();

    private static final Logger LOG = LoggerFactory.getLogger( LdapResourceProvider.class );

    private static final String ENTRYDN_HEADER = "X-ENTRYDN";

    private final Map<String, ConnectionSession> connMap = new ConcurrentHashMap<String, ConnectionSession>();
    
    private boolean allowAuthorizedUsers = false;
    
    private boolean initialized = false;

    private volatile boolean stop;
    
    private long sessionTimeout = 2 * 60 * 1000;
    
    public LdapResourceProvider()
    {
    }


    public LdapResourceProvider( LdapConnection connection )
    {
        this.adminConnection = connection;
    }


    public void init() throws Exception
    {
        LOG.info( "Initializing LDAP resource provider" );
        
        try
        {
            String jsonSchemaDir = System.getProperty( "escimo.json.schema.dir", null );
            
            File schemaDir = new File( jsonSchemaDir );
            
            List<URL> urls = SchemaUtil.getSchemas( schemaDir );
            
            if( urls.isEmpty() )
            {
                LOG.info( "No schemas found at {} , extracting and loading the default schemas", jsonSchemaDir );
                schemas = SchemaUtil.storeDefaultSchemas( schemaDir );
            }
            else
            {
                for( URL u : urls )
                {
                    JsonSchema json = SchemaUtil.getSchemaJson( u );
                    schemas.put( json.getId(), json );
                }
            }
        }
        catch( Exception e )
        {
            RuntimeException re = new RuntimeException( "Failed to load the default schemas" );
            re.initCause( e );
            throw re;
        }
        
        Runnable r = new Runnable() 
        {
            public void run() 
            {
                List<String> keys = new ArrayList<String>();
                
                while( !stop )
                {
                    long now = System.currentTimeMillis();
                    
                    for( String key : connMap.keySet() )
                    {
                        ConnectionSession cs = connMap.get( key );
                        
                        if( ( now - cs.lastAccessed ) >= sessionTimeout )
                        {
                            try
                            {
                                LOG.debug( "Closing an inactive connection associated with the userDn {} and key {}", cs.userDn, key );
                                
                                keys.add( key );
                                
                                cs.connection.unBind();
                                cs.connection.close();
                            }
                            catch( Exception e )
                            {
                                //ignore
                                LOG.info( "Errors occurred while unbinding and closing an inactive connection", e );
                            }
                        }
                    }
                    
                    for( String k : keys )
                    {
                        connMap.remove( k );
                    }
                    
                    keys.clear();
                    
                    try
                    {
                        Thread.sleep( 60 * 1000 );
                    }
                    catch( InterruptedException e )
                    {
                        // ignore
                        LOG.warn( "Connection cleaner thread was interrupted", e );
                    }
                }
            }
        };
        
        Thread connCleaner = new Thread( r );
        connCleaner.start();
    }

    
    public RequestContext createCtx( UriInfo uriInfo, HttpServletRequest httpReq ) throws Exception
    {
        LdapConnection connection = getConnection(httpReq);
        LdapRequestContext ctx = new LdapRequestContext(this, connection, uriInfo, httpReq );
        return ctx;
    }


    private void _initInternal() throws Exception
    {
        if( initialized )
        {
            return;
        }

        if ( ( adminConnection == null ) || 
            ( ! ( adminConnection.isAuthenticated() || adminConnection.isConnected() ) ) )
        {
            createConnection();
        }

        if ( adminConnection instanceof LdapNetworkConnection )
        {
            ( ( LdapNetworkConnection ) adminConnection ).loadSchema();// new JarLdifSchemaLoader() );
        }

        ldapSchema = adminConnection.getSchemaManager();
        
        Map<String,JsonSchema> jsonSchemaCopy = new HashMap<String, JsonSchema>( schemas );
        schemaMapper = new LdapSchemaMapper( jsonSchemaCopy, ldapSchema );
        schemaMapper.loadMappings();
        userSchema = schemaMapper.getUserSchema();
        groupSchema = schemaMapper.getGroupSchema();
        
        initialized = true;
    }

    
    public void stop()
    {
        LOG.info( "Closing the LDAP server connection" );

        stop = true;
        
        if ( adminConnection != null )
        {
            try
            {
                adminConnection.close();
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

        String configDir = System.getProperty( "escimo.config.dir" );
        
        File ldapServerProps = new File( new File( configDir ), "ldap-server.properties" );
        
        Properties prop = null;
        InputStream in = null;
        
        if( !ldapServerProps.exists() )
        {
            in = this.getClass().getClassLoader().getResourceAsStream( ldapServerProps.getName() );
            FileWriter fw = new FileWriter( ldapServerProps );
            
            BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
            
            String s = null;
            
            while( ( s = br.readLine() ) != null )
            {
                fw.write( s + "\n" );
            }
            
            fw.close();
            br.close();
        }

        in = new FileInputStream( ldapServerProps );
        
        prop = new Properties();
        prop.load( in );
        
        in.close();
        
        String host = prop.getProperty( "escimo.ldap.server.host" );
        String portVal = prop.getProperty( "escimo.ldap.server.port" );
        int port = Integer.parseInt( portVal );
        String user = prop.getProperty( "escimo.ldap.server.user" );
        String password = prop.getProperty( "escimo.ldap.server.password" );
        String tlsVal = prop.getProperty( "escimo.ldap.server.useTls" );

        config = new LdapConnectionConfig();
        config.setLdapHost( host );
        config.setLdapPort( port );
        config.setUseTls( Boolean.parseBoolean( tlsVal ) );
        config.setName( user );
        config.setCredentials( password );

        adminConnection = new LdapNetworkConnection( config );
        adminConnection.bind();
    }


    public String authenticate( String userName, String password ) throws Exception
    {
        _initInternal();
        
        if( ( userName == null ) || ( password == null ) )
        {
            LOG.debug( "Missing username and/or password" );
            return null;
        }
        
        LOG.debug( "Authenticating user {}", userName );
        
        String userDn = null;
        SimpleType st = ( SimpleType ) userSchema.getAttribute( "userName" );
        
        String filter = "(" + st.getMappedTo() + "=" + userName + ")";
        
        EntryCursor cursor = null;
        
        try
        {
            cursor = adminConnection.search( userSchema.getBaseDn(), filter, SUBTREE, "1.1" );

            if ( cursor.next() )
            {
                userDn = cursor.get().getDn().getName();
            }
        }
        finally
        {
            if( cursor != null )
            {
                cursor.close();
            }
        }

        if( userDn == null )
        {
            // do not reveal that the user does not exist
            throw new UnauthorizedException( "Cannot authenticate user " + userName );
        }
        
        LdapConnection conn = new LdapNetworkConnection( config );
        try
        {
            conn.bind( userDn, password );
        }
        catch( LdapAuthenticationException e )
        {
            UnauthorizedException ue = new UnauthorizedException( "Cannot authenticate user " + userName + " : " + e.getMessage() );
            ue.initCause( e );
            throw ue;
        }
        
        conn.setSchemaManager( ldapSchema );
        
        String sessionId = UUID.randomUUID().toString();
        
        connMap.put( sessionId, new ConnectionSession( conn, userDn ) );
        
        return sessionId;
    }


    public List<AttributeType> getLdapTypes( String scimAtName, ResourceSchema schema )
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
                return Collections.singletonList( ldapSchema.getAttributeType( st.getMappedTo() ) );
            }
            else if ( st.getAtHandlerName() != null )
            {
                LdapAttributeHandler atHandler = ( LdapAttributeHandler ) schema.getHandler( st.getAtHandlerName() );
                return atHandler.getLdapAtTypes( st, "", schema, ldapSchema );
            }
        }
        else if ( bt != null )// a complex or multivalued attribute with a handler
        {
            int pos = scimAtName.indexOf( '.' );
            
            String remainingScimAttributePath = null;
            if( pos > 0 )
            {
                remainingScimAttributePath = scimAtName.substring( pos + 1 );
            }
            
            LdapAttributeHandler atHandler = ( LdapAttributeHandler ) schema.getHandler( bt.getAtHandlerName() );
            
            if( atHandler != null )
            {
                return atHandler.getLdapAtTypes( bt, remainingScimAttributePath, schema, ldapSchema );
            }
            else
            {
                SimpleTypeGroup stg = null;
                
                if( bt instanceof ComplexType )
                {
                    stg = ( ( ComplexType ) bt ).getAtGroup();
                }
                else if( bt instanceof MultiValType )
                {
                    stg = ( ( MultiValType ) bt ).getAtGroup();
                }
                
                if( stg != null )
                {
                    List<AttributeType> atList = new ArrayList<AttributeType>();
                    
                    for( SimpleType st : stg.getSubTypes() )
                    {
                        if( Strings.isNotEmpty( st.getMappedTo() ) )
                        {
                            atList.add( ldapSchema.getAttributeType( st.getMappedTo() ) );
                        }
                    }
                    
                    return atList;
                }
            }
        }
        
        return null;
    }
    
    
    public ListResponse search( String scimFilter, String attributes, RequestContext ctx ) throws Exception
    {
        FilterNode filter = FilterParser.parse( scimFilter );
        
        String path = ctx.getUriInfo().getPath();
        String uri = ScimUtil.CORE_USER_URI;
        
        if( path.endsWith( "Groups" ) || path.endsWith( "Groups/" ) )
        {
            uri = ScimUtil.CORE_GROUP_URI;
        }
        
        ResourceSchema scimSchema = schemaMapper.getSchemaWithUri( uri );
        
        ExprNode ldapFilter = null;
        
        if ( filter != null )
        {
            ldapFilter = LdapUtil._scimToLdapFilter( filter, scimSchema, ldapSchema, this );
        }
        else
        {
            ldapFilter = org.apache.directory.api.ldap.model.filter.FilterParser.parse( scimSchema.getFilter() );
        }
        
        LOG.debug( "LDAP filter {}", ldapFilter );
        
        SearchRequest sr = new SearchRequestImpl();
        sr.setBase( new Dn( scimSchema.getBaseDn() ) );
        sr.setFilter( ldapFilter );
        sr.setScope( SearchScope.SUBTREE );
        
        String[] requested = getRequestedAttributes( attributes, scimSchema );
        sr.addAttributes( requested );
        
        LdapConnection conn = ( ( LdapRequestContext ) ctx ).getConnection();
        
        SearchCursor cursor = conn.search( sr );
        
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

    
    private String[] getRequestedAttributes( String attributes, ResourceSchema scimSchema )
    {
        List<String> ldapAtNames = new ArrayList<String>();
        ldapAtNames.add( SchemaConstants.ENTRY_UUID_AT );
        
        if( Strings.isNotEmpty( attributes ) )
        {
            String[] names = attributes.split( "," );
            for( String n : names )
            {
                List<AttributeType> atList = getLdapTypes( n, scimSchema );
                if( atList != null )
                {
                    for( AttributeType at : atList )
                    {
                        ldapAtNames.add( at.getName() );
                    }
                }
            }
            
            return ldapAtNames.toArray( new String[1] );
        }
        
        return ALL_ATTRIBUTES_ARRAY;
    }

    public UserResource getUser( RequestContext ctx, String id ) throws ResourceNotFoundException
    {
        Entry entry = fetchEntryById( id, userSchema, ctx );

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

    
    public InputStream getUserPhoto( String id, String atName, RequestContext ctx ) throws MissingParameterException
    {
        if ( Strings.isEmpty( id ) )
        {
            throw new MissingParameterException( "parameter 'id' cannot be null or empty" );
        }

        if ( Strings.isEmpty( atName ) )
        {
            throw new MissingParameterException( "parameter 'atName' cannot be null or empty" );
        }

        Entry entry = fetchEntryById( id, userSchema, ctx );

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
        Entry entry = fetchEntryById( groupId, groupSchema, ctx );

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

            String dn = ctx.getReqHeaderValue( ENTRYDN_HEADER );
            
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
            
            LdapConnection conn = ( ( LdapRequestContext ) ctx ).getConnection();
            
            conn.add( entry );

            entry = conn.lookup( entry.getDn(), SchemaConstants.ALL_ATTRIBUTES_ARRAY );

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

            String dn = ctx.getReqHeaderValue( ENTRYDN_HEADER );
            
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
            
            LdapConnection conn = ( ( LdapRequestContext ) ctx ).getConnection();
            
            conn.add( entry );
            
            entry = conn.lookup( entry.getDn(), SchemaConstants.ALL_ATTRIBUTES_ARRAY );

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
        
        Entry existingEntry = fetchEntryById( resourceId, resourceSchema, ctx );
        
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
        
        LdapConnection conn = ( ( LdapRequestContext ) ctx ).getConnection();
        
        ModifyResponse modResp = conn.modify( modReq );
        
        if( modResp.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS )
        {
            throw new Exception( "Failed to replace the resource " + modResp.getLdapResult().getDiagnosticMessage() );
        }
        
        if( newUserNameAt != null )
        {
            if( !existingUserNameAt.contains( newUserNameAt.getString() ) )
            {
                // a modDN needs to be performed
                conn.rename( existingEntry.getDn().getName(), newUserNameAt.getUpId() + "=" + newUserNameAt.getString(), true );
            }
        }
        
        entry = fetchEntryById( resourceId, resourceSchema, ctx );
        
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

        Entry existingEntry = fetchEntryById( resourceId, resourceSchema, ctx );
        
        if( existingEntry == null )
        {
            throw new ResourceNotFoundException( "No resource found with the id " + resourceId );
        }
        
        
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( existingEntry.getDn() );
        
        JsonObject metaObj = ( JsonObject ) obj.get( "meta" );
        if( metaObj != null )
        {
            JsonArray metaAtNames = ( JsonArray ) metaObj.get( "attributes" );
            if( metaAtNames != null )
            {
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

        LdapConnection conn = ( ( LdapRequestContext ) ctx ).getConnection();
        
        try
        {
            LdapUtil.patchAttributes( existingEntry, obj, ctx, resourceSchema, modReq );
            
            ModifyResponse modResp = conn.modify( modReq );
            
            LdapResult result = modResp.getLdapResult();
            if( result.getResultCode() != ResultCodeEnum.SUCCESS )
            {
                throw new Exception( result.getDiagnosticMessage() );
            }
            
            // send attributes if requested
            if( ctx.getParamAttributes() != null )
            {
                Entry entry = fetchEntryById( resourceId, resourceSchema, ctx );
                
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

    
    public void deleteUser( String id, RequestContext ctx ) throws Exception
    {
        deleteResource( id, userSchema, ctx );
    }
    
    public void deleteGroup( String id, RequestContext ctx ) throws Exception
    {
        deleteResource( id, groupSchema, ctx );
    }
    
    
    private void deleteResource( String id, ResourceSchema schema, RequestContext ctx ) throws LdapException
    {
        Entry entry = fetchEntryById( id, schema, ctx );
        LdapConnection conn = ( ( LdapRequestContext ) ctx ).getConnection();
        conn.delete( entry.getDn() );
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


    public Entry fetchEntryByDn( String dn, RequestContext ctx )
    {
        try
        {
            LdapConnection conn = ( ( LdapRequestContext ) ctx ).getConnection();
            
            return conn.lookup( dn, ALL_ATTRIBUTES_ARRAY );
        }
        catch ( LdapException e )
        {
            LOG.debug( "Couldn't find the entry with dn {}", dn, e );
        }

        return null;
    }

    public Entry fetchEntryById( String id, ResourceSchema resourceSchema, RequestContext ctx )
    {
        EntryCursor cursor = null;

        SimpleType st = ( SimpleType ) resourceSchema.getCoreAttribute( "id" );
        String resourceIdName = st.getMappedTo();

        String filter = "(" + resourceIdName + "=" + id + ")";

        Entry entry = null;

        String[] attributes = ALL_ATTRIBUTES_ARRAY;
        
        if( ctx != null )
        {
            attributes = getRequestedAttributes( ctx.getParamAttributes(), resourceSchema );
        }
        
        LdapConnection conn = ( ( LdapRequestContext ) ctx ).getConnection();
        
        try
        {
            cursor = conn.search( resourceSchema.getBaseDn(), filter, SUBTREE, attributes );

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
    
    
    public LdapConnection getConnection( HttpServletRequest httpReq ) throws Exception
    {
        
        if( allowAuthorizedUsers )
        {
            ConnectionSession cs = connMap.get( httpReq.getHeader( RequestContext.USER_AUTH_HEADER ) );
            
            if( cs == null )
            {
                throw new UnauthorizedException( "Not Authenticated" );
            }
            
            cs.touch();
            
            return cs.connection;
        }
        
        _initInternal();
        
        return adminConnection;
    }
    
    /**
     * @return the allowAuthorizedUsers
     */
    public boolean isAllowAuthorizedUsers()
    {
        return allowAuthorizedUsers;
    }


    /**
     * @param allowAuthorizedUsers the allowAuthorizedUsers to set
     */
    public void setAllowAuthorizedUsers( boolean allowAuthorizedUsers )
    {
        this.allowAuthorizedUsers = allowAuthorizedUsers;
    }

    class ConnectionSession
    {
        private String userDn;
        
        private LdapConnection connection;
        
        private long lastAccessed;
        
        public ConnectionSession( LdapConnection connection, String userDn )
        {
            this.userDn = userDn;
            this.connection = connection;
            touch();
        }
        
        public void touch()
        {
            lastAccessed = System.currentTimeMillis();
        }
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
            provider.adminConnection.close();
        }
    }
}
