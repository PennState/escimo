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
package org.apache.directory.scim.ldap.schema;


import org.apache.directory.scim.schema.BaseType;


/**
 * TODO SimpleType.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MultiValType extends BaseType
{
    private SimpleTypeGroup atGroup;

    private String baseDn;

    private String filter;

    /** used for setting the value of "formatted" attribute */
    private String format;


    public MultiValType( String uri, String name, boolean show, SimpleTypeGroup atGroup, String baseDn, String filter )
    {
        super( uri, name, show );
        this.atGroup = atGroup;
        this.baseDn = baseDn;
        this.filter = filter;
    }


    /**
     * @return the stGroup
     */
    public SimpleTypeGroup getAtGroup()
    {
        return atGroup;
    }


    /**
     * @return the baseDn
     */
    public String getBaseDn()
    {
        return baseDn;
    }


    /**
     * @return the filter
     */
    public String getFilter()
    {
        return filter;
    }


    /**
     * @return the format
     */
    public String getFormat()
    {
        return format;
    }


    @Override
    public boolean isComplex()
    {
        return true;
    }

}
