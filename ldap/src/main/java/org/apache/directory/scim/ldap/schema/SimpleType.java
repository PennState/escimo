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


/**
 * TODO SimpleType.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SimpleType extends BaseType
{
    private String name;

    private String mappedTo;

    private boolean show = true;


    public SimpleType( String name, String mappedTo, String uri, boolean show )
    {
        super(uri);
        this.name = name;
        this.mappedTo = mappedTo;
        this.show = show;
    }


    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }


    /**
     * @return the mappedTo
     */
    public String getMappedTo()
    {
        return mappedTo;
    }


    /**
     * @return the show
     */
    public boolean isShow()
    {
        return show;
    }


    @Override
    public boolean isSimple()
    {
        return true;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "SimpleType [name=" + name + ", mappedTo=" + mappedTo + ", show=" + show + "]";
    }

}
