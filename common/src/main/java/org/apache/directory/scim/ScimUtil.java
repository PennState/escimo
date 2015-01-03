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
import static org.apache.directory.scim.schema.ErrorCode.UNAUTHORIZED;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.directory.scim.exception.AttributeNotFoundException;
import org.apache.directory.scim.exception.ResourceConflictException;
import org.apache.directory.scim.exception.ResourceNotFoundException;
import org.apache.directory.scim.exception.UnauthorizedException;
import org.apache.directory.scim.json.ResourceSerializer;
import org.apache.directory.scim.schema.ErrorCode;
import org.apache.directory.scim.schema.ErrorResponse;
import org.apache.directory.scim.schema.ErrorResponse.ScimError;
import org.apache.directory.scim.schema.SchemaUtil;
import org.apache.directory.scim.schema.ScimType;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ScimUtil
{
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
        return ( SchemaUtil.CORE_USER_ID.equals( uri ) || 
                 SchemaUtil.CORE_GROUP_ID.equals( uri ) );
    }

    
    public static ResponseBuilder buildError( Exception e )
    {
        // set the default type to server error
        ErrorCode ec = INTERNAL_SERVER_ERROR;
        String detail = e.getMessage();
        ScimType scimType = null;
        
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
            scimType = ScimType.UNIQUENESS;
        }
        else if ( e instanceof UnauthorizedException )
        {
            ec = UNAUTHORIZED;
        }
        
        if ( detail == null )
        {
            detail = ec.getDetail();
        }
        
        ErrorResponse.ScimError error = new ErrorResponse.ScimError( ec, scimType, detail );
        
        error.setStackTrace( exceptionToStr( e ) );
        
        ErrorResponse erResp = new ErrorResponse( error );
        
        String json = ResourceSerializer.serialize( erResp );
        
        ResponseBuilder rb = Response.status( ec.getVal() ).entity( json );
        
        return rb;
    }

    
    public static Response sendBadRequest( String message )
    {
        ScimError err = new ScimError( ErrorCode.BAD_REQUEST, message );
        
        ErrorResponse resp = new ErrorResponse( err );
        String json = ResourceSerializer.serialize( resp );
        ResponseBuilder rb = Response.status( err.getCode().getVal() ).entity( json );
        
        return rb.build();
    }
    
    public static void main( String[] args )
    {
        ErrorResponse.ScimError error = new ErrorResponse.ScimError( ErrorCode.BAD_REQUEST, ScimType.INVALID_FILTER, "detail error" );
        
        ErrorResponse erResp = new ErrorResponse( error );
        
        String json = ResourceSerializer.serialize( erResp );
        
        System.out.println( json );
    }
}
