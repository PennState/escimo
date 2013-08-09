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
package org.apache.directory.scim.ldap.handlers;

import org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.scim.AttributeHandler;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.schema.BaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO ActiveAttributeHandler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ActiveAttributeHandler implements AttributeHandler
{

    private static final Logger LOG = LoggerFactory.getLogger( ActiveAttributeHandler.class );
    
    @Override
    public void handle( BaseType bt, Object srcResource, RequestContext ctx )
    {
        if( !bt.getName().equals( "active" ) )
        {
            LOG.debug( "ActiveAttributeHandler can  only be called on the active attribute, invalid attribute name {}", bt.getName() );
            return;
        }
        
        Entry entry = ( Entry ) srcResource;
        
        Attribute lockAt = entry.get( PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT );
        
        SimpleAttribute st = null;
        
        if( lockAt != null )
        {
            try
            {
                if( "000001010000Z".equals( lockAt.getString() ) )
                {
                    st = new SimpleAttribute( bt.getName(), Boolean.FALSE );
                }
            }
            catch( LdapException e )
            {
                LOG.warn( "Failed to get the value for the attribute {}", bt.getName(), e );
            }
        }
        
        if ( st == null )
        {
            st = new SimpleAttribute( bt.getName(), Boolean.TRUE );
        }
        
        ctx.getUser().addAttribute( bt.getUri(), st );
    }

    }
