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
package org.apache.directory.scim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.scim.User.Email;
import org.apache.directory.scim.User.Name;
import org.apache.directory.scim.schema.CoreResource;
import org.apache.directory.scim.schema.ErrorCode;
import org.apache.directory.scim.schema.MetaData;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.LdapCoreSessionConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TODO UserResourceTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UserResourceTest
{
    private static String baseUrl = "http://localhost:8080/v2";
    
    private static EscimoClient client;
    
    @BeforeClass
    public static void startJetty() throws Exception
    {
        Map<String,Class<? extends CoreResource>> uriClassMap = new HashMap<String, Class<? extends CoreResource>>();
        uriClassMap.put( User.SCHEMA_ID, User.class );
        uriClassMap.put( Group.SCHEMA_ID, Group.class );
        uriClassMap.put( EnterpriseUser.SCHEMA_ID, EnterpriseUser.class );
        
        client = new EscimoClient( baseUrl, uriClassMap );
        
        JettyServer.start();
        
        EscimoResult er = client.authenticate( "admin", "secret" );
        System.out.println( er );
    }
    
    @AfterClass
    public static void stopJetty() throws Exception
    {
        JettyServer.stop();
    }
    
    @Test
    public void testAddGetAndDeleteUser() throws Exception
    {
        User user = new User();
        user.setUserName( "test1" );
        user.setDisplayName( "Test UserResource" );
        user.setPassword( "secret01" );
        
        Name name = new Name();
        name.setFamilyName( "UserResource" );
        name.setGivenName( "Test" );
        
        user.setName( name );
        
        List<Email> emails = new ArrayList<Email>();
        Email mail = new Email();
        mail.setValue( "test@example.com" );
        emails.add( mail );
        user.setEmails( emails );
        
        EscimoResult result = client.addUser( user );
        assertTrue( result.isSuccess() );
        
        User addedUser = ( User ) result.getResource(); 
        
        assertNotNull( addedUser );
        
        assertEquals( user.getUserName(), addedUser.getUserName() );
        
        result = client.getUser( addedUser.getId() );
        User fetchedUser = result.getResourceAs( User.class );
        
        assertEquals( addedUser.getUserName(), fetchedUser.getUserName() );
        assertEquals( addedUser.getId(), fetchedUser.getId() );
        
        result = client.addUser( user );
        assertFalse( result.isSuccess() );
        assertEquals( ErrorCode.CONFLICT.getVal(), result.getErrorResponse().getFirstErrorCode() );
        
        result = client.deleteUser( fetchedUser.getId() );
        assertTrue( result.isSuccess() );
        
        result = client.getUser( addedUser.getId() );
        fetchedUser = result.getResourceAs( User.class );
        assertNull( fetchedUser );
    }
    
    @Test
    public void testPut() throws Exception
    {
        User user = new User();
        user.setUserName( "test2" );
        user.setDisplayName( "Test UserResource" );
        user.setPassword( "secret01" );
        
        Name name = new Name();
        name.setFamilyName( "UserResource" );
        name.setGivenName( "Test" );
        
        user.setName( name );
        
        List<Email> emails = new ArrayList<Email>();
        Email mail = new Email();
        mail.setValue( "test@example.com" );
        emails.add( mail );
        user.setEmails( emails );

        EscimoResult result = client.addUser( user );
        assertTrue( result.isSuccess() );
        
        User addedUser = ( User ) result.getResource(); 

        assertNotNull( addedUser );

        addedUser.getEmails().clear();
        
        Email newEmail = new Email();
        newEmail.setValue( "newemail@example.com" );
        addedUser.getEmails().add( newEmail );
        
        result = client.putUser( addedUser.getId(), addedUser );
        User replacedUser = result.getResourceAs( User.class );

        assertNotNull( replacedUser );
        assertEquals( 1, replacedUser.getEmails().size() );
        assertEquals( newEmail.getValue(), replacedUser.getEmails().get( 0 ).getValue() );
    }
    
    
    @Test
    public void testPatch() throws Exception
    {
        User user = new User();
        user.setUserName( "testPatch" );
        user.setDisplayName( "Test UserResource" );
        user.setPassword( "secret01" );
        
        Name name = new Name();
        name.setFamilyName( "UserResource" );
        name.setGivenName( "Test" );
        
        user.setName( name );
        
        List<Email> emails = new ArrayList<Email>();
        Email mail = new Email();
        mail.setValue( "test@example.com" );
        emails.add( mail );
        user.setEmails( emails );

        EscimoResult result = client.addUser( user );
        assertTrue( result.isSuccess() );
        
        User addedUser = ( User ) result.getResource(); 

        assertNotNull( addedUser );

        addedUser.getEmails().get( 0 ).setOperation( "delete" );
        
        List<String> newEmails = new ArrayList<String>();
        newEmails.add( "newemail@example.com" );
        newEmails.add( "anothermail@example.com" );
        for( String e : newEmails )
        {
            Email newEmail = new Email();
            newEmail.setValue( e );
            addedUser.getEmails().add( newEmail );
        }
        
        result = client.patchUser( addedUser.getId(), addedUser );
        User patchedUser = result.getResourceAs( User.class );

        assertNull( patchedUser );
        
        result = client.getUser( addedUser.getId() );
        patchedUser = result.getResourceAs( User.class ); 
        assertNotNull( patchedUser );
        
        for( Email e : patchedUser.getEmails() )
        {
            System.out.println(e.getValue());
            assertTrue( newEmails.contains( e.getValue() ) );
        }
        
        assertEquals( 2, patchedUser.getEmails().size() );
    }


    @Test
    public void testCreateGroups() throws Exception
    {
        Group group = new Group();
        group.setDisplayName( "Administrator" );
        
        List<Group.Member> members = new ArrayList<Group.Member>();
        group.setMembers( members );
        
        CoreSession session = JettyServer.getAdminSession();
        LdapCoreSessionConnection conn = new LdapCoreSessionConnection( session );
        
        EntryCursor cursor = conn.search( "ou=system", "(objectClass=*)", SearchScope.SUBTREE, SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        
        int count = 0;
        while( cursor.next() )
        {
            Entry entry = cursor.get();
            
            Group.Member m1 = new Group.Member();
            
            String value = entry.get( SchemaConstants.ENTRY_UUID_AT ).getString();
            
            m1.setValue( value );
            m1.set$ref( baseUrl + "/Users/" + value );
            
            members.add( m1 );
            count++;
        }
        
        EscimoResult result = client.addGroup( group );
        assertTrue( result.isSuccess() );
        
        Group addedGroup = result.getResourceAs( Group.class );
        
        assertNotNull( addedGroup );
        assertEquals( group.getDisplayName(), addedGroup.getDisplayName() );
        assertNotNull( addedGroup.getId() );
        assertEquals( count, addedGroup.getMembers().size() );
        
        Group.Member deletedMember = members.get( 0 );
        
        Group tobePatchedGroup = new Group();
        
        List<Group.Member> patchedMembers = new ArrayList<Group.Member>();
        tobePatchedGroup.setMembers( patchedMembers );
        
        deletedMember.setOperation( "delete" );
        patchedMembers.add( deletedMember );
        
        client.patchGroup( addedGroup.getId(), tobePatchedGroup );
        
        result = client.getGroup( addedGroup.getId() );
        Group patchedGroup = result.getResourceAs( Group.class );

        assertEquals( ( count - 1 ), patchedGroup.getMembers().size() );
        
        for( Group.Member gm : patchedGroup.getMembers() )
        {
            if( gm.getValue().equals( deletedMember.getValue() ) )
            {
                fail( "This member shouldn't present" );
            }
        }
        
        Group deleteMemGroup = new Group();
        MetaData meta = new MetaData();
        List<String> attributes = new ArrayList<String>();
        attributes.add( "members" );
        meta.setAttributes( attributes );
        deleteMemGroup.setMeta( meta );
        
        result = client.patchGroup( addedGroup.getId(), deleteMemGroup );
        patchedGroup = result.getResourceAs( Group.class );
        assertTrue( result.isSuccess() );
        assertNull( patchedGroup );
        //TODO uncomment the below line as soon as client supports HTTP parameter passing
        //assertNull( patchedGroup.getMembers() );
    }
}
