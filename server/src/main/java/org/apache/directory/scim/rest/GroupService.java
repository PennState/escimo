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


import static org.apache.directory.scim.ScimUtil.exceptionToStr;

import java.net.URI;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.directory.scim.GroupResource;
import org.apache.directory.scim.ProviderService;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.ResourceNotFoundException;
import org.apache.directory.scim.ServerResource;
import org.apache.directory.scim.json.ResourceSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO GroupService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Path( "Groups" )
public class GroupService
{
    private ProviderService provider = ServerInitializer.getProvider();

    private static final Logger LOG = LoggerFactory.getLogger( GroupService.class );
    
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{id}")
    public Response getGroup( @PathParam("id") String groupId, @Context UriInfo uriInfo, @Context HttpHeaders headers )
    {
        ResponseBuilder rb = null;
        
        try
        {
            RequestContext ctx = new RequestContext( provider, uriInfo, headers );
            
            GroupResource group = provider.getGroup( ctx, groupId );
            String json = ResourceSerializer.serialize( group );
            rb = Response.ok( json, MediaType.APPLICATION_JSON );
        }
        catch( ResourceNotFoundException e )
        {
            rb = Response.status( Status.INTERNAL_SERVER_ERROR ).entity( exceptionToStr( e ) );
        }
        
        return rb.build();
    }

    
    @DELETE
    @Path("{id}")
    public Response deleteGroup( @PathParam("id") String userId, @Context UriInfo uriInfo, @Context HttpHeaders headers )
    {
        ResponseBuilder rb = Response.ok();
        
        try
        {
            provider.deleteGroup( userId );
        }
        catch( Exception e )
        {
            rb = Response.status( Status.INTERNAL_SERVER_ERROR ).entity( exceptionToStr( e ) );
        }
        
        return rb.build();
    }

    
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public Response addGroup( String jsonData, @Context UriInfo uriInfo, @Context HttpHeaders headers )
    {
        ResponseBuilder rb = null;

        if( ( jsonData == null ) || ( jsonData.trim().length() == 0 ) )
        {
            rb = Response.status( Status.BAD_REQUEST ).entity( "No data is present with the call to " + uriInfo.getAbsolutePath() );
            return rb.build();
        }
        
        LOG.debug( "Data received at the URI {}\n{}", uriInfo.getAbsolutePath(), jsonData );
        
        try
        {
            RequestContext ctx = new RequestContext( provider, uriInfo, headers );
            
            provider.addGroup( jsonData, ctx );
            
            ServerResource res = ctx.getCoreResource();
            
            String json = ResourceSerializer.serialize( res );
            
            URI location = uriInfo.getBaseUriBuilder().build( res.getId() );
            
            rb = Response.created( location ).entity( json );
        }
        catch( Exception e )
        {
            rb = Response.status( Status.INTERNAL_SERVER_ERROR ).entity( exceptionToStr( e ) );
        }
        
        return rb.build();
    }

    
    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    public Response putGroup( String jsonData, @Context UriInfo uriInfo, @Context HttpHeaders headers )
    {
        ResponseBuilder rb = null;

        if( ( jsonData == null ) || ( jsonData.trim().length() == 0 ) )
        {
            rb = Response.status( Status.BAD_REQUEST ).entity( "No data is present with the call to " + uriInfo.getAbsolutePath() );
            return rb.build();
        }
        
        LOG.debug( "Data received at the URI {}\n{}", uriInfo.getAbsolutePath(), jsonData );
        
        try
        {
            RequestContext ctx = new RequestContext( provider, uriInfo, headers );
            
            ServerResource res = provider.putGroup( jsonData, ctx );
            
            String json = ResourceSerializer.serialize( res );
            
            URI location = uriInfo.getBaseUriBuilder().build( res.getId() );
            
            rb = Response.ok().entity( json ).location( location );
        }
        catch( Exception e )
        {
            rb = Response.status( Status.INTERNAL_SERVER_ERROR ).entity( exceptionToStr( e ) );
        }
        
        return rb.build();
    }

}
