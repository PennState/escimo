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
package org.apache.directory.scim.rest.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.directory.scim.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO BasicAuthenticator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BasicAuthenticator implements EscimoAuthenticator
{
    private static final Logger LOG = LoggerFactory.getLogger( BasicAuthenticator.class );
    
    public String authenticate( HttpServletRequest req, ResourceProvider provider ) throws Exception
    {
        String authHeader = req.getHeader( "Authorization" );
        
        LOG.debug( "received authorization header {}", authHeader );
        
        int pos = authHeader.indexOf( ' ' );
        if( pos <= 0 )
        {
            return null;
        }
        
        authHeader = new String( Base64.decodeBase64( authHeader.substring( pos + 1 ) ) );
        
        String[] credentials = authHeader.split( ":" );
        
        if( credentials.length > 1 )
        {
            String headerVal = provider.authenticate( credentials[0], credentials[1] );
            return headerVal;
        }
        
        return null;
    }
}
