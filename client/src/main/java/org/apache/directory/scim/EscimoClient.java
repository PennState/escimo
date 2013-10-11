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
import java.util.List;

import org.apache.directory.scim.schema.CoreResource;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private static final Logger LOG = LoggerFactory.getLogger( EscimoClient.class );


    public EscimoClient( String providerUrl )
    {
        this.providerUrl = providerUrl;
        GsonBuilder gb = new GsonBuilder();
        gb.setExclusionStrategies( new FieldExclusionStrategy() );
        //        gb.setDateFormat( pattern );
        serializer = gb.create();
    }

    public CoreResource addUser( CoreResource resource ) throws Exception
    {
        return addResource( resource, USERS_URI );
    }

    private CoreResource addResource( CoreResource resource, String uri ) throws Exception
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
            
            if ( sl.getStatusCode() == 200 )
            {
                String retVal = EntityUtils.toString( resp.getEntity() );
                
                return deserialize( retVal );
            }
        }
        catch ( Exception e )
        {
            LOG.warn( "", e );
            throw e;
        }
        
        return null;
    }


    private CoreResource deserialize( String json )
    {
        JsonParser parser = new JsonParser();
        JsonObject obj = ( JsonObject ) parser.parse( json );

        CoreResource top = serializer.fromJson( obj, CoreResource.class );

        for ( java.util.Map.Entry<String, JsonElement> e : obj.entrySet() )
        {
            String key = e.getKey();

            if ( key.startsWith( "urn:scim:schemas:" ) )
            {
                CoreResource ext = serializer.fromJson( e.getValue(), CoreResource.class );
                top.addExtendedResource( ext );
            }
        }

        return top;
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


    public static void main( String[] args )
    {
        EscimoClient ec = new EscimoClient( "http://example.com" );
        String json = ec.serializer.toJson( ec );
        System.out.println( json );

        EscimoClient clone = ec.serializer.fromJson( json, EscimoClient.class );
        System.out.println( clone );
    }
}
