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
package org.apache.directory.scim;


import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.apache.directory.scim.exception.EscimoException;
import org.apache.directory.scim.exception.MissingParameterException;
import org.apache.directory.scim.exception.ResourceNotFoundException;
import org.apache.directory.scim.schema.JsonSchema;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ResourceProvider
{
    String SERVLET_CONTEXT_ATTRIBUTE_KEY = "ESCIMO_PROVIDER";


    void init() throws EscimoException;


    void stop();


    RequestContext createCtx( UriInfo uriInfo, HttpServletRequest httpReq ) throws EscimoException;


    ServerResource getResource( RequestContext ctx, String id ) throws ResourceNotFoundException;


    InputStream getUserPhoto( String id, String atName, RequestContext ctx ) throws MissingParameterException;


    JsonSchema getJsonSchemaById( String id );


    List<JsonSchema> getJsonSchemas();


    ServerResource addResource( String jsonData, RequestContext ctx ) throws EscimoException;


    void deleteResource( String id, RequestContext ctx ) throws EscimoException;


    ServerResource putResource( String id, String jsonData, RequestContext ctx ) throws EscimoException;


    ServerResource patchResource( String id, String jsonData, RequestContext ctx ) throws EscimoException;


    ListResponse search( String filter, String attributes, RequestContext ctx ) throws EscimoException;


    String authenticate( String userName, String password ) throws EscimoException;


    void setAllowAuthorizedUsers( boolean allowAuthorizedUsers );


    List<String> getResourceUris();


    boolean isAllowAuthorizedUsers();


    JsonArray getAllResourceTypesSchema( String servletCtxPath );


    JsonObject getResourceTypeSchema( String servletCtxPath, String resName );
}
