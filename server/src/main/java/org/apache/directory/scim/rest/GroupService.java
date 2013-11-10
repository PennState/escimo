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


import static org.apache.directory.scim.ScimUtil.buildError;
import static org.apache.directory.scim.ScimUtil.sendBadRequest;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.directory.scim.GroupResource;
import org.apache.directory.scim.ListResponse;
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
    
    @Context
    HttpServletRequest httpReq;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{id}")
    public Response getGroup( @PathParam("id") String groupId, @Context UriInfo uriInfo )
    {
        ResponseBuilder rb = null;
        
        try
        {
            RequestContext ctx = new RequestContext( provider, uriInfo, httpReq );
            
            GroupResource group = provider.getGroup( ctx, groupId );
            String json = ResourceSerializer.serialize( group );
            rb = Response.ok( json, MediaType.APPLICATION_JSON );
        }
        catch( ResourceNotFoundException e )
        {
            rb = buildError( e );
        }
        
        return rb.build();
    }

    
    @DELETE
    @Path("{id}")
    public Response deleteGroup( @PathParam("id") String groupId, @Context UriInfo uriInfo )
    {
        ResponseBuilder rb = Response.ok();
        
        try
        {
            provider.deleteGroup( groupId );
        }
        catch( Exception e )
        {
            rb = buildError( e );
        }
        
        return rb.build();
    }

    
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public Response addGroup( String jsonData, @Context UriInfo uriInfo )
    {
        ResponseBuilder rb = null;

        if( ( jsonData == null ) || ( jsonData.trim().length() == 0 ) )
        {
            return sendBadRequest( "No data is present with the call to " + uriInfo.getAbsolutePath() );
        }
        
        LOG.debug( "Data received at the URI {}\n{}", uriInfo.getAbsolutePath(), jsonData );
        
        try
        {
            RequestContext ctx = new RequestContext( provider, uriInfo, httpReq );
            
            provider.addGroup( jsonData, ctx );
            
            ServerResource res = ctx.getCoreResource();
            
            String json = ResourceSerializer.serialize( res );
            
            URI location = uriInfo.getBaseUriBuilder().build( res.getId() );
            
            rb = Response.created( location ).entity( json );
        }
        catch( Exception e )
        {
            rb = buildError( e );
        }
        
        return rb.build();
    }

    
    @PUT
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response putGroup( @PathParam("id") String groupId, String jsonData, @Context UriInfo uriInfo )
    {
        ResponseBuilder rb = null;

        if( ( jsonData == null ) || ( jsonData.trim().length() == 0 ) )
        {
            return sendBadRequest( "No data is present with the call to " + uriInfo.getAbsolutePath() );
        }
        
        LOG.debug( "Data received at the URI {}\n{}", uriInfo.getAbsolutePath(), jsonData );
        
        try
        {
            RequestContext ctx = new RequestContext( provider, uriInfo, httpReq );
            
            ServerResource res = provider.putGroup( groupId, jsonData, ctx );
            
            String json = ResourceSerializer.serialize( res );
            
            URI location = uriInfo.getBaseUriBuilder().build( res.getId() );
            
            rb = Response.ok().entity( json ).location( location );
        }
        catch( Exception e )
        {
            rb = buildError( e );
        }
        
        return rb.build();
    }

    @PATCH
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response patchGroup( @PathParam("id") String groupId, String jsonData, @Context UriInfo uriInfo )
    {
        ResponseBuilder rb = null;

        if( ( jsonData == null ) || ( jsonData.trim().length() == 0 ) )
        {
            return sendBadRequest( "No data is present with the call to " + uriInfo.getAbsolutePath() );
        }
        
        LOG.debug( "Data received at the URI {}\n{}", uriInfo.getAbsolutePath(), jsonData );
        
        try
        {
            RequestContext ctx = new RequestContext( provider, uriInfo, httpReq );
            
            ServerResource resource = provider.patchGroup( groupId, jsonData, ctx );
            
            if( resource == null )
            {
                rb = Response.status( Status.NO_CONTENT );
            }
            else
            {
                String json = ResourceSerializer.serialize( resource );
                rb = Response.ok().entity( json );
            }
        }
        catch( Exception e )
        {
            rb = buildError( e );
        }
        
        return rb.build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response search( @QueryParam("filter") String filter, @QueryParam("attributes") String attributes, @Context UriInfo uriInfo )
    {
        ResponseBuilder rb = null;

        if( ( ( filter == null ) || ( filter.trim().length() == 0 ) ) &&
            ( ( attributes == null ) || ( attributes.trim().length() == 0 ) ) )
        {
            return sendBadRequest( "Neither filter nor attributes parameter is present with the call to " + uriInfo.getAbsolutePath() );
        }
        
        LOG.debug( "Filter : {}", filter );
        LOG.debug( "Attributes : {}", attributes );
    
        try
        {
            RequestContext ctx = new RequestContext( provider, uriInfo, httpReq );
            ListResponse lr = provider.search( filter, attributes, ctx );

            String json = ResourceSerializer.serialize( lr );
            rb = Response.ok().entity( json );
        }
        catch( Exception e )
        {
            rb = buildError( e );
        }

        return rb.build();
    }

}
