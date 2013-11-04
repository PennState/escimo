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
package org.apache.directory.scim.search;


import org.apache.directory.scim.util.ResourceUtil;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 * TODO ResourceUtilTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ResourceUtilTest
{
    @Test
    public void testScimDateToLdapDate()
    {
        String scimDate = "2012-03-02T16:41:34Z";

        String ldapDate = ResourceUtil.toLdapDate( scimDate );
        assertEquals( "20120302164134Z", ldapDate );
    }


    @Test
    public void testLdapDateToScimDate()
    {
        String ldapDate = "20120302164134Z";

        String scimDate = ResourceUtil.toScimDate( ldapDate );
        assertEquals( "2012-03-02T16:41:34Z", scimDate );
    }

}
