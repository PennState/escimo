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
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.scim.ResourceProvider;


/**
 * TODO EscimoContextListener.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EscimoContextListener implements ServletContextListener
{
    private static ResourceProvider provider;


    public void contextInitialized( ServletContextEvent sce )
    {
        extractDefaultConfig( sce );

        String fqcn = System.getProperty( "escimo.resource.provider",
            "org.apache.directory.scim.ldap.LdapResourceProvider" );
        if ( StringUtils.isBlank( fqcn ) )
        {
            throw new RuntimeException( "No resource provider implementation class is found" );
        }

        try
        {
            provider = ( ResourceProvider ) Class.forName( fqcn ).newInstance();
            provider.init();

            sce.getServletContext().setAttribute( ResourceProvider.SERVLET_CONTEXT_ATTRIBUTE_KEY, provider );
        }
        catch ( Exception e )
        {
            RuntimeException re = new RuntimeException(
                "Unable to instantiate the resource provider implementation class " + fqcn );
            re.initCause( e );
            throw re;
        }

        EscimoApplication app = new EscimoApplication();
        Set<Object> instances = app.getInstances();

        // dynamic registration of resource paths
        for ( String uri : provider.getResourceUris() )
        {
            ResourceService service = new ResourceService();
            service.setPath( uri );
            instances.add( service );
        }
    }


    public void contextDestroyed( ServletContextEvent sce )
    {
        provider.stop();
    }


    private void extractDefaultConfig( ServletContextEvent sce )
    {
        ServletContext ctx = sce.getServletContext();

        String configDir = ctx.getInitParameter( "configDir" );

        if ( StringUtils.isBlank( configDir ) )
        {
            throw new IllegalArgumentException(
                "Mandatory parameter 'configDir' is missing in ConfigurationFilter declaration in web.xml" );
        }

        if ( configDir.startsWith( "/WEB-INF" ) )
        {
            configDir = ctx.getRealPath( configDir );
        }

        File dir = new File( configDir );

        if ( !dir.exists() )
        {
            boolean created = dir.mkdirs();

            if ( !created )
            {
                throw new IllegalArgumentException( "Could not create the given config directory " + configDir );
            }
        }

        System.setProperty( "escimo.config.dir", dir.getAbsolutePath() );

        File jsonSchemaDir = new File( dir, "json-schema" );
        jsonSchemaDir.mkdir();

        System.setProperty( "escimo.json.schema.dir", jsonSchemaDir.getAbsolutePath() );
    }
}
