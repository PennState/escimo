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
package org.apache.directory.scim.schema;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * TODO SchemaUtil.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaUtil
{
    private static String[] stockNames =
        { "user-schema.json", "group-schema.json", "enterprise-user-schema.json", "serviceproviderconfig-schema.json" };

    public static final String PROVIDER_SERVICE_SCHEMA_ID = "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig";

    public static final String CORE_USER_ID = "urn:ietf:params:scim:schemas:core:2.0:User";

    public static final String CORE_GROUP_ID = "urn:ietf:params:scim:schemas:core:2.0:Group";

    public static final String CORE_EXT_USER_ID = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

    public static final String CORE_SCHEMA_ID_PREFIX = "urn:ietf:params:scim:schemas:core:2.0";

    private static final Logger LOG = LoggerFactory.getLogger( SchemaUtil.class );
    

    public static List<URL> getSchemas( File schemaDir )
    {
        File[] files = schemaDir.listFiles();
        
        List<URL> urls = new ArrayList<URL>();
        
        for( File f : files )
        {
            if( f.getName().endsWith( "-schema.json" ) )
            {
                try
                {
                    urls.add( f.toURI().toURL() );
                }
                catch( MalformedURLException e )
                {
                    // should never happen
                    throw new RuntimeException( e );
                }
            }
        }
        
        return urls;
    }
    
    
    public static Map<String,JsonSchema> storeDefaultSchemas( File schemaDir ) throws IOException
    {
        List<URL> urls = SchemaUtil.getDefaultSchemas();
        
        Map<String,JsonSchema> schemas = new HashMap<String, JsonSchema>();
        
        for( URL u : urls )
        {
            String json = getSchemaJson( u );
            
            JsonSchema schema = JsonSchema.parse( json );
            
            if ( schema != null )
            {
                schemas.put( schema.getId(), schema );
            }
            
            String name = u.getFile();
            int pos = name.lastIndexOf( File.separator );
            if( pos > 0 )
            {
                name = name.substring( pos, name.length() );
            }
            
            FileWriter fw = new FileWriter( new File( schemaDir, name ) );
            fw.write( json );
            fw.close();
        }
        
        return schemas;
    }
    
    
    public static List<URL> getDefaultSchemas()
    {
        List<URL> lst = new ArrayList<URL>();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for ( String s : stockNames )
        {
            URL u = cl.getResource( s );
            lst.add( u );
        }

        return lst;
    }


    public static String getSchemaJson( URL url ) throws IOException
    {
        BufferedReader br = null;
        try
        {
            br = new BufferedReader( new InputStreamReader( url.openStream() ) );

            String s;

            StringBuilder sb = new StringBuilder();
            while ( ( s = br.readLine() ) != null )
            {
                sb.append( s );
            }

            return sb.toString();
        }
        finally
        {
            if ( br != null )
            {
                try
                {
                    br.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
    }

    
    public static JsonObject getResourceProviderConfig()
    {
        String jsonSchemaDir = System.getProperty( "escimo.json.schema.dir", null );

        if ( jsonSchemaDir == null )
        {
            return null;
        }
        
        File schemaDir = new File( jsonSchemaDir );

        List<URL> urls = SchemaUtil.getSchemas( schemaDir );
        
        try
        {
            JsonParser parser = new JsonParser();

            for( URL u : urls )
            {
                String json = SchemaUtil.getSchemaJson( u );
                JsonObject obj = ( JsonObject ) parser.parse( json );
                JsonElement scEl = obj.get( "schemas" );
                if( ( scEl != null ) && ( scEl.isJsonArray() ) )
                {
                    JsonArray ja = scEl.getAsJsonArray();
                    if( ja.size() > 0 )
                    {
                        String scName = ja.get( 0 ).getAsString();
                        if( scName.equals( PROVIDER_SERVICE_SCHEMA_ID ) )
                        {
                            return obj;
                        }
                    }
                }
            }
        }
        catch( Exception e )
        {
            LOG.warn( "Failed to get ResourceProviderConfig from the directory {}", jsonSchemaDir );
            LOG.warn( "", e );
        }
        
        return null;
    }
}
