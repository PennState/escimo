/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.scim.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.ProviderService;
import org.apache.directory.scim.ResourceNotFoundException;
import org.apache.directory.scim.User;
import org.apache.directory.scim.json.ResourceSerializer;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Path( "Users" )
public class UserService
{

    private ProviderService provider = ServerInitializer.getProvider();
    
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{id}")
    public Response getUser( @PathParam("id") String userId, @Context UriInfo uriInfo )
    {
        ResponseBuilder rb = null;
        
        try
        {
            RequestContext ctx = new RequestContext();
            ctx.setUriInfo( uriInfo );
            
            User user = provider.getUser( ctx, userId );
            String json = ResourceSerializer.serialize( user );
            rb = Response.ok( json, MediaType.APPLICATION_JSON );
        }
        catch( ResourceNotFoundException e )
        {
            rb = Response.status( Status.INTERNAL_SERVER_ERROR );
        }
        
        return rb.build();
    }
}
