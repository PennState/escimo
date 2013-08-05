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


import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.directory.scim.AbstractAttribute;
import org.apache.directory.scim.ComplexAttribute;
import org.apache.directory.scim.MultiValAttribute;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.SimpleAttributeGroup;
import org.apache.directory.scim.User;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


/**
 * TODO ResourceSerializer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ResourceSerializer
{
    private Gson gson = new Gson();

    public static String serialize( User user )
    {
        JsonObject json = new JsonObject();

        Map<String, List<AbstractAttribute>> attributes = user.getAttributes();
        for ( String uri : attributes.keySet() )
        {
            serialize( json, attributes.get( uri ) );
        }
        
        return json.toString();
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
            for( SimpleAttribute t : ct.getAtList() )
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
            
            for( SimpleAttributeGroup stg : lstGrp )
            {
                JsonObject json = new JsonObject();
                for( SimpleAttribute t : stg.getAtList() )
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
    }
}
