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


import java.util.Collections;
import java.util.List;


/**
 * TODO SimpleTypeGroup.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SimpleTypeGroup
{
    private List<SimpleType> subTypes;

    public SimpleTypeGroup( List<SimpleType> subTypes )
    {
        this.subTypes = subTypes;
    }


    /**
     * @return the lstSTypes
     */
    public List<SimpleType> getSubTypes()
    {
        return Collections.unmodifiableList( subTypes );
    }


    public SimpleType getValueType()
    {
        return getType( "value" );
    }


    public SimpleType getType( String name )
    {
        if( subTypes == null )
        {
            return null;
        }
        
        for ( SimpleType st : subTypes )
        {
            if ( name.equals( st.getName() ) )
            {
                return st;
            }
        }

        return null;
    }


    @Override
    public String toString()
    {
        return "SimpleTypeGroup [subTypes=" + subTypes + "]";
    }

}
