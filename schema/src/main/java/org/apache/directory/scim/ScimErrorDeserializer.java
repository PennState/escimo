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

package org.apache.directory.scim;


import java.lang.reflect.Type;

import org.apache.directory.scim.schema.ErrorResponse.ScimError;
import org.apache.directory.scim.schema.StatusCode;
import org.apache.directory.scim.schema.ScimType;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


/**
 * TODO ScimErrorDeserializer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ScimErrorDeserializer implements JsonDeserializer<ScimError>, JsonSerializer<ScimError>
{

    public JsonElement serialize( ScimError src, Type typeOfSrc, JsonSerializationContext context )
    {
        JsonObject obj = new JsonObject();
        
        if ( src.getScimType() != null )
        {
            obj.addProperty( "scimType", src.getScimType().getVal() );
        }

        obj.addProperty( "status", src.getCode().getVal() );
        obj.addProperty( "detail", src.getCode().getDetail() );
        
        return obj;
    }

    public ScimError deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context )
        throws JsonParseException
    {
        JsonObject obj = ( JsonObject ) json;

        JsonPrimitive elScimType = ( JsonPrimitive ) obj.get( "scimType" );
        
        ScimType scimType = null;
        
        if( elScimType != null )
        {
            scimType = ScimType.getByVal( elScimType.getAsString() );
        }
        
        
        JsonPrimitive elStatus = ( JsonPrimitive ) obj.get( "status" );
        
        StatusCode status = null;
        
        if ( elStatus != null )
        {
            status = StatusCode.getByVal( elStatus.getAsInt() );
        }

        JsonPrimitive elDetail = ( JsonPrimitive ) obj.get( "detail" );
        
        String detail = null;
        
        if( elDetail != null )
        {
            detail = elDetail.getAsString();
        }
        
        ScimError scimErr = new ScimError( status, scimType, detail );
        
        return scimErr;
    }

}
