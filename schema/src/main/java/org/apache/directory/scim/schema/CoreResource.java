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

import java.util.ArrayList;
import java.util.List;

/**
 * TODO CoreResource.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class CoreResource
{
    private List<CoreResource> extResources;

    private MetaData meta;
    
    private String schemaId;
    
    private String resourceDesc;
    
    public void addExtendedResource( CoreResource resource )
    {
        if( extResources == null )
        {
            extResources = new ArrayList<CoreResource>();
        }

        extResources.add( resource );
    }

    /**
     * @return the extResources
     */
    public List<CoreResource> getExtResources()
    {
        return extResources;
    }

    /**
     * @param extResources the extResources to set
     */
    public void setExtResources( List<CoreResource> extResources )
    {
        this.extResources = extResources;
    }

    /**
     * @return the meta
     */
    public MetaData getMeta()
    {
        return meta;
    }

    /**
     * @param meta the meta to set
     */
    public void setMeta( MetaData meta )
    {
        this.meta = meta;
    }

    /**
     * @return the schemaId
     */
    public String getSchemaId()
    {
        return schemaId;
    }

    /**
     * @param schemaId the schemaId to set
     */
    public void setSchemaId( String schemaId )
    {
        this.schemaId = schemaId;
    }

    /**
     * @return the resourceDesc
     */
    public String getResourceDesc()
    {
        return resourceDesc;
    }

    /**
     * @param resourceDesc the resourceDesc to set
     */
    public void setResourceDesc( String resourceDesc )
    {
        this.resourceDesc = resourceDesc;
    }
    
}
