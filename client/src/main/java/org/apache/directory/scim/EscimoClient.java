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
package org.apache.directory.scim;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.scim.schema.CoreResource;
import org.apache.directory.scim.schema.ErrorResponse;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * TODO EscimoClient.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EscimoClient
{
    private String providerUrl;

    private static String USERS_URI = "/Users";

    private static String GROUPS_URI = "/Groups";

    private Gson serializer;

    private Map<String,Class<? extends CoreResource>> uriClassMap;
    
    private static final Logger LOG = LoggerFactory.getLogger( EscimoClient.class );


    public EscimoClient( String providerUrl, Map<String,Class<? extends CoreResource>> uriClassMap )
    {
        this.providerUrl = providerUrl;
        this.uriClassMap = uriClassMap;
        
        GsonBuilder gb = new GsonBuilder();
        gb.setExclusionStrategies( new FieldExclusionStrategy() );
        gb.setDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );
        serializer = gb.create();
    }

    public EscimoResult addUser( CoreResource resource )
    {
        return addResource( resource, USERS_URI );
    }

    
    public EscimoResult addGroup( CoreResource resource )
    {
        return addResource( resource, GROUPS_URI );
    }

    
    public EscimoResult getUser( String id )
    {
        return getResource( id, USERS_URI );
    }

    
    public EscimoResult getGroup( String id )
    {
        return getResource( id, GROUPS_URI );
    }

    public EscimoResult deleteUser( String id )
    {
        return deleteResource( id, USERS_URI );
    }

    public EscimoResult deleteGroup( String id )
    {
        return deleteResource( id, GROUPS_URI );
    }

    public EscimoResult putUser( String userId, CoreResource resource )
    {
        return putResource( userId, resource, USERS_URI );
    }

    
    public EscimoResult putGroup( String groupId, CoreResource resource )
    {
        return putResource( groupId, resource, GROUPS_URI );
    }

    public EscimoResult patchUser( String userId, CoreResource resource )
    {
        return patchResource( userId, resource, USERS_URI );
    }

    
    public EscimoResult patchGroup( String groupId, CoreResource resource )
    {
        return patchResource( groupId, resource, GROUPS_URI );
    }

    
    private EscimoResult deleteResource( String id, String uri )
    {

        if ( id == null )
        {
            throw new IllegalArgumentException( "resource ID cannot be null" );
        }

        HttpDelete delete = new HttpDelete( providerUrl + uri + "/" + id );
        
        LOG.debug( "Trying to delete resource with ID {} at URI {}", id, uri );

        HttpClient client = HttpClients.createDefault();

        try
        {
            HttpResponse resp = client.execute( delete );
            StatusLine sl = resp.getStatusLine();
            
            EscimoResult result = new EscimoResult( sl.getStatusCode(), resp.getAllHeaders() );
            
            if ( sl.getStatusCode() == 200 )
            {
                return result;
            }
            else
            {
                String retVal = EntityUtils.toString( resp.getEntity() );
                result.setErrorResponse( deserializeError( retVal ) );
            }
            
            return result;
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed while deleting the resource at {}", delete.getURI() );
            throw new RuntimeException( e );
        }
    }
    
    
    private EscimoResult getResource( String id, String uri )
    {
        if ( id == null )
        {
            throw new IllegalArgumentException( "resource ID cannot be null" );
        }

        HttpGet get = new HttpGet( providerUrl + uri + "/" + id );
        
        LOG.debug( "Trying to retrieve resource with ID {} at URI {}", id, uri );

        HttpClient client = HttpClients.createDefault();

        try
        {
            HttpResponse resp = client.execute( get );
            StatusLine sl = resp.getStatusLine();

            EscimoResult result = new EscimoResult( sl.getStatusCode(), resp.getAllHeaders() );
            
            String retVal = EntityUtils.toString( resp.getEntity() );

            if ( sl.getStatusCode() == 200 )
            {
                result.setResource( deserialize( retVal ) );
            }
            else
            {
                result.setErrorResponse( deserializeError( retVal ) );
            }
            
            return result;
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed while retrieving resource from {}", get.getURI() );
            throw new RuntimeException( e );
        }
    }

    
    private EscimoResult addResource( CoreResource resource, String uri )
    {
        if ( resource == null )
        {
            throw new IllegalArgumentException( "resource cannot be null" );
        }

        HttpPost post = new HttpPost( providerUrl + uri );

        String payload = serialize( resource ).toString();

        LOG.debug( "sending JSON payload to URI {} for adding resource:\n{}", uri, payload );

        post.setEntity( new StringEntity( payload, ContentType.APPLICATION_JSON ) );

        HttpClient client = HttpClients.createDefault();

        try
        {
            HttpResponse resp = client.execute( post );
            StatusLine sl = resp.getStatusLine();
            
            EscimoResult result = new EscimoResult( sl.getStatusCode(), resp.getAllHeaders() );
            
            String retVal = EntityUtils.toString( resp.getEntity() );
            
            if ( sl.getStatusCode() == 201 )
            {
                result.setResource( deserialize( retVal ) );
            }
            else
            {
                result.setErrorResponse( deserializeError( retVal ) );
            }
            
            return result;
        }
        catch( Exception e )
        {
            LOG.warn( "Failed while trying to add a resource at {}", post.getURI() );
            throw new RuntimeException( e );
        }
    }


    private EscimoResult putResource( String resourceId, CoreResource resource, String uri )
    {
        if ( resource == null )
        {
            throw new IllegalArgumentException( "resource cannot be null" );
        }

        uri = uri + "/" + resourceId;
        
        HttpPut put = new HttpPut( providerUrl + uri );

        String payload = serialize( resource ).toString();

        LOG.debug( "sending JSON payload to URI {} for adding resource:\n{}", uri, payload );

        put.setEntity( new StringEntity( payload, ContentType.APPLICATION_JSON ) );

        HttpClient client = HttpClients.createDefault();

        try
        {
            HttpResponse resp = client.execute( put );
            StatusLine sl = resp.getStatusLine();

            EscimoResult result = new EscimoResult( sl.getStatusCode(), resp.getAllHeaders() );
            
            String retVal = EntityUtils.toString( resp.getEntity() );
            
            if ( sl.getStatusCode() == 200 )
            {
                result.setResource( deserialize( retVal ) );
            }
            else
            {
                result.setErrorResponse( deserializeError( retVal ) );
            }
            
            return result;
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed while trying to replace a resource at {}", put.getURI() );
            throw new RuntimeException( e );
        }
    }

    
    private EscimoResult patchResource( String resourceId, CoreResource resource, String uri )
    {
        if ( resource == null )
        {
            throw new IllegalArgumentException( "resource cannot be null" );
        }

        uri = uri + "/" + resourceId;
        
        HttpPatch put = new HttpPatch( providerUrl + uri );

        String payload = serialize( resource ).toString();

        LOG.debug( "sending JSON payload to URI {} for adding resource:\n{}", uri, payload );

        put.setEntity( new StringEntity( payload, ContentType.APPLICATION_JSON ) );

        HttpClient client = HttpClients.createDefault();

        try
        {
            HttpResponse resp = client.execute( put );
            StatusLine sl = resp.getStatusLine();
            
            EscimoResult result = new EscimoResult( sl.getStatusCode(), resp.getAllHeaders() );
            
            
            if ( sl.getStatusCode() == 200 )
            {
                String retVal = EntityUtils.toString( resp.getEntity() );
                result.setResource( deserialize( retVal ) );
            }
            else if ( sl.getStatusCode() == 204 )
            {
                // do nothing
            }
            else // everything else is an error
            {
                String retVal = EntityUtils.toString( resp.getEntity() );
                result.setErrorResponse( deserializeError( retVal ) );
            }
            
            return result;
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed while trying to patch a resource at {}", put.getURI() );
            throw new RuntimeException( e );
        }
    }

    private CoreResource deserialize( String json )
    {
        if( json == null )
        {
            return null;
        }
        
        JsonParser parser = new JsonParser();
        JsonObject obj = ( JsonObject ) parser.parse( json );

        JsonArray schemas = obj.get( "schemas" ).getAsJsonArray();
        
        CoreResource top = null;
        
        List<CoreResource> extList = new ArrayList<CoreResource>();
        
        for( JsonElement je : schemas )
        {
            String sch = je.getAsString();
            
            JsonObject subres = obj.getAsJsonObject( sch );
            
            // this is the top/core schema object
            if( subres == null )
            {
                top = serializer.fromJson( obj, uriClassMap.get( sch ) );
            }
            else
            {
                CoreResource ext = serializer.fromJson( subres, uriClassMap.get( sch ) );
                extList.add( ext );
            }
        }

        if( !extList.isEmpty() )
        {
            top.setExtResources( extList );
        }
        
        return top;
    }

    private ErrorResponse deserializeError( String json )
    {
        if( json == null )
        {
            return null;
        }
        
        return serializer.fromJson( json, ErrorResponse.class );
    }
    
    private JsonObject serialize( CoreResource resource )
    {
        JsonObject json = ( JsonObject ) serializer.toJsonTree( resource );

        List<CoreResource> exts = resource.getExtResources();

        if ( exts != null )
        {
            for ( CoreResource e : exts )
            {
                JsonElement el = serializer.toJsonTree( resource );
                json.add( e.getSchemaId(), el );
            }
        }

        return json;
    }

    private Map<String, Class<? extends CoreResource>> getUriClassMap( CoreResource resource )
    {
        String topUri = resource.getSchemaId();
        Map<String, Class<? extends CoreResource>> map = new HashMap<String, Class<? extends CoreResource>>();
        map.put( topUri, resource.getClass() );
        
        List<CoreResource> ext = resource.getExtResources();
        
        if( ext != null )
        {
            for( CoreResource c : ext )
            {
                map.put( c.getSchemaId(), c.getClass() );
            }
        }
        
        return map;
    }

}
