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

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.StringValue;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.GreaterEqNode;
import org.apache.directory.api.ldap.model.filter.LessEqNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.SyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.GeneralizedTimeSyntaxChecker;
import org.apache.directory.api.util.Base64;
import org.apache.directory.api.util.Strings;
import org.apache.directory.scim.AttributeHandler;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.ldap.schema.ComplexType;
import org.apache.directory.scim.ldap.schema.MultiValType;
import org.apache.directory.scim.ldap.schema.ResourceSchema;
import org.apache.directory.scim.ldap.schema.SimpleType;
import org.apache.directory.scim.ldap.schema.SimpleTypeGroup;
import org.apache.directory.scim.schema.BaseType;
import org.apache.directory.scim.search.BranchNode;
import org.apache.directory.scim.search.FilterNode;
import org.apache.directory.scim.search.TerminalNode;
import org.apache.directory.scim.util.ResourceUtil;
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
        storeSimpleAttribute( st, el.getAsString(), entry, ldapSschema );
    }
    
    private static void storeSimpleAttribute( SimpleType st, String encodedOrPlainStrVal, Entry entry, SchemaManager ldapSschema ) throws LdapException
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
            ldapAt = new DefaultAttribute( ldapType );
            entry.add( ldapAt );
        }
        
        if( !ldapType.getSyntax().isHumanReadable() )
        {
            byte[] value = Base64.decode( encodedOrPlainStrVal.toCharArray() );
            ldapAt.add( value );
        }
        else
        {
            ldapAt.add( encodedOrPlainStrVal );
        }
    }

    
    
    //================ PATCH ========
    
    public static void patchAttributes( Entry existingEntry, JsonObject obj, RequestContext ctx, ResourceSchema resourceSchema, ModifyRequest modReq ) throws Exception
    {
        for( java.util.Map.Entry<String, JsonElement> e : obj.entrySet() )
        {
            String name = e.getKey();
            
            if( name.startsWith( "urn:scim:schemas:" ) )
            {
                continue;
            }
            
            BaseType bt = resourceSchema.getAttribute( name );
            
            if( bt == null )
            {
                throw new IllegalArgumentException( "Unknown attribute name "  + name + " is present in the JSON payload that has no corresponding mapping in the escimo-ldap-mapping.xml file" );
            }
            
            if( bt.isReadOnly() )
            {
                continue;
            }

            AttributeHandler handler = bt.getHandler();
            
            if( handler != null )
            {
                handler.patch( bt, e.getValue(), existingEntry, ctx, modReq );
            }
            else
            {
                patchLdapAttribute( bt, e.getValue(), existingEntry, ctx, modReq );
            }
        }
    }

    
    public static void patchLdapAttribute( BaseType bt, JsonElement el, Entry entry, RequestContext ctx, ModifyRequest modReq ) throws LdapException
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
            patchSimpleTypeGroup( complexStg, jo, entry, ldapSchema, ct.getName(), modReq );
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
                    patchSimpleAttribute( multiStg.getValueType(), je, entry, ldapSchema, modReq, false );
                }
                else
                {
                    JsonObject jo = ( JsonObject ) je;
                    patchSimpleTypeGroup( multiStg, jo, entry, ldapSchema, mt.getName(), modReq );
                }
            }
        }
        else
        {
            patchSimpleAttribute( ( SimpleType ) bt, el, entry, ldapSchema, modReq, false );
        }
    }


    // BaseType atType, JsonElement jsonData, Object targetEntry, Object patchCtx 
    private static void patchSimpleTypeGroup( SimpleTypeGroup stg, JsonObject jo, Entry entry, SchemaManager ldapSchema, String scimComplexAtName, ModifyRequest modReq ) throws LdapException
    {
        //ModificationOperation operation = REPLACE_ATTRIBUTE; // the default
        
        boolean delete = false;
        
        JsonElement atOperation = jo.get( "operation" );
        if( atOperation != null )
        {
            if( atOperation.getAsString().equalsIgnoreCase( "delete" ) )
            {
                //operation = REMOVE_ATTRIBUTE;
                delete = true;
            }
            
            jo.remove( "operation" );
        }
        
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
                patchSimpleAttribute( st, e.getValue(), entry, ldapSchema, modReq, delete );
            }
            else
            {
                LOG.warn( "No LDAP mapping found for the sub attribute " + scimAtName + " of the complex attribute " + scimComplexAtName );
            }
        }
    }
    
    public static void patchSimpleAttribute( SimpleType st, JsonElement el, Entry entry, SchemaManager ldapSschema, ModifyRequest modReq, boolean delete ) throws LdapException
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
        
        if( !ldapType.getSyntax().isHumanReadable() )
        {
            byte[] value = Base64.decode( el.getAsString().toCharArray() );
            
            if( delete )
            {
                modReq.remove( ldapAtName, value );
            }
            else if( !entry.contains( ldapType, value ) )
            {
                modReq.add( ldapAtName, value );
            }
        }
        else
        {
            String value = el.getAsString();

            if( delete )
            {
                modReq.remove( ldapAtName, value );
            }
            else if( !entry.contains( ldapType, value ) )
            {
                modReq.add( ldapAtName, value );
            }
            
//            if( operation == REPLACE_ATTRIBUTE )
//            {
//                modReq.replace( ldapAtName, value );
//            }
//            else if( operation == REMOVE_ATTRIBUTE )
//            {
//                modReq.remove( ldapAtName, value );
//            }
//            else
//            {
//                modReq.add( ldapAtName, value );
//            }
        }
    }

    
    public static void deleteAttribute( BaseType bt, Entry existingEntry, ModifyRequest modReq )
    {
        if( bt.isReadOnly() )
        {
            return;
        }

        SimpleTypeGroup stg = null;
        
        if( bt instanceof MultiValType )
        {
            MultiValType mt = ( MultiValType ) bt;
            stg = mt.getAtGroup();
        }
        else if ( bt instanceof ComplexType )
        {
            ComplexType ct = ( ComplexType ) bt;
            stg = ct.getAtGroup();
        }

        if( stg != null )
        {
            for( SimpleType st : stg.getSubTypes() )
            {
                if( st.isReadOnly() )
                {
                    continue;
                }
                
                String name = st.getMappedTo();
                
                if( !Strings.isEmpty( name ) )
                {
                    Attribute ldapAt = existingEntry.get( name );
                    if( ldapAt != null )
                    {
                        modReq.remove( ldapAt );
                    }
                }
            }

            return;
        }
        
        SimpleType st = ( SimpleType ) bt;
        
        String name = st.getMappedTo();
        
        if( !Strings.isEmpty( name ) )
        {
            Attribute ldapAt = existingEntry.get( name );
            if( ldapAt != null )
            {
                modReq.remove( ldapAt );
            }
        }
    }
    
    
    //------------------ search filter processing -----------------
    
