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


import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;


/**
 * TODO RequestContext.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RequestContext
{
    private ProviderService providerService;

    private UriInfo uriInfo;

    private HttpHeaders headers;
    
    private ServerResource resource;

    public RequestContext( ProviderService providerService, UriInfo uriInfo, HttpHeaders headers )
    {
        this.providerService = providerService;
        this.uriInfo = uriInfo;
        this.headers = headers;
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


    /**
     * @return the headers
     */
    public HttpHeaders getHeaders()
    {
        return headers;
    }


    public ProviderService getProviderService()
    {
        return providerService;
    }

    
    public String getHeaderValue( String name )
    {
        List<String> lst = headers.getRequestHeader( name );
        
        if( ( lst == null ) || ( lst.isEmpty() ) )
        {
            return null;
        }
        
        return lst.get( 0 );
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
