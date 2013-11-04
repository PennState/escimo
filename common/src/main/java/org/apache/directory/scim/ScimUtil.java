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


import static org.apache.directory.scim.schema.ErrorCode.BAD_REQUEST;
import static org.apache.directory.scim.schema.ErrorCode.CONFLICT;
import static org.apache.directory.scim.schema.ErrorCode.INTERNAL_SERVER_ERROR;
import static org.apache.directory.scim.schema.ErrorCode.NOT_FOUND;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.directory.scim.json.ResourceSerializer;
import org.apache.directory.scim.schema.ErrorCode;
import org.apache.directory.scim.schema.ErrorResponse;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ScimUtil
{
    public static final String CORE_USER_URI = "urn:scim:schemas:core:2.0:User";
    public static final String CORE_GROUP_URI = "urn:scim:schemas:core:2.0:Group";
    public static final String CORE_EXT_USER_URI = "urn:scim:schemas:extension:enterprise:2.0:User";

    public static String exceptionToStr( Exception e )
    {
        StringWriter sw = new StringWriter();

        PrintWriter pw = new PrintWriter( sw );

        String msg = e.getMessage();
        if( msg != null )
        {
            pw.write( msg );
        }
        
        e.printStackTrace( pw );

        pw.close();

        return sw.toString();
    }

    
    public static boolean isCoreAttribute( String uri )
    {
        return ( CORE_USER_URI.equals( uri ) || 
                 CORE_GROUP_URI.equals( uri ) );
    }

    
    public static ResponseBuilder buildError( Exception e )
    {
        // set the default type to server error
        ErrorCode ec = INTERNAL_SERVER_ERROR;
        String desc = e.getMessage();
        
        if( ( e instanceof AttributeNotFoundException ) || ( e instanceof ResourceNotFoundException ) )
        {
            ec = NOT_FOUND;
        }
        else if ( e instanceof IllegalArgumentException )
        {
            ec = BAD_REQUEST;
        }
        else if ( e instanceof ResourceConflictException )
        {
            ec = CONFLICT;
        }
        
        if ( desc == null )
        {
            desc = ec.getDesc();
        }
        
        ErrorResponse.Error error = new ErrorResponse.Error( ec.getVal(), desc );
        
        error.setStackTrace( exceptionToStr( e ) );
        
        ErrorResponse erResp = new ErrorResponse( error );
        
        String json = ResourceSerializer.serialize( erResp );
        
        ResponseBuilder rb = Response.status( ec.getVal() ).entity( json );
        
        return rb;
    }
}
