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


import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.scim.schema.SchemaUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * TODO JsonSchema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JsonSchema
{
    private String rawJson;

    private String id;

    private String name;

    private String desc;

    private boolean core;

    private Map<String, JsonObject> attributes;


    private JsonSchema( String rawJson )
    {
        this.rawJson = rawJson;
        this.attributes = new HashMap<String, JsonObject>();
    }


    public static JsonSchema parse( String rawJson )
    {
        JsonSchema schema = new JsonSchema( rawJson );
        boolean result = schema._parse();

        if ( !result )
        {
            schema = null;
        }
        
        return schema;
    }


    private boolean _parse()
    {
        JsonParser parser = new JsonParser();
        JsonObject obj = ( JsonObject ) parser.parse( rawJson );

        JsonElement idEl = obj.get( "id" );
        
        if ( idEl == null )
        {
            JsonElement scEl = obj.get( "schemas" );
            System.out.println( "Ignoring schema with schemas " + scEl + " , it is not a valid SCIM resource schema" );
            return false;
        }
        
        this.id = idEl.getAsString();
        this.name = obj.get( "name" ).getAsString();
        this.desc = obj.get( "description" ).getAsString();

        core = id.startsWith( SchemaUtil.CORE_SCHEMA_ID_PREFIX );

        _readAttributeDef( obj );
        
        return true;
    }


    private void _readAttributeDef( JsonObject obj )
    {
        JsonArray atArray = null;

        String parentName = null;

        if ( obj.has( "attributes" ) )
        {
            atArray = obj.get( "attributes" ).getAsJsonArray();
        }
        else
        {
            atArray = obj.get( "subAttributes" ).getAsJsonArray();
            parentName = obj.get( "name" ).getAsString();
        }

        for ( JsonElement je : atArray )
        {
            JsonObject attribute = ( JsonObject ) je;
            String type = attribute.get( "type" ).getAsString();
            String name = attribute.get( "name" ).getAsString();

            if ( parentName != null )
            {
                name = parentName + "." + name;
            }

            attributes.put( name.toLowerCase(), attribute );

            if ( type.equals( "complex" ) )
            {
                _readAttributeDef( attribute );
            }
        }
    }


    /**
     * gives the schema definition of an attribute with the given name.
     * JSON dot notation is also supported in the attribute's name.
     * e.x emails.value will give the definition of 'value' sub-attribute
     * of the 'emails' attribute
     * 
     * @param name the name of the attribute, e.x 'userName', 'emails.value' etc.
     * @return a JSON object containing the definition of attribute's schema
     */
    public JsonObject getAttributeDef( String name )
    {
        return attributes.get( name );
    }


    /**
     * tells if an attribute with the given name is read-only.
     * 
     * JSON dot notation is also supported in the attribute's name.
     * e.x emails.value will tell if 'value' sub-attribute of the 
     * 'emails' attribute is read-only
     * 
     *
     * @param name the name of the attribute, e.x 'userName', 'emails.value' etc.
     * @return true if attribute is read-only, false when 'readOnly' value is not 
     *              specified or is set to false
     */
    public boolean isReadOnly( String name )
    {
        JsonObject jo = getAttributeDef( name.toLowerCase() );

        if ( jo == null )
        {
            if ( name.equals( "meta" ) ||
                name.equals( "id" ) )
            {
                return true;
            }

            throw new IllegalArgumentException( "Unknown attribute name " + name );
        }

        JsonElement je = jo.get( "readOnly" );

        if ( je != null )
        {
            return je.getAsBoolean();
        }

        return false;
    }


    /**
     * @return the rawJson
     */
    public String getRawJson()
    {
        return rawJson;
    }


    /**
     * @return the id
     */
    public String getId()
    {
        return id;
    }


    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }


    /**
     * @return the desc
     */
    public String getDesc()
    {
        return desc;
    }


    public boolean isCore()
    {
        return core;
    }


    @Override
    public String toString()
    {
        return "JsonSchema [id=" + id + ", name=" + name + ", desc=" + desc + ", core=" + core + "]";
    }
}
