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
package org.apache.directory.scim.schema;


import java.util.Date;
import java.util.List;


/**
 * TODO MetaData.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MetaData
{
    private String resourceType;

    private Date created;

    private Date lastModified;

    private String location;

    private String version;

    private List<String> attributes;


    /**
     * @return the resourceType
     */
    public String getResourceType()
    {
        return resourceType;
    }


    /**
     * @param resourceType the resourceType to set
     */
    public void setResourceType( String resourceType )
    {
        this.resourceType = resourceType;
    }


    /**
     * @return the created
     */
    public Date getCreated()
    {
        return created;
    }


    /**
     * @param created the created to set
     */
    public void setCreated( Date created )
    {
        this.created = created;
    }


    /**
     * @return the lastModified
     */
    public Date getLastModified()
    {
        return lastModified;
    }


    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified( Date lastModified )
    {
        this.lastModified = lastModified;
    }


    /**
     * @return the location
     */
    public String getLocation()
    {
        return location;
    }


    /**
     * @param location the location to set
     */
    public void setLocation( String location )
    {
        this.location = location;
    }


    /**
     * @return the version
     */
    public String getVersion()
    {
        return version;
    }


    /**
     * @param version the version to set
     */
    public void setVersion( String version )
    {
        this.version = version;
    }


    /**
     * @return the attributes
     */
    public List<String> getAttributes()
    {
        return attributes;
    }


    /**
     * @param attributes the attributes to set
     */
    public void setAttributes( List<String> attributes )
    {
        this.attributes = attributes;
    }

}
