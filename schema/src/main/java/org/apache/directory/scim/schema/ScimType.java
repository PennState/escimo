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

/**
 * TODO ScimType.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum ScimType
{
    INVALID_FILTER("invalidFilter", "The specified filter syntax was invalid (does not comply with Figure 1) or the "
                                                + "specified attribute and filter comparison combination is not supported."),

    TOO_MANY("tooMany", "The specified filter yields many more results than the server is willing calculate or process."
                                    + " For example, a filter such as \"(userName pr)\" by itself would return all entries with a "
                                    + "\"userName\" and MAY not be acceptable to the service provider."),

    UNIQUENESS("uniqueness", "One or more of attribute values is already in use or is reserved."),

    MUTABILITY("mutability", "The attempted modification is not compatible with the target attributes mutability or current"
                                         + " state (e.g. modification of an immutable attribute with an existing value)."),

    INVALID_SYNTAX("invalidSyntax", "The request body message structure was invalid or did not conform to the request schema."),

    INVALID_PATH("invalidPath", "The path attribute was invalid or malformed (see Figure 7)."),

    NOTARGET("noTarget", "The specified \"path\" did not yield an attribute or attribute value that could be operated on. "
                                     + "This occurs when the specified \"path\" value contains a filter that yields no match."),

    INVALID_VALUE("invalidValue", "A required value was missing, or the value specified was not compatible with the operation"
                                              + " or attribute type."),

    INVALID_VERSION("invalidVers", "The specified SCIM protocol version is not supported");

    private String val;
    
    private String desc;

    private ScimType( String val, String desc )
    {
        this.val = val;
        this.desc = desc;
    }

    
    public static ScimType getByVal( String v )
    {
        for( ScimType sc : values() )
        {
            if ( sc.val.equalsIgnoreCase( v ) )
            {
                return sc;
            }
        }

        return null;
    }
    
    /**
     * @return the value
     */
    public String getVal()
    {
        return val;
    }


    @Override
    public String toString()
    {
        return val;
    }

}