//    public static ExprNode scimToLdapFilter( FilterNode scimFilter )
//    {
//        
//    }
    
    
    public static ExprNode _scimToLdapFilter( FilterNode scimFilter, ResourceSchema scimSchema, SchemaManager ldapSchema, LdapResourceProvider provider )
    {
        ExprNode ldapFilter = null;
        
        if( scimFilter instanceof TerminalNode )
        {
            TerminalNode tn = ( TerminalNode ) scimFilter;

            List<AttributeType> atList = provider.getLdapTypes( tn.getAttribute(), scimSchema );
            
            // if there is none or more than one do not consider that scim attribute
            if( ( atList == null ) || ( atList.size() > 1 ) )
            {
                return null;
            }
            
            AttributeType at = atList.get( 0 );
            
            String value = tn.getValue();
            
            SyntaxChecker sc = at.getSyntax().getSyntaxChecker();
            
            if( sc instanceof GeneralizedTimeSyntaxChecker )
            {
                value = ResourceUtil.toLdapDate( value );
            }
            
            switch( scimFilter.getOperator() )
            {
                case EQ:
                    ldapFilter = new EqualityNode<String>( at, new StringValue( value ) );
                    break;
                  
                case CO:
                    List<String> anyPattern = new ArrayList<String>();
                    anyPattern.add( value );
                    ldapFilter = new SubstringNode( anyPattern, at, null, null );
                    break;
                
                case GT:
                case GE:
                    ldapFilter = new GreaterEqNode<String>( at, new StringValue( value ) );
                    break;
                    
                case LT:
                case LE:
                    ldapFilter = new LessEqNode<String>( at, new StringValue( value ) );
                    break;
                    
                case PR:
                    ldapFilter = new PresenceNode( at );
                    break;
                    
                case SW:
                    ldapFilter = new SubstringNode( at, value, null );
                    break;
            }
        }
        else if( scimFilter instanceof BranchNode )
        {
            BranchNode bn = ( BranchNode ) scimFilter;
            
            ExprNode child1 = _scimToLdapFilter( bn.getLeftNode(), scimSchema, ldapSchema, provider );
            
            ExprNode child2 = _scimToLdapFilter( bn.getRightNode(), scimSchema, ldapSchema, provider );
            
            switch( scimFilter.getOperator() )
            {
                case AND:
                    AndNode andNode = new AndNode();
                    if( child1 != null )
                    {
                        andNode.addNode( child1 );
                    }
                    
                    if( child2 != null )
                    {
                        andNode.addNode( child2 );
                    }
                    
                    ldapFilter = andNode;
                    break;
                    
                case OR:
                    OrNode orNode = new OrNode();
                    if( child1 != null )
                    {
                        orNode.addNode( child1 );
                    }
                    
                    if( child2 != null )
                    {
                        orNode.addNode( child2 );
                    }
                    
                    ldapFilter = orNode;
                    break;
            }
        }
        
        return ldapFilter;
    }

}
