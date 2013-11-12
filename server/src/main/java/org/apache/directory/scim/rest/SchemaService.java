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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.directory.scim.ProviderService;
import org.apache.directory.scim.json.ResourceSerializer;
import org.apache.directory.scim.schema.ErrorCode;
import org.apache.directory.scim.schema.ErrorResponse;
import org.apache.directory.scim.schema.ErrorResponse.ScimError;
import org.apache.directory.scim.schema.JsonSchema;

/**
 * TODO SchemaService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Path( "Schemas" )
public class SchemaService
{

    private ProviderService provider = ServerInitializer.getProvider();

    
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("{uri}")
    public Response getUser( @PathParam("uri") String schemaUri, @Context UriInfo uriInfo )
    {
        ResponseBuilder rb = null;
        
        JsonSchema jsonSchema = provider.getSchema( schemaUri );
        
        if( jsonSchema != null )
        {
            rb = Response.ok( jsonSchema.getRawJson(), MediaType.APPLICATION_JSON );
        }
        else
        {
            ScimError err = new ScimError( ErrorCode.NOT_FOUND, "No schema found with the URI " + schemaUri );
            
            ErrorResponse resp = new ErrorResponse( err );
            String json = ResourceSerializer.serialize( resp );
            rb = Response.status( err.getCode() ).entity( json );
        }
        
        return rb.build();
    }

}
