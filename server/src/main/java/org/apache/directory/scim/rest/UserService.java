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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.directory.scim.MissingParameterException;
import org.apache.directory.scim.ProviderService;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.ResourceNotFoundException;
import org.apache.directory.scim.UserResource;
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
            RequestContext ctx = new RequestContext( provider );
            ctx.setUriInfo( uriInfo );
            
            UserResource user = provider.getUser( ctx, userId );
            String json = ResourceSerializer.serialize( user );
            rb = Response.ok( json, MediaType.APPLICATION_JSON );
        }
        catch( ResourceNotFoundException e )
        {
            rb = Response.status( Status.INTERNAL_SERVER_ERROR );
        }
        
        return rb.build();
    }
    
    
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public Response addUser( String jsonData, @Context UriInfo uriInfo )
    {
        ResponseBuilder rb = null;

        if( ( jsonData == null ) || ( jsonData.trim().length() == 0 ) )
        {
            rb = Response.status( Status.BAD_REQUEST ).entity( "No data is present with the call to " + uriInfo.getPath() );
            return rb.build();
        }
        
        try
        {
            RequestContext ctx = new RequestContext( provider );
            ctx.setUriInfo( uriInfo );
            
            provider.addUser( jsonData, ctx );
            
            String json = ResourceSerializer.serialize( ctx.getCoreResource() );
            rb = Response.ok( json, MediaType.APPLICATION_JSON );
        }
        catch( Exception e )
        {
            rb = Response.status( Status.INTERNAL_SERVER_ERROR );
        }
        
        return rb.build();
    }
    
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    @Path("photo")
    public Response getPhoto( @QueryParam("atName") String atName, @QueryParam("id") String id )
    {
        final ResponseBuilder rb = Response.ok();
        
        try
        {
            final InputStream in = provider.getUserPhoto( id, atName );
            if( in == null )
            {
                rb.status( Status.NOT_FOUND );
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
                        catch( IOException e )
                        {
                            rb.status( Status.INTERNAL_SERVER_ERROR );
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
            rb.status( Status.BAD_REQUEST );
        }
        
        return rb.build();

    }
}
