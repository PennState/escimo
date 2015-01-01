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
package org.apache.directory.scim.schema;

/**
 * TODO ErrorCode.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum ErrorCode
{

    TEMPORARY_REDIRECT(307, null, " The client is directed to repeat the same HTTP request at the location identified."
                            + " The client  SHOULD NOT use the location provided in the response as a permanent reference to the"
                            + " resource and SHOULD continue to use the original request URI"),
                            
    PERMANENT_REDIRECT(308, null, "The client is directed to repeat the same HTTP request at the location identified. The client"
                            + " SHOULD use the location provided in the response as the permanent reference to the resource"),
                            
    BAD_REQUEST(400, null, "Request is unparseable, syntactically incorrect, or violates schema"),
    
    UNAUTHORIZED(401, null, "Authorization failure"),
    
    FORBIDDEN(403, null, "Server does not support requested operation"),
    
    NOT_FOUND(404, null, "Specified resource does not exist"),
    
    CONFLICT(409, null, "The specified version number does not match the resource's latest version number or a Service Provider refused"
                        + " to create a new, duplicate resource"),
    
    PRECONDITION_FAILED(412, null, "Failed to update as Resource changed on the server since last retrieved"),
    
    REQUEST_ENTITY_TOO_LARGE(413, null, "Requested entity too large"),
    
    INTERNAL_SERVER_ERROR(500, null, "Internal server error"),
    
    NOT_IMPLEMENTED(501, null, "Service Provider does not support the requested operation"),
    
    BAD_REQUEST_INVALID_FILTER(400, "invalidFilter", "The specified filter syntax was invalid (does not comply with Figure 1) or the "
                                                     + "specified attribute and filter comparison combination is not supported."),
    
    BAD_REQUEST_TOO_MANY(400, "tooMany", "The specified filter yields many more results than the server is willing calculate or process."
                                         + " For example, a filter such as \"(userName pr)\" by itself would return all entries with a "
                                         + "\"userName\" and MAY not be acceptable to the service provider."),
    
    BAD_REQUEST_UNIQUENESS(400, "uniqueness", "One or more of attribute values is already in use or is reserved."),
    
    BAD_REQUEST_MUTABILITY(400, "mutability", "The attempted modification is not compatible with the target attributes mutability or current"
                                              + " state (e.g. modification of an immutable attribute with an existing value)."),
    
    BAD_REQUEST_INVALID_SYNTAX(400, "invalidSyntax", "The request body message structure was invalid or did not conform to the request schema."),

    BAD_REQUEST_INVALID_PATH(400, "invalidPath", "The path attribute was invalid or malformed (see Figure 7)."),
    
    BAD_REQUEST_NOTARGET(400, "noTarget", "The specified \"path\" did not yield an attribute or attribute value that could be operated on. "
                                          + "This occurs when the specified \"path\" value contains a filter that yields no match."),
    
    BAD_REQUEST_INVALID_VALUE(400, "invalidValue", "A required value was missing, or the value specified was not compatible with the operation"
                                                   + " or attribute type."),
    
    BAD_REQUEST_INVALID_VERSION(400, "invalidVers", "The specified SCIM protocol version is not supported");
    
    private int val;
    
    private String desc;
    
    private String scimType;
    
    private ErrorCode( int val, String scimType, String desc )
    {
        this.val = val;
        this.scimType = scimType;
        this.desc = desc;
    }

    /**
     * @return the val
     */
    public int getVal()
    {
        return val;
    }

    
    /**
     * @return the scimType desc
     */
    String getScimType()
    {
        return scimType;
    }

    
    /**
     * @return the desc
     */
    public String getDesc()
    {
        return desc;
    }
}
