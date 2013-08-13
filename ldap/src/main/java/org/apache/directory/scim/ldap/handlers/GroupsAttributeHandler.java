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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.BinaryValue;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.filter.FilterVisitor;
import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.util.Strings;
import org.apache.directory.scim.AttributeHandler;
import org.apache.directory.scim.MultiValAttribute;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.SimpleAttributeGroup;
import org.apache.directory.scim.ldap.LdapResourceProvider;
import org.apache.directory.scim.ldap.schema.MultiValType;
import org.apache.directory.scim.schema.BaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO GroupsAttributeHandler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GroupsAttributeHandler implements AttributeHandler
{

    private static final Logger LOG = LoggerFactory.getLogger( GroupsAttributeHandler.class );


    @Override
    public void handle( BaseType bt, Object srcResource, RequestContext ctx )
    {
        if ( !bt.getName().equals( "groups" ) )
        {
            LOG.warn( "GroupsAttributeHandler can only be used on groups multivalue attribute, invalid attribute name {}", bt.getName() );
            return;
        }

        Entry userEntry = ( Entry ) srcResource;

        List<Entry> members = null;

        Attribute memberAt = userEntry.get( SchemaConstants.MEMBER_AT );
        if ( memberAt != null )
        {
            members = getMemberEntries( memberAt, ( LdapResourceProvider ) ctx.getProviderService() );
        }
        else
        // query members based on the filter and base DN
        {
            MultiValType mvt = ( MultiValType ) bt;
            members = getMemberEntriesUsingFilter( mvt.getFilter(), mvt.getBaseDn(), userEntry,
                ( LdapResourceProvider ) ctx.getProviderService() );
        }

        if ( ( members != null ) && ( !members.isEmpty() ) )
        {
            MultiValAttribute mv = new MultiValAttribute( bt.getName() );

            for ( Entry memberEntry : members )
            {

                try
                {
                    List<SimpleAttribute> lst = new ArrayList<SimpleAttribute>();

                    SimpleAttribute id = new SimpleAttribute( "id", memberEntry.get( SchemaConstants.ENTRY_UUID_AT )
                        .getString() );
                    lst.add( id );

                    String locationVal = ctx.getUriInfo().getBaseUri().toString();
                    locationVal = locationVal + "Groups/" + id.getValue();

                    SimpleAttribute ref = new SimpleAttribute( "$ref", locationVal );
                    lst.add( ref );

                    SimpleAttribute display = new SimpleAttribute( "display", memberEntry.getDn().getRdn().getValue()
                        .getString() );
                    lst.add( display );

                    SimpleAttributeGroup sg = new SimpleAttributeGroup( lst );

                    mv.addAtGroup( sg );
                }
                catch ( LdapException ex )
                {
                    LOG.warn( "Failed to get attributes from entry {}", memberEntry.getDn() );
                }
            }

            ctx.getUser().addAttribute( bt.getUri(), mv );
        }
    }


    private List<Entry> getMemberEntries( Attribute memberAt, LdapResourceProvider provider )
    {
        List<Entry> members = new ArrayList<Entry>();

        Iterator<Value<?>> itr = memberAt.iterator();
        while ( itr.hasNext() )
        {
            Value<?> dn = itr.next();
            Entry entry = provider.fetchEntryByDn( dn.getString() );
            if ( entry != null )
            {
                members.add( entry );
            }
        }

        return members;
    }


    private List<Entry> getMemberEntriesUsingFilter( String filter, String baseDn, Entry userEntry,
        LdapResourceProvider provider )
    {
        if ( Strings.isEmpty( baseDn ) )
        {
            baseDn = ""; // RootDSE
        }

        if ( Strings.isEmpty( filter ) )
        {
            return Collections.EMPTY_LIST;
        }

        List<Entry> lst = new ArrayList<Entry>();

        try
        {
            ExprNode rootNode = FilterParser.parse( filter );

            FilterTokenVisitor tv = new FilterTokenVisitor( userEntry );
            tv.visit( rootNode );

            EntryCursor cursor = provider.getConnection().search( baseDn, rootNode.toString(), SearchScope.SUBTREE,
                SchemaConstants.ALL_ATTRIBUTES_ARRAY );
            while ( cursor.next() )
            {
                Entry mvEntry = cursor.get();
                lst.add( mvEntry );
            }

            cursor.close();
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to get the groups using the filter {} and base DN {}", filter, baseDn );
            LOG.warn( "", e );
        }

        return lst;
    }

    class FilterTokenVisitor implements FilterVisitor
    {
        private Entry entry;


        FilterTokenVisitor( Entry entry )
        {
            this.entry = entry;
        }


        public boolean canVisit( ExprNode node )
        {
            return node instanceof ExprNode;
        }


        public List<ExprNode> getOrder( BranchNode node, List<ExprNode> children )
        {
            return children;
        }


        public boolean isPrefix()
        {
            return false;
        }


        public Object visit( ExprNode node )
        {
            if ( node instanceof BranchNode )
            {
                BranchNode bnode = ( BranchNode ) node;

                // --------------------------------------------------------------------
                // we want to check each child leaf node to see if it must be expanded
                // children that are branch nodes are recursively visited
                // --------------------------------------------------------------------

                final List<ExprNode> children = bnode.getChildren();

                for ( ExprNode child : children )
                {
                    visit( child );
                }
            }
            else
            {
                if ( node instanceof SimpleNode )
                {
                    SimpleNode sn = ( SimpleNode ) node;
                    String val = sn.getValue().getString();
                    if ( val.startsWith( "$" ) )
                    {
                        Attribute at = entry.get( val.substring( 1 ) );
                        if ( at != null )
                        {
                            try
                            {
                                Value<?> newVal = null;
                                if ( !at.isHumanReadable() )
                                {
                                    newVal = new BinaryValue( at.getAttributeType(), at.getBytes() );
                                }
                                else
                                {
                                    newVal = new org.apache.directory.api.ldap.model.entry.StringValue(
                                        at.getAttributeType(), at.getString() );
                                }

                                sn.setValue( newVal );
                            }
                            catch ( LdapException e )
                            {
                                LOG.warn( "Failed to set the value for the attribute {} in the filter", at );
                            }
                        }
                    }
                }
            }

            return null;
        }

    }
}
