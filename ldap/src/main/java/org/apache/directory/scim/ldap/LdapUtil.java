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
package org.apache.directory.scim.ldap;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Base64;
import org.apache.directory.api.util.Strings;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.ldap.schema.ComplexType;
import org.apache.directory.scim.ldap.schema.MultiValType;
import org.apache.directory.scim.ldap.schema.SimpleType;
import org.apache.directory.scim.ldap.schema.SimpleTypeGroup;
import org.apache.directory.scim.schema.BaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * TODO LdapUtil.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapUtil
{
    private static final Logger LOG = LoggerFactory.getLogger( LdapUtil.class );
    
    public static void scimToLdapAttribute( BaseType bt, JsonElement el, Entry entry, RequestContext ctx ) throws LdapException
    {
        if( bt.isReadOnly() )
        {
            return;
        }
        
        SchemaManager ldapSchema = ( ( LdapResourceProvider ) ctx.getProviderService() ).getLdapSchema();
        
        if( bt instanceof ComplexType )
        {
            ComplexType ct = ( ComplexType ) bt;
            SimpleTypeGroup complexStg = ct.getAtGroup();
            
            JsonObject jo = ( JsonObject ) el;
            processSimpleTypeGroup( complexStg, jo, entry, ldapSchema, ct.getName() );
        }
        else if( bt instanceof MultiValType )
        {
            MultiValType mt = ( MultiValType ) bt;
            SimpleTypeGroup multiStg = mt.getAtGroup();
            
            JsonArray valArray = el.getAsJsonArray();
            
            for( JsonElement je : valArray )
            {
                // for the cases where multivalued attribute comes as an array of primitives
                // e.x "emails":['elecharny@apache.org', 'pajbam@apache.org']
                if( je.isJsonPrimitive() )
                {
                    storeSimpleAttribute( multiStg.getValueType(), je, entry, ldapSchema );
                }
                else
                {
                    JsonObject jo = ( JsonObject ) je;
                    processSimpleTypeGroup( multiStg, jo, entry, ldapSchema, mt.getName() );
                }
            }
        }
        else
        {
            storeSimpleAttribute( ( SimpleType ) bt, el, entry, ldapSchema );
        }
    }
    
    private static void processSimpleTypeGroup( SimpleTypeGroup stg, JsonObject jo, Entry entry, SchemaManager ldapSchema, String scimComplexAtName ) throws LdapException
    {
        for( java.util.Map.Entry<String, JsonElement> e : jo.entrySet() )
        {
            String scimAtName = e.getKey();
            
            SimpleType st = null;
            
            for( SimpleType temp : stg.getSubTypes() )
            {
                if( scimAtName.equals( temp.getName() ) )
                {
                    st = temp;
                    break;
                }
            }
            
            if( st != null )
            {
                storeSimpleAttribute( st, e.getValue(), entry, ldapSchema );
            }
            else
            {
                LOG.warn( "No LDAP mapping found for the sub attribute " + scimAtName + " of the complex attribute " + scimComplexAtName );
            }
        }
    }
    
    public static void storeSimpleAttribute( SimpleType st, JsonElement el, Entry entry, SchemaManager ldapSschema ) throws LdapException
    {
        if( st.isReadOnly() )
        {
            return;
        }
        
        String ldapAtName = st.getMappedTo();
        
        if( Strings.isEmpty( ldapAtName ) )
        {
            throw new IllegalArgumentException( "Attribute " + st.getName() + " is not mapped to any LDAP attribute in the config" );
        }
        
        AttributeType ldapType = ldapSschema.getAttributeType( ldapAtName );
        
        Attribute ldapAt = entry.get( ldapType );
        if( ldapAt == null )
        {
            ldapAt = new DefaultAttribute( ldapAtName );
            entry.add( ldapAt );
        }
        
        if( !ldapType.getSyntax().isHumanReadable() )
        {
            byte[] value = Base64.decode( el.getAsString().toCharArray() );
            ldapAt.add( value );
        }
        else
        {
            ldapAt.add( el.getAsString() );
        }
    }

}
