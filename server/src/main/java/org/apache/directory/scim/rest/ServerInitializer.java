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

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.scim.ProviderService;
import org.apache.directory.scim.schema.SchemaUtil;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * TODO ServerInitializer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ServerInitializer
{
    private static ProviderService provider;
    
    private static void init()
    {
        String fqcn = System.getProperty( "escimo.resource.provider", "org.apache.directory.scim.ldap.LdapResourceProvider" );
        if(StringUtils.isBlank( fqcn ))
        {
            throw new RuntimeException( "No resource provider implementation class is found" );
        }
        
        try
        {
            provider = ( ProviderService ) Class.forName( fqcn ).newInstance();
            provider.init();
        }
        catch( Exception e )
        {
            RuntimeException re = new RuntimeException( "Unable to instantiate the resource provider implementation class " + fqcn );
            re.initCause( e );
            throw re;
        }
        
    }
    
    public static ProviderService getProvider()
    {
        if( provider == null )
        {
            init();
        }
        
        return provider;
    }
}
