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
import java.util.Map;

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

    private Resource resource;

    private Map<String,String> reqParams;

    public RequestContext( ProviderService providerService )
    {
        this.providerService = providerService;
    }


    public Resource getCoreResource()
    {
        return resource;
    }


    public void setCoreResource( Resource resource )
    {
        this.resource = resource;
    }


    public UriInfo getUriInfo()
    {
        return uriInfo;
    }


    public void setUriInfo( UriInfo uriInfo )
    {
        this.uriInfo = uriInfo;
    }


    public ProviderService getProviderService()
    {
        return providerService;
    }

    
    public void addReqParam( String name, String value )
    {
        if( reqParams == null )
        {
            reqParams = new HashMap<String, String>();
        }
        
        reqParams.put( name, value );
    }
    
    
    public String getReqParam( String name )
    {
        if( reqParams == null )
        {
            return null;
        }
        
        return reqParams.get( name );
    }
}
