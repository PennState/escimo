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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.util.Strings;
import org.apache.directory.scim.schema.BaseType;


/**
 * 
 * TODO ResourceSchema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ResourceSchema
{
    private String baseDn;
    private String filter;

    private SimpleType rdnType;

    private String reqUri;

    private String name;

    private List<String> schemaIds = new ArrayList<String>();

    private Map<String, BaseType> coreTypes = new LinkedHashMap<String, BaseType>();
    private Map<String, BaseType> extendedTypes = new LinkedHashMap<String, BaseType>();

    private List<String> objectClasses = new ArrayList<String>();


    public ResourceSchema( String baseDn, String filter )
    {
        if ( Strings.isEmpty( baseDn ) )
        {
            baseDn = ""; // RootDSE
        }

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


    public BaseType getAttribute( String name )
    {
        if ( name == null )
        {
            return null;
        }

        name = name.trim();

        int colonPos = name.lastIndexOf( ":" );
        if ( colonPos > 0 )
        {
            name = name.substring( colonPos + 1 );
        }

        if ( name.contains( "." ) )
        {
            String[] atPath = name.split( "\\." );

            BaseType b = _findAtType( atPath[0] );

            SimpleTypeGroup stg = null;

            if ( b instanceof ComplexType )
            {
                ComplexType c = ( ComplexType ) b;
                stg = c.getAtGroup();
            }
            else if ( b instanceof MultiValType )
            {
                MultiValType m = ( MultiValType ) b;
                stg = m.getAtGroup();
            }

            if ( stg != null )
            {
                return stg.getType( atPath[1] );
            }

            return null;
        }

        return _findAtType( name );
    }


    private BaseType _findAtType( String name )
    {
        BaseType bt = coreTypes.get( name );

        if ( bt == null )
        {
            bt = extendedTypes.get( name );
        }

        return bt;
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


    public Collection<BaseType> getCoreTypes()
    {
        return Collections.unmodifiableCollection( coreTypes.values() );
    }


    public Collection<BaseType> getExtendedTypes()
    {
        return Collections.unmodifiableCollection( extendedTypes.values() );
    }


    public void addUri( String uri )
    {
        schemaIds.add( uri );
    }


    public void addObjectClass( String oc )
    {
        objectClasses.add( oc );
    }


    /**
     * @return the objectClasses
     */
    public List<String> getObjectClasses()
    {
        return Collections.unmodifiableList( objectClasses );
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


    public SimpleType getRdnType()
    {
        return rdnType;
    }


    public void setRdnType( SimpleType rdnType )
    {
        this.rdnType = rdnType;
    }


    public String getReqUri()
    {
        return reqUri;
    }


    public String getName()
    {
        return name;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public void setReqUri( String reqUri )
    {
        this.reqUri = reqUri;
    }


    public List<String> getSchemaIds()
    {
        return new ArrayList<String>( schemaIds );
    }
}
