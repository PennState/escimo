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

import static org.apache.directory.scim.search.Operator.*;

/**
 * TODO FilterParser.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class FilterParser
{
    
    private static class Position
    {
        int val;
        private Position( int pos )
        {
            this.val = pos;
        }
        
        private void increment()
        {
            val++;
        }
        
        private void set( int pos )
        {
            this.val = pos;
        }
        
        private void decrement()
        {
            val--;
        }
    }
    
    public static FilterNode parse( String filter )
    {
        Position pos = new Position( 0 );
        
        int len = filter.length();
        
        FilterNode node = null;
        
        char prevChar = ' ';
        
        while( pos.val < len )
        {
            char c = filter.charAt( pos.val );
            
            switch( c )
            {
                case '(':
                    //FIXME handle groupings
                    break;
                
                default:
                    FilterNode next = parseNode( pos, filter );
                    if( next instanceof BranchNode )
                    {
                        if( node != null )
                        {
                            ( ( BranchNode ) next ).addNode( node );
                        }
                        
                        node = next;
                    }
                    else if ( node instanceof BranchNode )
                    {
                        ( ( BranchNode ) node ).addNode( next );
                    }
                    else
                    {
                        node = next;
                    }
            }
            
            prevChar = c;
        }
        
        return node;
    }
    
    private static FilterNode parseNode( Position pos, String filter )
    {
        FilterNode node = null;

        String attribute = parseToken( pos, filter );
        
        Operator branchOperator = Operator.getByName( attribute );
        
        if( branchOperator != UNKNOWN )
        {
            switch ( branchOperator )
            {
                case AND:
                case OR:
                     return new BranchNode( branchOperator );

                default:
                    throw new IllegalArgumentException( "Invalid predicate in filter, expected an attribute or an operator token but found " + attribute );
            }            
        }
        
        
        Operator operator = Operator.getByName( parseToken( pos, filter ) );
        
        int curPos = pos.val;
        
        String value = null;
        
        String valOrOperator = parseToken( pos, filter );
        
        if( Operator.getByName( valOrOperator ) != UNKNOWN )
        {
            // move back
            pos.set( curPos );
        }
        else
        {
            value = valOrOperator;
        }
        
        switch ( operator )
        {
            case AND:
            case OR:
                 //node = new BranchNode( operator );
                throw new IllegalArgumentException( "Invalid predicate in filter, expected a non branching operator but found " + operator );

            default:
                TerminalNode tn= new TerminalNode( operator );
                tn.setAttribute( attribute );
                tn.setValue( value );
                node = tn;
                break;
        }
        
        return node;
    }
    
    private static String parseToken( Position pos, String filter )
    {
        boolean foundNonSpace = false;
        
        StringBuilder sb = new StringBuilder();
        
        char prevChar = ' ';
        
        boolean isEscaped = false;
        
//        boolean isEnclosed = false;
        
        boolean startQuote = false;
        
        boolean endQuote = false;
        
        while( pos.val < filter.length() )
        {
            char c = filter.charAt( pos.val );
            pos.increment();
            
            if( ( prevChar == '\\' ) && ( c != '\\') )
            {
                isEscaped = true;
            }
            
            if( c == '"' )
            {
//                isEnclosed = true;
                
                if( !isEscaped )
                {
                    if( startQuote )
                    {
                        endQuote = true;
                    }
                    else
                    {
                        startQuote = true;
                    }
                    
                    continue;
                }
            }
            
            switch( c )
            {
                case ' ' :
                    
                    if( !foundNonSpace || ( startQuote && !endQuote ) )
                    {
                        continue;
                    }
                    else
                    {
                        return sb.toString();
                    }
                    
                default:
                    sb.append( c );
                    foundNonSpace = true;
            }
            
            prevChar = c;
        }
        
        return sb.toString();
    }
    
    public static void main( String[] args )
    {
        String filter = "(x eq y) and userName   eq \"bjensen\"";
        
        FilterNode node = parse( filter );
        System.out.println( node );
    }
}
