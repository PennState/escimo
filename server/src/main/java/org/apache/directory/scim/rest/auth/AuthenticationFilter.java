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


import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.scim.ResourceProvider;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.ScimUtil;
import org.apache.directory.scim.json.ResourceSerializer;
import org.apache.directory.scim.schema.StatusCode;
import org.apache.directory.scim.schema.ErrorResponse;
import org.apache.directory.scim.schema.ErrorResponse.ScimError;

/**
 * 
 * TODO AuthenticationFilter.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticationFilter implements Filter
{

    private EscimoAuthenticator authenticator;

    private ResourceProvider provider;
    
    public void destroy()
    {
    }


    public void doFilter( ServletRequest req, ServletResponse resp, FilterChain chain ) throws IOException,
        ServletException
    {
        if( authenticator == null )
        {
            chain.doFilter( req, resp );
            return;
        }
        
        HttpServletRequest httpReq = ( HttpServletRequest ) req;
        HttpServletResponse httpResp = ( HttpServletResponse ) resp;

        // /Schemas serves read-only resources
        if( httpReq.getRequestURI().contains( "/Schemas" ) )
        {
            chain.doFilter( req, resp );
            return;
        }
        
        String userHeader = httpReq.getHeader( RequestContext.USER_AUTH_HEADER );
        
        if( ( userHeader != null ) && ( userHeader.trim().length() > 0 ) )
        {
            chain.doFilter( req, resp );
            return;
        }
        
        String authToken = null;
        String errorMsg = null;
        
        String authHeader = httpReq.getHeader( "Authorization" );
        
        if( authHeader == null )
        {
            httpResp.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
            httpResp.setHeader( "WWW-Authenticate", "Basic realm=escimo" );
            return;
        }
        
        try
        {
            authToken = authenticator.authenticate( ( HttpServletRequest ) req, provider );
        }
        catch( Exception e )
        {
            errorMsg = ScimUtil.exceptionToStr( e );
        }
        
        
        if( authToken == null )
        {
            if( errorMsg == null )
            {
                errorMsg = "Not authenticated";
            }
            
            ScimError error = new ScimError( StatusCode.UNAUTHORIZED, errorMsg );
            ErrorResponse erResp = new ErrorResponse( error );
            
            String json = ResourceSerializer.serialize( erResp );
            
            httpResp.setStatus( error.getCode().getVal() );
            httpResp.getWriter().write( json );
            
            return;
        }

        httpResp.setHeader( RequestContext.USER_AUTH_HEADER, authToken );
        
        chain.doFilter( new EscimoHttpServletRequest( httpReq, authToken ), resp );
    }


    public void init( FilterConfig filterConfig ) throws ServletException
    {
        String authMethod = filterConfig.getInitParameter( "authMethod" );
        
        if( StringUtils.isEmpty( authMethod ) )
        {
            authMethod = "NONE";
        }
        
        if( authMethod.equalsIgnoreCase( "BASIC" ) )
        {
            authenticator = new BasicAuthenticator();
        }
        
        if( authenticator != null )
        {
            provider = ( ResourceProvider ) filterConfig.getServletContext().getAttribute( ResourceProvider.SERVLET_CONTEXT_ATTRIBUTE_KEY );
            provider.setAllowAuthorizedUsers( true );
        }
    }

}
