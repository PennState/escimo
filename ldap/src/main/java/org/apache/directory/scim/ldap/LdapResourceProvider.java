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
package org.apache.directory.scim.ldap;


import static org.apache.directory.api.ldap.model.constants.SchemaConstants.ALL_ATTRIBUTES_ARRAY;
import static org.apache.directory.api.ldap.model.message.SearchScope.SUBTREE;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.api.ldap.codec.standalone.StandaloneLdapApiService;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.EntryChange;
import org.apache.directory.api.ldap.model.message.controls.PersistentSearch;
import org.apache.directory.api.ldap.model.message.controls.PersistentSearchImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Base64;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.scim.ResourceNotFoundException;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.User;
import org.apache.directory.scim.ldap.schema.BaseType;
import org.apache.directory.scim.ldap.schema.ComplexType;
import org.apache.directory.scim.ldap.schema.SimpleType;
import org.apache.directory.scim.ldap.schema.SimpleTypeGroup;
import org.apache.directory.scim.ldap.schema.TypedType;
import org.apache.directory.scim.ldap.schema.UserSchema;


/**
 * TODO LdapResourceProvider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapResourceProvider
{
    private LdapConnection connection;

    private LdapSchemaMapper schema;

    private UserSchema userSchema;


    public LdapResourceProvider( LdapConnection connection )
    {
        schema = new LdapSchemaMapper();
        schema.loadMappings();
        userSchema = schema.getUserSchema();
    }


    public User getUser( String id ) throws ResourceNotFoundException
    {
        SimpleType st = ( SimpleType ) userSchema.getCoreAttribute( "id" );
        String userIdName = st.getMappedTo();

        String filter = "(" + userIdName + "=" + id + ")";

        Entry entry = null;

        try
        {
            EntryCursor cursor = connection.search( userSchema.getBaseDn(), filter, SUBTREE, ALL_ATTRIBUTES_ARRAY );

            if ( cursor.next() )
            {
                entry = cursor.get();
            }

            cursor.close();

        }
        catch ( Exception e )
        {
            throw new ResourceNotFoundException( e );
        }

        if ( entry == null )
        {
            throw new ResourceNotFoundException( "No User resource found with the ID " + id );
        }

        return toUser( entry );
    }


    public User toUser( Entry entry ) throws Exception
    {
        Collection<BaseType> coreTypes = userSchema.getCoreAttributes();

        User user = new User();

        for ( BaseType bt : coreTypes )
        {
            if ( bt instanceof SimpleType )
            {
                SimpleType st = ( SimpleType ) bt;
                
                if(!st.isShow()) 
                {
                    continue;
                }
                
                String name = st.getName();
                Attribute at = entry.get( st.getMappedTo() );
                if ( at != null )
                {
                    String value = null;
                    if( at.isHumanReadable() )
                    {
                        value = at.getString();
                    }
                    else
                    {
                        value = new String( Base64.encode( at.getBytes() ) );
                    }
                    
                    user.addAttribute( bt.getUri(), new SimpleAttribute( name, value ) );
                }
            }
            else if ( bt instanceof ComplexType )
            {
                ComplexType ct = ( ComplexType ) bt;
                
                if(!ct.isShow()) 
                {
                    continue;
                }
                
                SimpleTypeGroup stg = ct.getStGroup();
                List<SimpleType> lstTyps = stg.getLstSTypes();
                
            }
        }

        Iterator<Attribute> itr = entry.iterator();
        return null;
    }


    public static void main( String[] args ) throws Exception
    {
        System.setProperty( StandaloneLdapApiService.DEFAULT_CONTROLS_LIST,
            "org.apache.directory.api.ldap.codec.controls.cascade.CascadeFactory," +
                "org.apache.directory.api.ldap.codec.controls.manageDsaIT.ManageDsaITFactory," +
                "org.apache.directory.api.ldap.codec.controls.search.entryChange.EntryChangeFactory," +
                "org.apache.directory.api.ldap.codec.controls.search.pagedSearch.PagedResultsFactory," +
                "org.apache.directory.api.ldap.codec.controls.search.persistentSearch.PersistentSearchFactory," +
                "org.apache.directory.api.ldap.codec.controls.search.subentries.SubentriesFactory" );

        LdapNetworkConnection c = new LdapNetworkConnection( "localhost", 10389 );
        c.setTimeOut( Long.MAX_VALUE );
        c.loadSchema();
        c.bind( "uid=admin,ou=system", "secret" );

        PersistentSearch ps = new PersistentSearchImpl();
        ps.setChangesOnly( false );
        ps.setReturnECs( true );

        SearchRequest searchRequest = new SearchRequestImpl().setBase( new Dn(
            "ou=system" ) ).setFilter( "(objectclass=*)" ).setScope(
            SearchScope.SUBTREE ).addControl( ps );
        searchRequest.addAttributes( "uid" );

        SearchCursor cursor = c.search( searchRequest );

        while ( cursor.next() )
        {
            Response response = cursor.get();
            SearchResultEntry se = ( SearchResultEntry ) response;
            System.out.println( se.getEntry() );
            System.out.println( se.getControl( EntryChange.OID ) );
        }
        cursor.close();
    }
}
