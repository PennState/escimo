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
 * TODO BranchNode.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BranchNode extends FilterNode
{
    private FilterNode leftNode;
    
    private FilterNode rightNode;
    
    public BranchNode( Operator operator )
    {
        super( operator );
    }
    
    public void addNode( FilterNode node )
    {
        if( leftNode == null )
        {
            leftNode = node;
        }
        else if( rightNode == null )
        {
            rightNode = node;
        }
        else
        {
            throw new IllegalStateException( "A branch node can only hold two nodes" );
        }
    }

    /**
     * @return the leftNode
     */
    public FilterNode getLeftNode()
    {
        return leftNode;
    }

    /**
     * @return the rightNode
     */
    public FilterNode getRightNode()
    {
        return rightNode;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        if( leftNode != null )
        {
            sb.append( leftNode );
        }
        
        sb.append( " AND " );
        
        if( rightNode != null )
        {
            sb.append( rightNode );
        }
        
        return sb.toString();
    }
    
}
