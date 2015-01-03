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
package org.apache.directory.scim.json;


import java.util.List;
import java.util.Map;

import org.apache.directory.scim.AbstractAttribute;
import org.apache.directory.scim.ComplexAttribute;
import org.apache.directory.scim.FieldExclusionStrategy;
import org.apache.directory.scim.ListResponse;
import org.apache.directory.scim.MultiValAttribute;
import org.apache.directory.scim.ScimErrorDeserializer;
import org.apache.directory.scim.ScimUtil;
import org.apache.directory.scim.ServerResource;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.SimpleAttributeGroup;
import org.apache.directory.scim.schema.ErrorResponse;
import org.apache.directory.scim.schema.ErrorResponse.ScimError;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


/**
 * TODO ResourceSerializer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ResourceSerializer
{

    private static final JsonArray ERROR_RESPONSE_SCHEMAS = new JsonArray();

    private static Gson serializer;

    static
    {
        ERROR_RESPONSE_SCHEMAS.add( new JsonPrimitive( ErrorResponse.SCHEMA_ID ) );
        
        GsonBuilder gb = new GsonBuilder();
        gb.setExclusionStrategies( new FieldExclusionStrategy() );
        gb.setDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );
        gb.registerTypeAdapter( ScimError.class, new ScimErrorDeserializer() );
        serializer = gb.create();
    }

    public static String serialize( ServerResource resource )
    {
        return _serialize( resource ).toString();
    }
    
    private static JsonObject _serialize( ServerResource resource )
    {
        JsonObject root = new JsonObject();

        Map<String, List<AbstractAttribute>> attributes = resource.getAttributes();

        JsonArray schemas = new JsonArray();
        root.add( "schemas", schemas );

        for ( String uri : attributes.keySet() )
        {
            schemas.add( new JsonPrimitive( uri ) );

            JsonObject parent = root;

            if ( !ScimUtil.isCoreAttribute( uri ) )
            {
                parent = new JsonObject();
                root.add( uri, parent );
            }

            serialize( parent, attributes.get( uri ) );
        }

        return root;
    }


    public static void serialize( JsonObject parent, List<AbstractAttribute> attributes )
    {
        for ( AbstractAttribute at : attributes )
        {
            serializeAt( parent, at );
        }
    }


    public static void serializeAt( JsonObject parent, AbstractAttribute at )
    {
        if ( at instanceof SimpleAttribute )
        {
            serializeSimpleAt( parent, ( SimpleAttribute ) at );
        }
        else if ( at instanceof ComplexAttribute )
        {
            ComplexAttribute ct = ( ComplexAttribute ) at;
            JsonObject json = new JsonObject();
            for ( SimpleAttribute t : ct.getAtList() )
            {
                serializeSimpleAt( json, t );
            }

            parent.add( ct.getName(), json );
        }
        else if ( at instanceof MultiValAttribute )
        {
            MultiValAttribute mv = ( MultiValAttribute ) at;
            List<SimpleAttributeGroup> lstGrp = mv.getAtGroupList();

            JsonArray array = new JsonArray();

            for ( SimpleAttributeGroup stg : lstGrp )
            {
                JsonObject json = new JsonObject();
                for ( SimpleAttribute t : stg.getAtList() )
                {
                    serializeSimpleAt( json, t );
                }

                array.add( json );
            }

            parent.add( mv.getName(), array );
        }
    }


    public static void serializeSimpleAt( JsonObject parent, SimpleAttribute at )
    {
        Object obj = at.getValue();

        if ( obj instanceof String )
        {
            parent.addProperty( at.getName(), ( String ) obj );
        }
        else if ( obj instanceof Number )
        {
            parent.addProperty( at.getName(), ( Number ) obj );
        }
        if ( obj instanceof Boolean )
        {
            parent.addProperty( at.getName(), ( Boolean ) obj );
        }
    }
    
    public static String serialize( ErrorResponse err )
    {
        JsonObject jo = ( JsonObject ) serializer.toJsonTree( err );
        
        jo.add( "schemas", ERROR_RESPONSE_SCHEMAS );
        
        return jo.toString();
    }
    
    
    public static String serialize( ListResponse lr )
    {
        JsonObject json = new JsonObject();
        json.addProperty( "totalResults", lr.getTotalResults() );
        
        if( lr.getItemsPerPage() > -1 )
        {
            json.addProperty( "itemsPerPage", lr.getItemsPerPage() );
        }
        
        if( lr.getStartIndex() > -1 )
        {
            json.addProperty( "startIndex", lr.getStartIndex() );
        }
        
        JsonArray resArray = new JsonArray();
        
        for( ServerResource sr : lr.getResources() )
        {
            JsonObject resObj = _serialize( sr );
            resObj.remove( "schemas" );
            resArray.add( resObj );
        }
        
        json.add( "Resources", resArray );
        
        return json.toString();
    }
}
