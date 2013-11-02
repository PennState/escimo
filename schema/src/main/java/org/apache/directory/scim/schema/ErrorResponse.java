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


import java.util.ArrayList;
import java.util.List;


/**
 * TODO ErrorResponse.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ErrorResponse
{
    public static final String SCHEMA_ID = "urn:scim:schemas:core:2.0:Error";

    // named the variable with uppercase 'E'
    // to allow direct serialization using Gson
    private List<Error> Errors;


    public ErrorResponse( Error error )
    {
        addError( error );
    }


    public int getFirstErrorCode()
    {
        return Errors.get( 0 ).getCode();
    }
    
    public String getFirstErrorDesc()
    {
        return Errors.get( 0 ).getDescription();
    }

    public void addError( Error error )
    {
        if ( Errors == null )
        {
            Errors = new ArrayList<Error>();
        }

        Errors.add( error );
    }


    /**
     * @return the errors
     */
    public List<Error> getErrors()
    {
        return Errors;
    }


    /**
     * @param errors the errors to set
     */
    public void setErrors( List<Error> errors )
    {
        this.Errors = errors;
    }

    public static class Error
    {
        private String description;
        
        private int code;

        // this is an eSCIMo specific field used for 
        // debugging purpose
        private String stackTrace;

        public Error( int code, String description )
        {
            this.code = code;
            this.description = description;
        }


        /**
         * @return the stackTrace
         */
        public String getStackTrace()
        {
            return stackTrace;
        }


        /**
         * @param stackTrace the stackTrace to set
         */
        public void setStackTrace( String stackTrace )
        {
            this.stackTrace = stackTrace;
        }


        /**
         * @return the description
         */
        public String getDescription()
        {
            return description;
        }


        /**
         * @return the code
         */
        public int getCode()
        {
            return code;
        }
    }
}