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
import java.util.List;

import org.apache.directory.scim.User.Email;
import org.apache.directory.scim.User.Name;
import org.junit.Test;

/**
 * TODO UserResourceTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UserResourceTest
{
    private EscimoClient client = new EscimoClient( "http://localhost:8080/v2" );
    
    @Test
    public void testAddUser() throws Exception
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
    }
}
