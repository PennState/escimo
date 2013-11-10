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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.directory.scim.schema.CoreResource;
import org.apache.directory.scim.schema.ErrorResponse;
import org.apache.http.Header;


/**
 * TODO EscimoResult.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EscimoResult
{
    private int httpStatusCode;

    private ErrorResponse errorResponse;

    private CoreResource resource;

    private Header[] headers;


    public EscimoResult( int httpStatusCode, Header[] headers )
    {
        this.httpStatusCode = httpStatusCode;
        this.headers = headers;
    }

    public boolean isSuccess()
    {
        return ( errorResponse == null );
    }

    public Header getHeader( String name )
    {
        for ( Header h : headers )
        {
            if ( h.getName().equals( name ) )
            {
                return h;
            }
        }

        return null;
    }


    public List<Header> getHeaders( String name )
    {
        List<Header> lst = new ArrayList<Header>();

        for ( Header h : headers )
        {
            if ( h.getName().equals( name ) )
            {
                lst.add( h );
            }
        }

        return lst;
    }


    /**
     * @return the httpStatusCode
     */
    public int getHttpStatusCode()
    {
        return httpStatusCode;
    }


    /**
     * @return the errorResponse
     */
    public ErrorResponse getErrorResponse()
    {
        return errorResponse;
    }


    /**
     * @param errorResponse the errorResponse to set
     */
    public void setErrorResponse( ErrorResponse errorResponse )
    {
        this.errorResponse = errorResponse;
    }


    /**
     * @return the resource
     */
    public CoreResource getResource()
    {
        return resource;
    }


    public <T> T getResourceAs( Class<T> typeOfresource )
    {
        return ( T ) resource;
    }

    /**
     * @param resource the resource to set
     */
    public void setResource( CoreResource resource )
    {
        this.resource = resource;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "EscimoResult [httpStatusCode=" + httpStatusCode + ", errorResponse=" + errorResponse + ", resource="
            + resource + ", headers=" + Arrays.toString( headers ) + "]";
    }

}
