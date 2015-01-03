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


import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.directory.scim.ResourceProvider;
import org.apache.directory.scim.json.ResourceSerializer;
import org.apache.directory.scim.schema.StatusCode;
import org.apache.directory.scim.schema.ErrorResponse;
import org.apache.directory.scim.schema.ErrorResponse.ScimError;
import org.apache.directory.scim.schema.JsonSchema;
import org.apache.directory.scim.schema.SchemaUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * TODO MetaSchemaService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Path("")
public class MetaSchemaService
{

    @Context
    private ServletContext servletCtx;

    // variable to cache the schema object
    private static String jsonSchemas;

    private static String resProviderSchema;

    @GET
    @Produces(
        { MediaType.APPLICATION_JSON })
    @Path("/Schemas")
    public Response getJsonSchemas( @Context UriInfo uriInfo )
    {
        return _getJsonSchema( null, uriInfo );
    }


    @GET
    @Produces(
        { MediaType.APPLICATION_JSON })
    @Path("/Schemas/{id}")
    public Response getJsonSchema( @PathParam("id") String schemaId, @Context UriInfo uriInfo )
    {
        return _getJsonSchema( schemaId, uriInfo );
    }


    @GET
    @Produces(
        { MediaType.APPLICATION_JSON })
    @Path("/ResourceTypes")
    public Response getResourceTypesSchema( @Context UriInfo uriInfo )
    {
        return _getResourceTypeSchema( null, uriInfo );
    }


    @GET
    @Produces(
        { MediaType.APPLICATION_JSON })
    @Path("/ResourceTypes/{resType}")
    public Response getResourceTypeSchema( @PathParam("resType") String resType, @Context UriInfo uriInfo )
    {
        return _getResourceTypeSchema( resType, uriInfo );
    }


    @GET
    @Produces(
        { MediaType.APPLICATION_JSON })
    @Path("/ServiceProviderConfig")
    public Response getSrviceProviderSchema( @Context UriInfo uriInfo )
    {
        ResponseBuilder rb = null;

        if ( resProviderSchema == null )
        {
            JsonObject obj = SchemaUtil.getResourceProviderConfig();
            
            JsonObject meta = obj.get( "meta" ).getAsJsonObject();
            meta.remove( "location" );
            
            meta.addProperty( "location", uriInfo.getBaseUri().toString() + "ServiceProviderConfig" );
            
            resProviderSchema = obj.toString();
        }

        if ( resProviderSchema != null )
        {
            rb = Response.ok( resProviderSchema, MediaType.APPLICATION_JSON );
        }
        else
        {
            ScimError err = new ScimError( StatusCode.NOT_FOUND, "No schema found with the URI "
                + SchemaUtil.PROVIDER_SERVICE_SCHEMA_ID );

            ErrorResponse resp = new ErrorResponse( err );
            String json = ResourceSerializer.serialize( resp );
            rb = Response.status( err.getCode().getVal() ).entity( json );
        }

        return rb.build();
    }


    private Response _getJsonSchema( String schemaId, UriInfo uriInfo )
    {
        ResponseBuilder rb = null;

        ResourceProvider provider = ( ResourceProvider ) servletCtx
            .getAttribute( ResourceProvider.SERVLET_CONTEXT_ATTRIBUTE_KEY );

        if ( schemaId == null )
        {
            List<JsonSchema> lst = provider.getJsonSchemas();

            if( jsonSchemas == null )
            {
                JsonArray arr = new JsonArray();
                
                JsonParser parser = new JsonParser();
                
                for ( JsonSchema js : lst )
                {
                    JsonElement je = parser.parse( js.getRawJson() );
                    arr.add( je );
                }
                
                jsonSchemas = arr.toString();
            }

            rb = Response.ok( jsonSchemas, MediaType.APPLICATION_JSON );
        }
        else
        {
            JsonSchema jsonSchema = provider.getJsonSchemaById( schemaId );

            if ( jsonSchema != null )
            {
                rb = Response.ok( jsonSchema.getRawJson(), MediaType.APPLICATION_JSON );
            }
            else
            {
                ScimError err = new ScimError( StatusCode.NOT_FOUND, "No schema found with the URI " + schemaId );

                ErrorResponse resp = new ErrorResponse( err );
                String json = ResourceSerializer.serialize( resp );
                rb = Response.status( err.getCode().getVal() ).entity( json );
            }
        }

        return rb.build();
    }


    private Response _getResourceTypeSchema( String resType, UriInfo uriInfo )
    {
        ResponseBuilder rb = null;

        ResourceProvider provider = ( ResourceProvider ) servletCtx
            .getAttribute( ResourceProvider.SERVLET_CONTEXT_ATTRIBUTE_KEY );

        String servletCtxPath = uriInfo.getBaseUri().toString();

        JsonElement el;

        if ( resType == null )
        {
            el = provider.getAllResourceTypesSchema( servletCtxPath );
        }
        else
        {
            el = provider.getResourceTypeSchema( servletCtxPath, resType );
        }

        if ( el != null )
        {
            rb = Response.ok( el.toString(), MediaType.APPLICATION_JSON );
        }
        else
        {
            ScimError err = new ScimError( StatusCode.NOT_FOUND, "No ResourceType found with the name " + resType );

            ErrorResponse resp = new ErrorResponse( err );
            String json = ResourceSerializer.serialize( resp );
            rb = Response.status( err.getCode().getVal() ).entity( json );
        }

        return rb.build();
    }

}
