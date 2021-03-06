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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.directory.scim.schema.SchemaUtil;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * TODO JsonToJava.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class JsonToJava extends AbstractMojo
{
    @Parameter(defaultValue = "org.apache.directory.scim")
    private String generatePackage;

    @Parameter
    private URL schemaBaseUrl;

    @Parameter
    private List<String> schemaFiles;

    @Parameter
    private boolean useDefaultSchemas;

    @Parameter(defaultValue = "${project.build.directory}")
    protected File targetDirectory;

    private static StringTemplateGroup stg = new StringTemplateGroup( "json" );


    public void execute() throws MojoExecutionException, MojoFailureException
    {
        Log log = getLog();

        log.info( "Starting json2java" );

        if ( useDefaultSchemas && ( ( schemaBaseUrl != null ) || ( schemaFiles != null ) ) )
        {
            String msg = "Conflicting configuration options. schemaBaseUrl or schemaFiles cannot be specified when useDefaultSchemas is set to true";
            log.warn( msg );
            throw new MojoFailureException( msg );
        }

        String packageDirPath = generatePackage.replace( ".", "/" );

        File srcDir = new File( targetDirectory, "generated-sources/json2java/" + packageDirPath );

        log.debug( "Creating directories for storing generated source files" );

        if( !srcDir.exists() )
        {
            boolean created = srcDir.mkdirs();
            if ( !created )
            {
                String msg = "Failed to create the directory " + srcDir.getAbsolutePath()
                    + " to store the generated source files";
                log.warn( msg );
                throw new MojoFailureException( msg );
            }
        }

        List<URL> lst = null;

        if ( useDefaultSchemas )
        {
            log.info( "Generating sources for the default schemas" );
            lst = SchemaUtil.getDefaultSchemas();

            if ( lst.isEmpty() )
            {
                throw new MojoFailureException( "None of the default schemas found in the classpath" );
            }
        }

        for ( URL url : lst )
        {
            String schemaJson = getSchemaJson( url );
            try
            {
                compileAndSave( schemaJson, srcDir );
            }
            catch( Exception e )
            {
                e.printStackTrace();
                throw new RuntimeException( e );
            }
        }
    }


    private String getSchemaJson( URL url ) throws MojoExecutionException
    {
        BufferedReader br = null;
        try
        {
            getLog().debug( "Fetching the contents of the resource at URL " + url );
            br = new BufferedReader( new InputStreamReader( url.openStream() ) );

            String s;

            StringBuilder sb = new StringBuilder();
            while ( ( s = br.readLine() ) != null )
            {
                sb.append( s );
            }

            return sb.toString();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to read schema data from the URL " + url, e );
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


    public void compileAndSave( String schema, File srcDir )  throws MojoExecutionException
    {
        JsonParser parser = new JsonParser();
        JsonObject json = ( JsonObject ) parser.parse( schema );

        JsonElement nameEl = json.get( "name" );
        
        if( nameEl == null )
        {
            JsonElement scEl = json.get( "schemas" );
            if( ( scEl != null ) && ( scEl.isJsonArray() ) )
            {
                JsonArray ja = scEl.getAsJsonArray();
                if( ja.size() > 0 )
                {
                    String scName = ja.get( 0 ).getAsString();
                    if( scName.contains( "ServiceProviderConfig" ) )
                    {
                        // no need to print a warning, just return
                        return;
                    }
                }
            }
            
            System.out.println( "Ignoring " + schema + " it is not a valid SCIM resource schema" );
            return;
        }
        
        String className = nameEl.getAsString();
        
        List<String> innerClasses = new ArrayList<String>();

        StringTemplate template = generateClass( json, innerClasses, null );
        template.setAttribute( "allInnerClasses", innerClasses );

        File javaFile = new File( srcDir, className + ".java" );
        
        FileWriter fw = null;
        
        try
        {
            fw = new FileWriter( javaFile );
            fw.write( template.toString() );
        }
        catch( IOException e )
        {
            throw new MojoExecutionException( "Failed to store the generated source file for schema " + className, e );
        }
        finally
        {
            if ( fw != null )
            {
                try
                {
                    fw.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
    }


    private StringTemplate generateClass( JsonObject json, List<String> innerClasses, StringTemplate parent )
    {
        StringTemplate template = stg.getInstanceOf( "resource-class" );

        boolean isCoreSchema = false;
        
        if ( json.has( "id" ) )
        {
            String schemaId = json.get( "id" ).getAsString();
            template.setAttribute( "schemaId", schemaId );
            if( schemaId.startsWith( "urn:ietf:params:scim:schemas:core:" ) )
            {
                isCoreSchema = true;
            }

            template.setAttribute( "date", new Date() );
            
            template.setAttribute( "package", generatePackage );

            template.setAttribute( "visibility", "public" );

            String desc = json.get( "description" ).getAsString();
            template.setAttribute( "resourceDesc", desc );
        }

        List<AttributeDetail> simpleAttributes = new ArrayList<AttributeDetail>();

        if ( parent != null )
        {
            template.setAttribute( "static", "static" );
            
            boolean multiValued = json.get( "multiValued" ).getAsBoolean();
            if ( multiValued )
            {
                AttributeDetail operation = new AttributeDetail( "operation", "String" );
                
                simpleAttributes.add( operation );
            }
        }

        String className = json.get( "name" ).getAsString();
        template.setAttribute( "className", className );


        JsonArray attributes;

        if ( json.has( "attributes" ) )
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

            boolean readOnly = false;
            
            JsonElement jeReadOnly = jo.get( "readOnly" );
            if( jeReadOnly != null )
            {
                readOnly = jeReadOnly.getAsBoolean();
            }
            
            String javaType = "String";
            if ( type.equals( "numeric" ) )
            {
                javaType = "Number";
            }
            else if ( type.equals( "boolean" ) )
            {
                javaType = "boolean";
            }

            boolean multiValued = false;

            if ( type.equals( "complex" ) )
            {
                javaType = Character.toUpperCase( name.charAt( 0 ) ) + name.substring( 1 );

                if ( javaType.endsWith( "s" ) )
                {
                    int endPos = javaType.length() - 1;

                    // special case for Address'es'
                    if ( javaType.endsWith( "Addresses" ) )
                    {
                        endPos -= 1;
                    }

                    javaType = javaType.substring( 0, endPos );
                }

                // replace the type's name
                jo.remove( "name" );
                jo.addProperty( "name", javaType );

                javaType = className + "." + javaType;

                multiValued = jo.get( "multiValued" ).getAsBoolean();

                if ( multiValued )
                {
                    javaType = "java.util.List<" + javaType + ">";
                }

                //how to add inner classes
                StringTemplate inner = generateClass( jo, innerClasses, template );
                innerClasses.add( inner.toString() );
            }

            AttributeDetail ad = new AttributeDetail( name, javaType );
            ad.setMultiValued( multiValued );

            simpleAttributes.add( ad );
        }

        if(isCoreSchema)
        {
            AttributeDetail id = new AttributeDetail( "id", "String" );
            simpleAttributes.add( id );
            
            AttributeDetail externalId = new AttributeDetail( "externalId", "String" );
            simpleAttributes.add( externalId );
        }
        
        template.setAttribute( "allAttrs", simpleAttributes );

        return template;
    }


    /**
     * @param generatePackage the generatePackage to set
     */
    public void setGeneratePackage( String generatePackage )
    {
        this.generatePackage = generatePackage;
    }


    /**
     * @param schemaBaseUrl the schemaBaseUrl to set
     */
    public void setSchemaBaseUrl( URL schemaBaseUrl )
    {
        this.schemaBaseUrl = schemaBaseUrl;
    }


    /**
     * @param schemaFiles the schemaFiles to set
     */
    public void setSchemaFiles( List<String> schemaFiles )
    {
        this.schemaFiles = schemaFiles;
    }


    /**
     * @param stg the stg to set
     */
    public static void setStg( StringTemplateGroup stg )
    {
        JsonToJava.stg = stg;
    }


    public static void main( String[] args ) throws Exception
    {
        InputStream in = JsonToJava.class.getClassLoader().getResourceAsStream( "example-user.json" );
        BufferedReader br = new BufferedReader( new InputStreamReader( in ) );

        //        BufferedReader br = new BufferedReader( new FileReader(
        //            "/Users/dbugger/projects/json-schema-escimo/common/src/main/resources/user-schema.json" ) );
        String s;

        StringBuilder sb = new StringBuilder();
        while ( ( s = br.readLine() ) != null )
        {
            sb.append( s );
        }

        //        JsonToJava.compile( sb.toString() );
        Gson gson = new Gson();

        //String json = sb.toString();
        //User u = gson.fromJson( json, User.class );
        //System.out.println( u );
    }
}
