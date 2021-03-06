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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO ServerResource.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ServerResource
{
    private Map<String,List<AbstractAttribute>> uriAtMap = new HashMap<String, List<AbstractAttribute>>();
    
    private String id;
    
    public void addAttribute( String uri, AbstractAttribute at )
    {
        List<AbstractAttribute> atList = uriAtMap.get( uri );
        
        if( atList == null )
        {
            atList = new ArrayList<AbstractAttribute>();
            uriAtMap.put( uri, atList );
        }
        
        atList.add( at );
    }

    public Map<String,List<AbstractAttribute>> getAttributes()
    {
        return uriAtMap;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }
    
    public AbstractAttribute get( String name )
    {
        for( List<AbstractAttribute> atList : uriAtMap.values() )
        {
            for( AbstractAttribute at : atList )
            {
                if( at.getName().equals( name ) )
                {
                    return at;
                }
            }
        }
        
        return null;
    }
    
    
    public Object getVal( String name )
    {
        AbstractAttribute at = get( name );

        if( at instanceof SimpleAttribute )
        {
            return ( ( SimpleAttribute ) at ).getValue();
        }
        
        return null;
    }
}
