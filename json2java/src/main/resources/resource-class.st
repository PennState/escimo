$if(package)$
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

package $package$;

import org.apache.directory.scim.schema.*;


/*
 * Generated on $date$ using JsonToJava plugin 
 */
 
$endif$
$visibility$ $static$ class $className$ $if(package)$extends CoreResource$endif$
{

    $allAttrs: { attr | private $attr.javaType$ $attr.name$;}; separator="\n\n"$

   $if(schemaId)$
    public static String SCHEMA_ID = "$schemaId$";
   $endif$
    
    public $className$()
    {
	    $if(schemaId)$
	     super.setSchemaId( SCHEMA_ID );
	    $endif$
	    
	    $if(resourceDesc)$
	     super.setResourceDesc( "$resourceDesc$" );
	    $endif$
    }
   
   $allAttrs: { attr |
   $if(!attr.readOnly)$ 
    public void set$attr.methodName$( $attr.javaType$ $attr.name$ )
    {      
       this.$attr.name$ = $attr.name$;
    }
   $endif$
   
    $if(attr.boolean)$
    public $attr.javaType$ is$attr.methodName$()
    $else$
    public $attr.javaType$ get$attr.methodName$()
    $endif$
    {
       return $attr.name$;
    }
    
   }$
    
    
    $allInnerClasses: { innerClass | $innerClass$ }; separator="\n\n"$
}