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
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.filter.FilterVisitor;
import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.scim.MultiValAttribute;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.SimpleAttributeGroup;
import org.apache.directory.scim.ldap.LdapResourceProvider;
import org.apache.directory.scim.ldap.schema.ResourceSchema;
import org.apache.directory.scim.schema.BaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * TODO MembersAttributeHandler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MembersAttributeHandler extends LdapAttributeHandler
{

    private static final Logger LOG = LoggerFactory.getLogger( MembersAttributeHandler.class );


    @Override
    public void read( BaseType bt, Object srcResource, RequestContext ctx ) throws Exception
    {
        checkHandler( bt, "members", this );
        
        Entry groupEntry = ( Entry ) srcResource;

        Attribute memberAt = groupEntry.get( SchemaConstants.UNIQUE_MEMBER_AT );
        if ( memberAt == null )
        {
            memberAt = groupEntry.get( SchemaConstants.MEMBER_AT );
        }

        if ( memberAt == null )
        {
            LOG.debug( "Neither member or uniqueMember attribute is present in the entry {}", groupEntry.getDn() );
            return;
        }

        List<SimpleAttributeGroup> lstAtGroup = new ArrayList<SimpleAttributeGroup>();

        Iterator<Value<?>> itr = memberAt.iterator();
        while ( itr.hasNext() )
        {
            Value<?> val = itr.next();
            SimpleAttributeGroup sg = getMemberDetails( val.getString(), ctx );
            if ( sg != null )
            {
                lstAtGroup.add( sg );
            }
        }

        if ( !lstAtGroup.isEmpty() )
        {
            MultiValAttribute mv = new MultiValAttribute( bt.getName(), lstAtGroup );
            ctx.getCoreResource().addAttribute( bt.getUri(), mv );
        }
    }


    @Override
    public void write( BaseType atType, JsonElement jsonData, Object targetEntry, RequestContext ctx ) throws Exception
    {
        checkHandler( atType, "members", this );
        
        LdapResourceProvider provider = ( LdapResourceProvider ) ctx.getProviderService();

        SchemaManager ldapSchema = provider.getLdapSchema();
        Entry entry = ( Entry ) targetEntry;
        
        AttributeType memberType = getMemberType( ldapSchema, entry );

        JsonArray members = ( JsonArray ) jsonData;
        
        for( JsonElement je : members )
        {
            JsonObject jo = ( JsonObject ) je;
            
            String dn = getMemberDn( jo, provider );
            
            if( dn == null )
            {
                continue;
            }
            
            Attribute ldapAt = entry.get( memberType );
            
            if( ldapAt == null )
            {
                ldapAt = new DefaultAttribute( memberType );
                ldapAt.add( dn );
                entry.add( ldapAt );
            }
            else
            {
                ldapAt.add( dn );
            }
        }
    }

    
    @Override
    public void patch( BaseType atType, JsonElement jsonData, Object existingEntry, RequestContext ctx, Object patchCtx )
        throws Exception
    {
        checkHandler( atType, "members", this );

        LdapResourceProvider provider = ( LdapResourceProvider ) ctx.getProviderService();

        SchemaManager ldapSchema = provider.getLdapSchema();

        Entry entry = ( Entry ) existingEntry;
        
        AttributeType memberType = getMemberType( ldapSchema, entry );
        
        ModifyRequest modReq = ( ModifyRequest ) patchCtx;
        
        JsonArray members = ( JsonArray ) jsonData;
        
        for( JsonElement je : members )
        {
            JsonObject jo = ( JsonObject ) je;
            
            String dn = getMemberDn( jo, provider );
            
            if( dn == null )
            {
                continue;
            }
        
            boolean delete = false;
            
            JsonElement atOperation = jo.get( "operation" );
            if( atOperation != null )
            {
                if( atOperation.getAsString().equalsIgnoreCase( "delete" ) )
                {
                    delete = true;
                }
            }

            if( delete )
            {
                modReq.remove( memberType.getName(), dn );
            }
            else if ( !entry.contains( memberType, dn ) )
            {
                modReq.add( memberType.getName(), dn );
            }
        }
    }

    
    @Override
    public void deleteAttribute( BaseType atType, Object targetEntry, RequestContext ctx, Object patchCtx )
        throws Exception
    {
        checkHandler( atType, "members", this );

        LdapResourceProvider provider = ( LdapResourceProvider ) ctx.getProviderService();

        SchemaManager ldapSchema = provider.getLdapSchema();

        Entry entry = ( Entry ) targetEntry;
        
        AttributeType memberType = getMemberType( ldapSchema, entry );
        
        ModifyRequest modReq = ( ModifyRequest ) patchCtx;
        
        modReq.remove( entry.get( memberType ) );
        // add a dummy member, cause groupOfNames and groupOfUniqueNames OC need atleast one member attribute
        modReq.add( memberType.getName(), "uid=dummyUser,ou=system" );
    }


    private String getMemberDn( JsonObject jo, LdapResourceProvider provider )
    {
        String resId = jo.get( "value" ).getAsString();
        String resRef = jo.get( "$ref" ).getAsString();
        
        ResourceSchema resSchema = provider.getUserSchema();
        
        if( resRef.endsWith( "/Groups/" + resId ) )
        {
            resSchema = provider.getGroupSchema();
        }
        
        Entry resEntry = provider.fetchEntryById( resId, resSchema );
        
        if( resEntry == null )
        {
            LOG.debug( "No resource found with the member ID {} with reference {}", resId, resRef );
            return null;
        }
        
        return resEntry.getDn().getName();
    }

    
    private AttributeType getMemberType( SchemaManager ldapSchema, Entry entry )
    {
        AttributeType ocType = ldapSchema.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        
        AttributeType memberType = null;
        
        if( entry.contains( ocType, SchemaConstants.GROUP_OF_NAMES_OC ) )
        {
            memberType = ldapSchema.getAttributeType( SchemaConstants.MEMBER_AT );
        }
        else if( entry.contains( ocType, SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC ) )
        {
            memberType = ldapSchema.getAttributeType( SchemaConstants.UNIQUE_MEMBER_AT );
        }
        
        return memberType;
    }
    
    private SimpleAttributeGroup getMemberDetails( String dn, RequestContext ctx ) throws Exception
    {
        LdapResourceProvider provider = ( LdapResourceProvider ) ctx.getProviderService();

        Entry memberEntry = provider.fetchEntryByDn( dn );

        if ( memberEntry == null )
        {
            return null;
        }

        SimpleAttributeGroup sg = null;
        
        try
        {
            List<SimpleAttribute> lst = new ArrayList<SimpleAttribute>();

            SimpleAttribute id = new SimpleAttribute( "value", memberEntry.get( SchemaConstants.ENTRY_UUID_AT )
                .getString() );
            lst.add( id );

            String locationVal = ctx.getUriInfo().getBaseUri().toString();
            locationVal = locationVal + "Users/" + id.getValue();

            SimpleAttribute ref = new SimpleAttribute( "$ref", locationVal );
            lst.add( ref );

            SimpleAttribute display = new SimpleAttribute( "display", memberEntry.getDn().getRdn().getValue()
                .getString() );
            lst.add( display );

            sg = new SimpleAttributeGroup( lst );
        }
        catch ( LdapException ex )
        {
            LOG.warn( "Failed to get attributes from entry {}", memberEntry.getDn() );
            throw ex;
        }

        return sg;
    }


    private List<Entry> getMemberEntriesUsingFilter( String filter, String baseDn, Entry userEntry,
        LdapResourceProvider provider ) throws Exception
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
            throw e;
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
                                throw new RuntimeException( e );
                            }
                        }
                    }
                }
            }

            return null;
        }

    }
}
