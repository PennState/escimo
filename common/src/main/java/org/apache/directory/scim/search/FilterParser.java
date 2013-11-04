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


import static org.apache.directory.scim.search.Operator.UNKNOWN;

import java.util.Stack;


/**
 * A Parser for converting a search filter string into a FilterNode tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class FilterParser
{

    /**
     * Internal class for keeping track of the index position while parsing the filter.
     */
    private static class Position
    {
        int val;


        private Position()
        {
            this( 0 );
        }


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


    /**
     * parses the given filter string and converts into a FilterNode tree
     * @param filter the filter string
     * @return
     */
    public static FilterNode parse( String filter )
    {
        if ( filter == null )
        {
            return null;
        }
        
        Position pos = new Position( 0 );

        int len = filter.length();

        FilterNode node = null;

        outer: while ( pos.val < len )
        {
            char c = filter.charAt( pos.val );

            FilterNode next = null;

            switch ( c )
            {
                case ' ':
                    pos.increment();
                    continue outer;

                case '(':
                    String group = getWithinParenthesis( pos, filter );
                    next = parse( group );
                    break;

                default:
                    next = parseNode( pos, filter );
            }

            //System.out.println("====\n" + next + "\n======");

            node = addChildNode( node, next );
        }

        return node;
    }


    /**
     * adds the given child node to the parent and returns the
     * newly created FilterNode
     * 
     * @param parent the parent node (will be null if the child is the first parsed node)
     * @param child the current node
     * @return
     */
    private static FilterNode addChildNode( FilterNode parent, FilterNode child )
    {
        if ( parent == null )
        {
            return child;
        }

        if ( parent instanceof BranchNode )
        {
            BranchNode bn = ( BranchNode ) parent;

            if ( !bn.hasBothChildren() )
            {
                bn.addNode( child );
                return parent;
            }
            else if ( ( child != null ) && ( child instanceof BranchNode ) )
            {
                ( ( BranchNode ) child ).addNode( parent );
                return child;
            }
        }

        if ( child instanceof BranchNode )
        {
            ( ( BranchNode ) child ).addNode( parent );
            return child;
        }

        return null;
    }


    /**
     * parses a single terminal node expression, e.x userName eq 'bjensen' or userName pr
     *
     * @param pos the current position of the index in filter string
     * @param filter the terminal node expression
     * @return
     */
    private static FilterNode parseNode( Position pos, String filter )
    {
        FilterNode node = null;

        String attribute = parseToken( pos, filter );

        Operator branchOperator = Operator.getByName( attribute );

        if ( branchOperator != UNKNOWN )
        {
            switch ( branchOperator )
            {
                case AND:
                case OR:
                    return new BranchNode( branchOperator );

                default:
                    throw new IllegalArgumentException(
                        "Invalid predicate in filter, expected an attribute or an operator token but found "
                            + attribute );
            }
        }

        Operator operator = Operator.getByName( parseToken( pos, filter ) );

        int curPos = pos.val;

        String value = null;

        String valOrOperator = parseToken( pos, filter );

        if ( Operator.getByName( valOrOperator ) != UNKNOWN )
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
                throw new IllegalArgumentException(
                    "Invalid predicate in filter, expected a non branching operator but found " + operator );

            default:
                TerminalNode tn = new TerminalNode( operator );
                tn.setAttribute( attribute );
                tn.setValue( value );
                node = tn;
                break;
        }

        return node;
    }


    /**
     * parses a string delimited by spaces (excluding the ones present inside the double quotes)
     * 
     * @param pos the current position of the filter
     * @param filter the filter expression
     * @return
     */
    private static String parseToken( Position pos, String filter )
    {
        boolean foundNonSpace = false;

        StringBuilder sb = new StringBuilder();

        char prevChar = ' ';

        boolean isEscaped = false;

        boolean startQuote = false;

        boolean endQuote = false;

        while ( pos.val < filter.length() )
        {
            char c = filter.charAt( pos.val );
            pos.increment();

            if ( ( prevChar == '\\' ) && ( c != '\\' ) )
            {
                isEscaped = true;
            }

            if ( c == '"' )
            {
                if ( !isEscaped )
                {
                    if ( startQuote )
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

            switch ( c )
            {
                case ' ':

                    if ( !foundNonSpace || ( startQuote && !endQuote ) )
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


    /**
     * gives the complete string present inside the open and end parentheses
     * 
     * @param pos the current position in filter
     * @param filter the filter expression
     * @return
     */
    private static String getWithinParenthesis( Position pos, String filter )
    {
        int start = -1;
        int end = -1;

        Stack<Integer> stack = new Stack<Integer>();

        char prevChar = ' ';

        boolean startQuote = false;

        boolean endQuote = false;

        boolean stop = false;

        while ( !stop && ( pos.val < filter.length() ) )
        {
            char c = filter.charAt( pos.val );

            switch ( c )
            {
                case '"':
                    if ( startQuote && prevChar != '\\' )
                    {
                        endQuote = true;
                    }
                    else if ( !startQuote )
                    {
                        startQuote = true;
                    }
                    break;

                case '(':
                    if ( !startQuote )
                    {
                        if ( start == -1 )
                        {
                            start = pos.val + 1;
                        }

                        stack.push( pos.val );
                    }
                    break;

                case ')':
                    if ( !startQuote )
                    {
                        if ( stack.size() == 1 )
                        {
                            end = pos.val;
                            stop = true;
                        }
                        else
                        {
                            stack.pop();
                        }
                    }
                    break;
            }

            if ( endQuote )
            {
                startQuote = false;
                endQuote = false;
            }

            prevChar = c;
            pos.increment();
        }

        return filter.substring( start, end );
    }

}
