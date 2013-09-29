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


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * TODO JsonToJava.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JsonToJava
{
    private static StringTemplateGroup stg = new StringTemplateGroup( "json" );
    
    public static void compile( String schema )
    {
        JsonParser parser = new JsonParser();
        JsonObject json = ( JsonObject ) parser.parse( schema );

        List<String> innerClasses = new ArrayList<String>();
        
        StringTemplate template = generateClass( json, innerClasses, null );
        template.setAttribute( "allInnerClasses", innerClasses );
        
        System.out.println( template );
    }

    
    private static StringTemplate generateClass( JsonObject json, List<String> innerClasses, StringTemplate parent )
    {
        StringTemplate template = stg.getInstanceOf( "resource-class" );

        if ( json.has( "id" ) )
        {
            String schemaId = json.get( "id" ).getAsString();
            template.setAttribute( "schemaId", schemaId );

            template.setAttribute( "package", "org.apache.directory.scim" );

            template.setAttribute( "visibility", "public" );

            String desc = json.get( "description" ).getAsString();
            template.setAttribute( "resourceDesc", desc );
        }

        if( parent != null )
        {
            template.setAttribute( "static", "static" );
        }

        String className = json.get( "name" ).getAsString();
        template.setAttribute( "className", className );

        List<AttributeDetail> simpleAttributes = new ArrayList<AttributeDetail>();


        JsonArray attributes;
        
        if( json.has( "attributes" ) )
        {
            attributes = json.get( "attributes" ).getAsJsonArray();
        }
        else
        {
            attributes = json.get( "subAttributes" ).getAsJsonArray();
        }

        Iterator<JsonElement> itr = attributes.iterator();
        while ( itr.hasNext() )
        {
            JsonObject jo = ( JsonObject ) itr.next();
            String type = jo.get( "type" ).getAsString();

            String name = jo.get( "name" ).getAsString();

            String javaType = "String";
            if ( type.equals( "numeric" ) )
            {
                javaType = "Number";
            }
            else if ( type.equals( "boolean" ) )
            {
                javaType = "boolean";
            }

            if ( type.equals( "complex" ) )
            {
                javaType = Character.toUpperCase( name.charAt( 0 ) ) + name.substring( 1 );
                
                if( javaType.endsWith( "s" ) )
                {
                    javaType = javaType.substring( 0, javaType.length() - 1 );
                }
                
                // replace the type's name
                jo.remove( "name" );
                jo.addProperty( "name", javaType );
                
                javaType = className + "." + javaType;
                
                boolean multiValued = jo.get( "multiValued" ).getAsBoolean();
                
                if ( multiValued )
                {
                    javaType = "java.util.List<" + javaType + ">";
                }
                
                //how to add inner classes
                StringTemplate inner = generateClass( jo, innerClasses, template );
                innerClasses.add( inner.toString() );
            }

            AttributeDetail nc = new AttributeDetail( name, javaType );
            simpleAttributes.add( nc );
        }

        template.setAttribute( "allAttrs", simpleAttributes );

        return template;
    }

    public static void main( String[] args ) throws Exception
    {
        //InputStream in = JsonToJava.class.getClassLoader().getSystemClassLoader().getResourceAsStream( "user-schema.json" );
        BufferedReader br = new BufferedReader( new FileReader(
            "/Users/dbugger/projects/json-schema-escimo/common/src/main/resources/user-schema.json" ) );
        String s;

        StringBuilder sb = new StringBuilder();
        while ( ( s = br.readLine() ) != null )
        {
            sb.append( s );
        }

        JsonToJava.compile( sb.toString() );
    }
}
