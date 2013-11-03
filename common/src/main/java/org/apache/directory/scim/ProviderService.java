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

import org.apache.directory.scim.schema.JsonSchema;

/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ProviderService 
{
    void init() throws Exception;
    
    void stop();
    
    UserResource getUser( RequestContext ctx, String userId ) throws ResourceNotFoundException;
    
    InputStream getUserPhoto( String id, String atName ) throws MissingParameterException;
    
    GroupResource getGroup( RequestContext ctx, String groupId ) throws ResourceNotFoundException;
    
    JsonSchema getSchema( String uriId );
    
    UserResource addUser( String jsonData, RequestContext ctx ) throws Exception;
    
    GroupResource addGroup( String jsonData, RequestContext ctx ) throws Exception;
    
    void deleteUser( String id ) throws Exception;
    
    void deleteGroup( String id ) throws Exception;
    
    UserResource putUser( String userId, String jsonData, RequestContext ctx ) throws Exception;
    
    GroupResource putGroup( String groupId, String jsonData, RequestContext ctx ) throws Exception;

    UserResource patchUser( String userId, String jsonData, RequestContext ctx ) throws Exception;
    
    GroupResource patchGroup( String groupId, String jsonData, RequestContext ctx ) throws Exception;
    
    ListResponse search( String filter, String attributes, RequestContext ctx ) throws Exception;
}
