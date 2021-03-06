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
package org.apache.directory.scim.search;

/**
 * TODO TerminalNode.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TerminalNode extends FilterNode
{
    private String attribute;
    
    private String value;
    
    public TerminalNode( Operator operator )
    {
        super( operator );
    }

    /**
     * @return the attribute
     */
    public String getAttribute()
    {
        return attribute;
    }

    /**
     * @param attribute the attribute to set
     */
    public void setAttribute( String attribute )
    {
        this.attribute = attribute;
    }

    /**
     * @return the value
     */
    public String getValue()
    {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue( String value )
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return "(" + attribute + " " + super.getOperator() + " " + value + ")";
    }
}
