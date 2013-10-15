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

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.scim.AttributeHandler;
import org.apache.directory.scim.ComplexAttribute;
import org.apache.directory.scim.ServerResource;
import org.apache.directory.scim.GroupResource;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.schema.BaseType;
import org.apache.directory.scim.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO MetaAttributeHandler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MetaAttributeHandler extends AttributeHandler
{

    private static final Logger LOG = LoggerFactory.getLogger( ActiveAttributeHandler.class );
    
    @Override
    public void read( BaseType bt, Object srcResource, RequestContext ctx )
    {
        Entry entry = ( Entry ) srcResource;

        try
        {
            List<SimpleAttribute> atList = new ArrayList<SimpleAttribute>();
            
            Attribute atCreated = entry.get( SchemaConstants.CREATE_TIMESTAMP_AT );
            String createTimestamp = null;
            if( atCreated != null )
            {
                SimpleAttribute created = new SimpleAttribute( "created" );
                createTimestamp = ResourceUtil.formatDate( atCreated.getString() );
                created.setValue( createTimestamp );
                atList.add( created );
            }

            Attribute atlastMod = entry.get( SchemaConstants.MODIFY_TIMESTAMP_AT );
            SimpleAttribute lastModified = null;
            if( atlastMod != null )
            {
                lastModified = new SimpleAttribute( "lastModified" );
                lastModified.setValue( ResourceUtil.formatDate( atlastMod.getString() ) );
            }
            else if( createTimestamp != null )
            {
                lastModified = new SimpleAttribute( "lastModified" );
                lastModified.setValue( createTimestamp );
            }
            
            if( atlastMod != null )
            {
                atList.add( lastModified );
            }

            ServerResource resource = ctx.getCoreResource();
            
            String resourceType = "User";
            
            if( resource instanceof GroupResource )
            {
                resourceType = "Group";
            }
            
            SimpleAttribute resourceTypeAt = new SimpleAttribute( "resourceType", resourceType );
            atList.add( resourceTypeAt );
            
            SimpleAttribute location = new SimpleAttribute( "location" );
            String locationVal = ctx.getUriInfo().getBaseUri().toString();
            locationVal = locationVal + resourceType + "s/" + resource.getId();
            
            location.setValue( locationVal );
            atList.add( location );
            
            ComplexAttribute ct = new ComplexAttribute( bt.getName(), atList );
            resource.addAttribute( bt.getUri(), ct );
        }
        catch( LdapException e )
        {
            LOG.warn( "Failed while creating meta attribute", e );
        }
        
    }

}
