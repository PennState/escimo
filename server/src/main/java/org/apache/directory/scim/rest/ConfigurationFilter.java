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
package org.apache.directory.scim.rest;

import java.io.File;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang.StringUtils;

/**
 * TODO ConfigurationFilter.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ConfigurationFilter implements Filter
{

    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException,
        ServletException
    {
        // do not do anything else here 
        chain.doFilter( request, response );
    }

    public void destroy()
    {
    }

    public void init( FilterConfig filterConfig ) throws ServletException
    {
        String configDir = filterConfig.getInitParameter( "configDir" );
        
        if( StringUtils.isBlank( configDir ) )
        {
            throw new IllegalArgumentException( "Mandatory parameter 'configDir' is missing in ConfigurationFilter declaration in web.xml" );
        }
        
        if( configDir.startsWith( "/WEB-INF" ) )
        {
            configDir = filterConfig.getServletContext().getRealPath( configDir );
        }
        
        File dir = new File( configDir );
        
        if( !dir.exists() )
        {
            boolean created = dir.mkdirs();
            
            if( !created )
            {
                throw new IllegalArgumentException( "Could not create the given config directory " + configDir );
            }
        }
        
        System.setProperty( "escimo.config.dir", dir.getAbsolutePath() );
        
        File jsonSchemaDir = new File( dir, "json-schema" );
        jsonSchemaDir.mkdir();
        
        System.setProperty( "escimo.json.schema.dir", jsonSchemaDir.getAbsolutePath() );
        /*
        File[] propFiles = dir.listFiles();
        
        for( File f : propFiles )
        {
            if( f.getName().endsWith( ".properties" ) )
            {
                Properties props = new Properties();
                
                FileInputStream fin = null;
                
                try
                {
                    fin = new FileInputStream( f );
                    props.load( fin );
                    System.setProperties( props );
                }
                catch( Exception e )
                {
                    throw new RuntimeException( e );
                }
                finally
                {
                    if( fin != null )
                    {
                        try
                        {
                            fin.close();
                        }
                        catch( IOException e )
                        {
                            //ignore
                        }
                    }
                }
            }
        } 
        */
    }

}
