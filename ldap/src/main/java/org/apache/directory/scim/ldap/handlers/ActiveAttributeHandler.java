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


import java.util.List;

import org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.ldap.schema.ResourceSchema;
import org.apache.directory.scim.schema.BaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;


/**
 * TODO ActiveAttributeHandler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ActiveAttributeHandler extends LdapAttributeHandler
{

    private static final Logger LOG = LoggerFactory.getLogger( ActiveAttributeHandler.class );


    @Override
    public void read( BaseType bt, Object srcResource, RequestContext ctx ) throws Exception
    {
        if ( !bt.getName().equals( "active" ) )
        {
            LOG.debug( "ActiveAttributeHandler can  only be called on the active attribute, invalid attribute name {}",
                bt.getName() );
            return;
        }

        Entry entry = ( Entry ) srcResource;

        Attribute lockAt = entry.get( PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT );

        SimpleAttribute st = null;

        if ( lockAt != null )
        {
            try
            {
                if ( "000001010000Z".equals( lockAt.getString() ) )
                {
                    st = new SimpleAttribute( bt.getName(), Boolean.FALSE );
                }
            }
            catch ( LdapException e )
            {
                LOG.warn( "Failed to get the value for the attribute {}", bt.getName(), e );
                throw e;
            }
        }

        if ( st == null )
        {
            st = new SimpleAttribute( bt.getName(), Boolean.TRUE );
        }

        ctx.getCoreResource().addAttribute( bt.getUri(), st );
    }


    @Override
    public void write( BaseType atType, JsonElement jsonData, Object targetEntry, RequestContext ctx ) throws Exception
    {
    }


    @Override
    public void patch( BaseType atType, JsonElement jsonData, Object entry, RequestContext ctx, Object patchCtx )
        throws Exception
    {
    }


    @Override
    public void deleteAttribute( BaseType atType, Object targetEntry, RequestContext ctx, Object patchCtx )
        throws Exception
    {
    }

}
