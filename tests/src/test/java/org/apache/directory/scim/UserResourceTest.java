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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.scim.User.Email;
import org.apache.directory.scim.User.Name;
import org.apache.directory.scim.schema.CoreResource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TODO UserResourceTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
//@ApplyLdifs(
//    {
//        "dn: uid=user1,ou=users,ou=system",
//        "objectClass: inetOrgPerson",
//        "sn: user1 sn",
//        "cn: user One",
//        "uid: user1",
//
//        "dn: uid=user2,ou=users,ou=system",
//        "objectClass: inetOrgPerson",
//        "sn: User Two",
//        "cn: user2",
//        "uid: user2",
//
//        "dn: uid=elecharny,ou=users,ou=system",
//        "objectClass: inetOrgPerson",
//        "sn:: RW1tYW51ZWwgTMOpY2hhcm55",
//        "cn: elecharny",
//        "uid: elecharny"
//})
public class UserResourceTest
{
    private static EscimoClient client;
    
    @BeforeClass
    public static void startJetty() throws Exception
    {
        Map<String,Class<? extends CoreResource>> uriClassMap = new HashMap<String, Class<? extends CoreResource>>();
        uriClassMap.put( User.SCHEMA_ID, User.class );
        uriClassMap.put( Group.SCHEMA_ID, Group.class );
        uriClassMap.put( EnterpriseUser.SCHEMA_ID, EnterpriseUser.class );
        
        client = new EscimoClient( "http://localhost:8080/v2", uriClassMap );
        
        JettyServer.start();
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
        
        User addedUser = ( User ) client.addUser( user );
        
        assertNotNull( addedUser );
        
        assertEquals( user.getUserName(), addedUser.getUserName() );
        
        User fetchedUser = ( User ) client.getUser( addedUser.getId() );
        
        assertEquals( addedUser.getUserName(), fetchedUser.getUserName() );
        assertEquals( addedUser.getId(), fetchedUser.getId() );
        
//        client.
    }
}
