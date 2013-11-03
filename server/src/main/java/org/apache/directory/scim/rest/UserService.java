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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.directory.scim.ListResponse;
import org.apache.directory.scim.MissingParameterException;
import org.apache.directory.scim.ProviderService;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.ScimUtil;
import org.apache.directory.scim.ServerResource;
import org.apache.directory.scim.UserResource;
import org.apache.directory.scim.json.ResourceSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Path( "Users" )
public class UserService
{

    private ProviderService provider = ServerInitializer.getProvider();

    private static final Logger LOG = LoggerFactory.getLogger( UserService.class );
    
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{id}")
    public Response getUser( @PathParam("id") String userId, @Context UriInfo uriInfo, @Context HttpHeaders headers )
    {
        ResponseBuilder rb = null;
        
        try
        {
            RequestContext ctx = new RequestContext( provider, uriInfo, headers );
            
            UserResource user = provider.getUser( ctx, userId );
            String json = ResourceSerializer.serialize( user );
            rb = Response.ok( json, MediaType.APPLICATION_JSON );
        }
        catch( Exception e )
        {
            rb = ScimUtil.buildError( e );
        }
        
        return rb.build();
    }
    

    @DELETE
    @Path("{id}")
    public Response deleteUser( @PathParam("id") String userId, @Context UriInfo uriInfo, @Context HttpHeaders headers )
    {
        ResponseBuilder rb = Response.ok();
        
        try
        {
            provider.deleteUser( userId );
        }
        catch( Exception e )
        {
            rb = ScimUtil.buildError( e );
        }
        
        return rb.build();
    }
    
    
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public Response addUser( String jsonData, @Context UriInfo uriInfo, @Context HttpHeaders headers )
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
            
            provider.addUser( jsonData, ctx );
            
            ServerResource res = ctx.getCoreResource();
            
            String json = ResourceSerializer.serialize( res );
            
            URI location = uriInfo.getBaseUriBuilder().build( res.getId() );
            
            rb = Response.created( location ).entity( json );
        }
        catch( Exception e )
        {
            rb = ScimUtil.buildError( e );
        }
        
        return rb.build();
    }

    
    @PUT
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response putUser( @PathParam("id") String userId, String jsonData, @Context UriInfo uriInfo, @Context HttpHeaders headers )
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
            
            ServerResource res = provider.putUser( userId, jsonData, ctx );
            
            String json = ResourceSerializer.serialize( res );
            
            URI location = uriInfo.getBaseUriBuilder().build( res.getId() );
            
            rb = Response.ok().entity( json ).location( location );
        }
        catch( Exception e )
        {
            rb = ScimUtil.buildError( e );
        }
        
        return rb.build();
    }

    
    @PATCH
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response patchUser( @PathParam("id") String userId, String jsonData, @Context UriInfo uriInfo, @Context HttpHeaders headers )
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
            
            ServerResource resource = provider.patchUser( userId, jsonData, ctx );
            
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
            rb = ScimUtil.buildError( e );
        }
        
        return rb.build();
    }


    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response search( @QueryParam("filter") String filter, @QueryParam("attributes") String attributes, @Context UriInfo uriInfo, @Context HttpHeaders headers )
    {
        ResponseBuilder rb = null;

        if( ( ( filter == null ) || ( filter.trim().length() == 0 ) ) &&
            ( ( attributes == null ) || ( attributes.trim().length() == 0 ) ) )
        {
            rb = Response.status( Status.BAD_REQUEST ).entity( "Neither filter nor attributes parameters are present with the call to " + uriInfo.getAbsolutePath() );
            return rb.build();
        }
        
        LOG.debug( "Filter : {}", filter );
        LOG.debug( "Attributes : {}", attributes );
    
        try
        {
            RequestContext ctx = new RequestContext( provider, uriInfo, headers );
            ListResponse lr = provider.search( filter, attributes, ctx );

            String json = ResourceSerializer.serialize( lr );
            rb = Response.ok().entity( json );
        }
        catch( Exception e )
        {
            rb = ScimUtil.buildError( e );
        }

        return rb.build();
    }

    
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    @Path("photo")
    public Response getPhoto( @QueryParam("atName") String atName, @QueryParam("id") String id )
    {
        ResponseBuilder rb = Response.ok();
        
        try
        {
            final InputStream in = provider.getUserPhoto( id, atName );
            if( in == null )
            {
                rb.status( Status.NOT_FOUND ).entity( "No photo found for the resource with ID " + id + " and attribute name " + atName );
            }
            else
            {
                StreamingOutput streamOut = new StreamingOutput()
                {
                    
                    public void write( OutputStream output ) throws IOException, WebApplicationException
                    {
                        byte[] buf = new byte[1024];
                        int read = -1;
                        try
                        {
                            while( true )
                            {
                                read = in.read( buf );
                                if( read <= 0 )
                                {
                                    break;
                                }
                                
                                output.write( buf, 0, read );
                            }
                        }
                        finally
                        {
                            in.close();
                        }
                    }
                };
                
                rb.entity( streamOut );
            }
        }
        catch( MissingParameterException e )
        {
            rb = ScimUtil.buildError( e );
        }
        
        return rb.build();

    }
}
