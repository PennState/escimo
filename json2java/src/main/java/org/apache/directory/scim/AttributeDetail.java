/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.scim;


/**
 * 
 * A simple pojo internally used while gerating the source code.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AttributeDetail
{
    private String name;
    private String javaType;
    private boolean multiValued = false;
    private boolean readOnly = false;


    public AttributeDetail( String name, String javaType )
    {
        if ( name == null || javaType == null )
        {
            throw new IllegalArgumentException( "Null value cannot be accepted" );
        }

        this.name = name;
        this.javaType = javaType;
    }


    public String getName()
    {
        return name;
    }

    public String getMethodName()
    {
        return Character.toUpperCase( name.charAt( 0 ) ) + name.substring( 1 );
    }

    public String getJavaType()
    {
        return javaType;
    }


    public boolean isMultiValued()
    {
        return multiValued;
    }


    public void setMultiValued( boolean isMultiValued )
    {
        this.multiValued = isMultiValued;
    }

    
    public boolean isBoolean()
    {
        return "boolean".equalsIgnoreCase( javaType );
    }
    

    public int compareTo( Object o )
    {
        AttributeDetail that = ( AttributeDetail ) o;

        return getName().compareTo( that.getName() );
    }


    @Override
    public boolean equals( Object obj )
    {
        return getName().equals( ( ( AttributeDetail ) obj ).getName() );
    }


    /**
     * @return the readOnly
     */
    public boolean isReadOnly()
    {
        return readOnly;
    }


    /**
     * @param readOnly the readOnly to set
     */
    public void setReadOnly( boolean readOnly )
    {
        this.readOnly = readOnly;
    }


    @Override
    public int hashCode()
    {
        return name.hashCode();
    }


    @Override
    public String toString()
    {
        return name + "=" + name;
    }

}
