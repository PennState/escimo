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
public class TypedType extends BaseType
{
    private String name;
    
    private SimpleTypeGroup atGroup;
    
    private boolean show = true;
    
    private boolean primary = false;
    

    public TypedType( String name, SimpleTypeGroup atGroup, boolean show, boolean primary, String uri )
    {
        super(uri);
        this.name = name;
        this.atGroup = atGroup;
        this.show = show;
        this.primary = primary;
    }
    
    /**
     * @return the atGroup
     */
    public SimpleTypeGroup getAtGroup()
    {
        return atGroup;
    }

    /**
     * @return the show
     */
    public boolean isShow()
    {
        return show;
    }

    /**
     * @return the primary
     */
    public boolean isPrimary()
    {
        return primary;
    }

    @Override
    public boolean isTyped()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "TypedType [type=" + name + ", atGroup=" + atGroup + ", show=" + show + ", primary="
            + primary + "]";
    }
    
    
}
