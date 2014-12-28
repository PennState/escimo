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


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;


/**
 * TODO RequestContext.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RequestContext
{
    private ResourceProvider providerService;

    private UriInfo uriInfo;

    private ServerResource resource;

    public static final String USER_AUTH_HEADER = "X-Escimo-Auth";
    
    private Map<String, String> respHeaders = new HashMap<String, String>();
    
    private HttpServletRequest httpReq;
    
    protected RequestContext( ResourceProvider providerService, UriInfo uriInfo, HttpServletRequest httpReq )
    {
        this.providerService = providerService;
        this.uriInfo = uriInfo;
        this.httpReq = httpReq;
    }


    public ServerResource getCoreResource()
    {
        return resource;
    }


    public void setCoreResource( ServerResource resource )
    {
        this.resource = resource;
    }


    public UriInfo getUriInfo()
    {
        return uriInfo;
    }


    public ResourceProvider getProviderService()
    {
        return providerService;
    }

    
    public void addRespHeader( String name, String value )
    {
        respHeaders.put( name, value );
    }
    
    
    public String getReqHeaderValue( String name )
    {
       return httpReq.getHeader( name );
    }
    
    
    public String getParamAttributes()
    {
        List<String> attributes = uriInfo.getQueryParameters().get( "attributes" );
        
        if( ( attributes == null ) || attributes.isEmpty() )
        {
            return null;
        }
        
        return attributes.get( 0 );
    }
}
