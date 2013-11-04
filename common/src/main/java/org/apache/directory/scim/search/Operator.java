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
 * An enum to hold all the operators used in search filters.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum Operator
{
    /**
     * The attribute and operator values must be identical for a match.     
     */
    EQ("eq", "equal"),
    
    /**
     * The entire operator value must be a substring of the attribute value for a match.                             
     */
    CO("co", "contains"),
    
    /**
     *  The entire operator value must be a substring of the attribute value, 
     *  starting at the beginning of the attribute value. This criterion is
     *  satisfied if the two strings are identical.                          
     */
    SW("sw", "starts with"),
    
    /**
     * If the attribute has a non-empty value, or if it contains a non-empty
     * node for complex attributes there is a match.                             
     */
    PR("pr", "present (has value)"),
    
    /**
     * If the attribute value is greater than operator value, there is a
     * match. The actual comparison is dependent on the attribute type. For
     * string attribute types, this is a lexicographical comparison and for
     * DateTime types, it is a chronological comparison.                          
     */
    GT("gt", "greater than"),
    
    /**
     * If the attribute value is greater than or equal to the operator value,
     * there is a match. The actual comparison is dependent on the attribute 
     * type. For string attribute types, this is a lexicographical comparison
     * and for DateTime types, it is a chronological comparison.       
     */
    GE("ge", "greater than or equal"),
    
    /**
     * If the attribute value is less than operator value, there is a match. The
     * actual comparison is dependent on the attribute type.  For string attribute
     * types, this is a lexicographical comparison and for DateTime types, it is a
     * chronological comparison.       
     */
    LT("lt", "less than"),
    
    /**
     * If the attribute value is less than or equal to the operator value, there
     * is a match. The actual comparison is dependent on the attribute type. For
     * string attribute types, this is a lexicographical comparison and for 
     * DateTime types, it is a chronological comparison.                          
     */
    LE("le", "less than or equal"),
    
    /**
     * The filter is only a match if both expressions evaluate to true  
     */
    AND("and", "logical and"),
    
    /**
     * The filter is a match if either expression evaluates to true.  
     */
    OR("or", "logical or"),
    
    /** Unknown operator */
    UNKNOWN("", "unknown operator");
    
    private String val;
    
    private String desc;
    
    private Operator( String val, String desc )
    {
        this.val = val;
        this.desc = desc;
    }
    
    
    public static Operator getByName( String name )
    {
        name = name.toLowerCase();
        
        if( name.equals( EQ.val ) )
        {
            return EQ;
        }
        else if( name.equals( CO.val ) )
        {
            return CO;
        }
        else if( name.equals( SW.val ) )
        {
            return SW;
        }
        else if( name.equals( PR.val ) )
        {
            return PR;
        }
        else if( name.equals( GT.val ) )
        {
            return GT;
        }
        else if( name.equals( GE.val ) )
        {
            return GE;
        }
        else if( name.equals( LT.val ) )
        {
            return LT;
        }
        else if( name.equals( LE.val ) )
        {
            return LE;
        }
        else if( name.equals( AND.val ) )
        {
            return AND;
        }
        else if( name.equals( OR.val ) )
        {
            return OR;
        }
        
        return UNKNOWN;
    }
}
