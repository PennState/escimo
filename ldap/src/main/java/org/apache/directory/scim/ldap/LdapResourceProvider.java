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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.api.ldap.codec.standalone.StandaloneLdapApiService;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.EntryChange;
import org.apache.directory.api.ldap.model.message.controls.PersistentSearch;
import org.apache.directory.api.ldap.model.message.controls.PersistentSearchImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.LdapSyntax;
import org.apache.directory.api.ldap.model.schema.SyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.IntegerSyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.JavaByteSyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.JavaIntegerSyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.JavaLongSyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.JavaShortSyntaxChecker;
import org.apache.directory.api.util.Base64;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.scim.ComplexAttribute;
import org.apache.directory.scim.MultiValAttribute;
import org.apache.directory.scim.ResourceNotFoundException;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.SimpleAttributeGroup;
import org.apache.directory.scim.User;
import org.apache.directory.scim.ldap.schema.BaseType;
import org.apache.directory.scim.ldap.schema.ComplexType;
import org.apache.directory.scim.ldap.schema.MultiValType;
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

        try
        {
            return toUser( entry );
        }
        catch ( Exception e )
        {
            throw new ResourceNotFoundException( e );
        }
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

                if ( !st.isShow() )
                {
                    continue;
                }

                SimpleAttribute at = getValueInto( st, entry );
                if ( at != null )
                {
                    user.addAttribute( bt.getUri(), at );
                }
            }
            else if ( bt instanceof ComplexType )
            {
                ComplexType ct = ( ComplexType ) bt;

                if ( !ct.isShow() )
                {
                    continue;
                }

                List<SimpleAttribute> lstAts = getValuesInto( ct.getAtGroup(), entry );

                if ( !lstAts.isEmpty() )
                {
                    ComplexAttribute cAt = new ComplexAttribute( ct.getName(), lstAts );
                    user.addAttribute( bt.getUri(), cAt );
                }
            }
            else if ( bt instanceof MultiValType )
            {
                MultiValType mt = ( MultiValType ) bt;

                if ( !mt.isShow() )
                {
                    continue;
                }

                List<TypedType> typedList = mt.getTypedList();
                SimpleTypeGroup stg = mt.getStGroup();

                if ( typedList != null )
                {
                    MultiValAttribute mv = new MultiValAttribute( mt.getName() );

                    for ( TypedType tt : typedList )
                    {
                        SimpleTypeGroup typeStg = tt.getAtGroup();
                        List<SimpleAttribute> lstAts = getValuesInto( typeStg, entry );
                        lstAts.add( new SimpleAttribute( "type", tt.getName() ) );
                        if ( tt.isPrimary() )
                        {
                            lstAts.add( new SimpleAttribute( "primary", true ) );
                        }

                        if ( !lstAts.isEmpty() )
                        {
                            mv.addAtGroup( new SimpleAttributeGroup( lstAts ) );
                        }
                    }

                    if ( mv.getAtGroupList() != null )
                    {
                        user.addAttribute( bt.getUri(), mv );
                    }
                }
                else if ( stg != null )
                {
                    List<SimpleAttributeGroup> atGroupList = null;

                    if ( !Strings.isEmpty( mt.getFilter() ) )
                    {
                        atGroupList = getDynamicMultiValAtFrom( stg, mt.getFilter(), mt.getBaseDn(), entry );
                    }
                    else
                    {
                        atGroupList = getValuesFor( stg, entry );
                    }

                    MultiValAttribute mv = new MultiValAttribute( mt.getName(), atGroupList );

                }
            }
        }

        return user;
    }


    private List<SimpleAttributeGroup> getDynamicMultiValAtFrom( SimpleTypeGroup stg, String filter, String baseDn,
        Entry entry )
        throws LdapException, IOException, CursorException
    {
        if ( Strings.isEmpty( baseDn ) )
        {
            baseDn = ""; // RootDSE
        }

        /* if( filter.contains( "$" ))
         {
             StringBuilder sb = new StringBuilder();
             int len = filter.length();
             int pos = 0;
             while( pos < len-1 )
             {
                 int equalPos = filter.indexOf( "=", pos );
                 sb.append( filter.subSequence( pos, equalPos+1 ) );
                 
                 int dollarPos = filter.indexOf( "$", equalPos );
                 if( dollarPos != -1 )
                 {
                     int rightParenPos = filter.indexOf( ")", dollarPos );
                     while( filter.charAt( rightParenPos - 1 ) == '\\' )
                     {
                         rightParenPos = filter.indexOf( ")", rightParenPos );
                     }
                     
                 }
                 
             }
         }*/

        List<SimpleAttributeGroup> lst = new ArrayList<SimpleAttributeGroup>();

        EntryCursor cursor = connection.search( baseDn, filter, SearchScope.SUBTREE,
            SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        while ( cursor.next() )
        {
            Entry mvEntry = cursor.get();
            List<SimpleAttributeGroup> tmp = getValuesFor( stg, mvEntry );
            if ( tmp != null )
            {
                lst.add( tmp.get( 0 ) );
            }
        }

        cursor.close();

        if ( lst.isEmpty() )
        {
            return null;
        }

        return lst;
    }


    private List<SimpleAttributeGroup> getValuesFor( SimpleTypeGroup stg, Entry entry ) throws LdapException
    {
        if ( stg == null )
        {
            return null;
        }

        SimpleType valType = stg.getValueType();

        if ( valType == null )
        {
            return null;
        }

        List<SimpleType> types = new ArrayList<SimpleType>( stg.getLstSTypes() );
        types.remove( valType );

        Attribute ldapAt = entry.get( valType.getMappedTo() );

        if ( ldapAt == null )
        {
            return null;
        }

        List<SimpleAttributeGroup> lst = new ArrayList<SimpleAttributeGroup>();

        Iterator<Value<?>> itr = ldapAt.iterator();

        while ( itr.hasNext() )
        {
            Value<?> val = itr.next();

            Object scimVal = getScimValFrom( val );
            SimpleAttributeGroup sg = new SimpleAttributeGroup();
            sg.addAttribute( new SimpleAttribute( valType.getName(), scimVal ) );

            for ( SimpleType type : types )
            {
                SimpleAttribute st = getValueInto( type, entry );

                if ( st != null )
                {
                    sg.addAttribute( st );
                }
            }

            lst.add( sg );
        }

        if ( lst.isEmpty() )
        {
            return null;
        }

        return lst;
    }


    public SimpleAttribute getValueInto( SimpleType st, Entry entry ) throws LdapException
    {
        String name = st.getName();
        Attribute at = entry.get( st.getMappedTo() );
        if ( at != null )
        {

            Object value = getScimValFrom( at );
            return new SimpleAttribute( name, value );
        }

        return null;
    }


    private Object getScimValFrom( Attribute at ) throws LdapException
    {
        return getScimValFrom( at.get() );
    }


    private Object getScimValFrom( Value<?> ldapValue ) throws LdapException
    {
        if ( !ldapValue.isHumanReadable() )
        {
            return new String( Base64.encode( ldapValue.getBytes() ) );
        }

        LdapSyntax syntax = ldapValue.getAttributeType().getSyntax();
        if ( syntax != null )
        {
            SyntaxChecker sc = syntax.getSyntaxChecker();
            if ( sc instanceof IntegerSyntaxChecker ||
                sc instanceof JavaByteSyntaxChecker ||
                sc instanceof JavaIntegerSyntaxChecker ||
                sc instanceof JavaLongSyntaxChecker ||
                sc instanceof JavaShortSyntaxChecker )
            {
                return Long.parseLong( ldapValue.getString() );
            }
        }

        return ldapValue.getString();

    }


    //    public List<SimpleAttribute> getValuesInto( List<SimpleType> lstTyps, Entry entry ) throws LdapException
    //    {
    //    }

    public List<SimpleAttribute> getValuesInto( SimpleTypeGroup stg, Entry entry ) throws LdapException
    {
        List<SimpleAttribute> lstAts = new ArrayList<SimpleAttribute>();

        for ( SimpleType st : stg.getLstSTypes() )
        {
            SimpleAttribute at = getValueInto( st, entry );
            if ( at != null )
            {
                lstAts.add( at );
            }

            //TODO handle the format
        }

        return lstAts;

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
