/*
 * Copyright (c) 2006-2011 Mirth Corporation.
 * All rights reserved.
 *
 * NOTICE:  All information contained herein is, and remains, the
 * property of Mirth Corporation. The intellectual and technical
 * concepts contained herein are proprietary and confidential to
 * Mirth Corporation and may be covered by U.S. and Foreign
 * Patents, patents in process, and are protected by trade secret
 * and/or copyright law. Dissemination of this information or reproduction
 * of this material is strictly forbidden unless prior written permission
 * is obtained from Mirth Corporation.
 */

package org.apache.directory.scim.ldap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.scim.ProviderService;
import org.apache.directory.scim.RequestContext;

/**
 *
 * @author Kiran Ayyagari
 */
public class LdapRequestContext extends RequestContext
{
    private LdapConnection connection;
    
    public LdapRequestContext( ProviderService providerService, LdapConnection connection, UriInfo uriInfo, HttpServletRequest httpReq )
    {
        super( providerService, uriInfo, httpReq );
        this.connection = connection;
    }

    public LdapConnection getConnection() 
    {
        return connection;
    }
}
