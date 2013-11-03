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

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.scim.AttributeHandler;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.ldap.LdapUtil;
import org.apache.directory.scim.ldap.schema.ResourceSchema;
import org.apache.directory.scim.ldap.schema.SimpleType;
import org.apache.directory.scim.schema.BaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

/**
 * TODO LdapAttributeHandler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class LdapAttributeHandler extends AttributeHandler
{

    private static final Logger LOG = LoggerFactory.getLogger( LdapAttributeHandler.class );
    
    protected void checkHandler( BaseType bt, String name, LdapAttributeHandler selfRef )
    {
        if ( !bt.getName().equals( name ) )
        {
            String message = selfRef.getClass().getSimpleName() + " can  only be called on the " + name +
                             " attribute, invalid attribute name " + bt.getName();
            LOG.warn( message );
            throw new IllegalArgumentException( message );
        }
    }
    
    @Override
    public void write( BaseType atType, JsonElement jsonData, Object targetEntry, RequestContext ctx ) throws Exception
    {
        LdapUtil.scimToLdapAttribute( atType, jsonData, ( Entry ) targetEntry, ctx );
    }

    @Override
    public void patch( BaseType atType, JsonElement jsonData, Object entry, RequestContext ctx, Object patchCtx ) throws Exception
    {
        LdapUtil.patchLdapAttribute( atType, jsonData, ( Entry ) entry, ctx, ( ModifyRequest ) patchCtx );
    }

    @Override
    public void deleteAttribute( BaseType atType, Object targetEntry, RequestContext ctx, Object patchCtx ) throws Exception
    {
        LdapUtil.deleteAttribute( atType, (Entry) targetEntry, ( ModifyRequest ) patchCtx );
    }
    
    
    public AttributeType getLdapAtType( BaseType bt, String remainingScimAttributePath, ResourceSchema schema, SchemaManager ldapSchema )
    {
        if( !( bt instanceof SimpleType ) )
        {
            return null;
        }
        
        return ldapSchema.getAttributeType( ( ( SimpleType ) bt ).getMappedTo() );
    }
}
