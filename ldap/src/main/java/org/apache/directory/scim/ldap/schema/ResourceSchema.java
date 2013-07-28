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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 
 * TODO ResourceSchema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class ResourceSchema
{
    private String baseDn;
    private String filter;

    private List<String> uris = new ArrayList<String>();

    private Map<String, BaseType> coreTypes = new HashMap<String, BaseType>();
    private Map<String, BaseType> extendedTypes = new HashMap<String, BaseType>();


    public ResourceSchema( String baseDn, String filter )
    {
        this.baseDn = baseDn;
        this.filter = filter;
    }


    public BaseType getCoreAttribute( String name )
    {
        return coreTypes.get( name );
    }


    public BaseType getExtAttribute( String name )
    {
        return extendedTypes.get( name );
    }


    public void addAttributeType( String name, BaseType type )
    {
        if ( type != null )
        {
            if ( type.isCoreAttribute() )
            {
                coreTypes.put( name, type );
            }
            else
            {
                extendedTypes.put( name, type );
            }
        }
    }


    public Collection<BaseType> getCoreAttributes()
    {
        return coreTypes.values();
    }


    public void addUri( String uri )
    {
        uris.add( uri );
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

}