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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.scim.AttributeHandler;
import org.apache.directory.scim.SchemaMapper;
import org.apache.directory.scim.ldap.schema.ComplexType;
import org.apache.directory.scim.ldap.schema.MultiValType;
import org.apache.directory.scim.ldap.schema.ResourceSchema;
import org.apache.directory.scim.ldap.schema.SimpleType;
import org.apache.directory.scim.ldap.schema.SimpleTypeGroup;
import org.apache.directory.scim.schema.JsonSchema;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO LdapSchemaMapper.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapSchemaMapper implements SchemaMapper
{
    private SchemaManager ldapSchema;

    private static final Logger LOG = LoggerFactory.getLogger( LdapSchemaMapper.class );

    private List<ResourceSchema> resourceSchemas = new ArrayList<ResourceSchema>();

    private Map<String,ResourceSchema> uriToResSchema;
    
    private Map<String,JsonSchema> jsonSchemas;
    
    public LdapSchemaMapper( Map<String,JsonSchema> jsonSchemas )
    {
        this.jsonSchemas = jsonSchemas;
    }

    
    /**
     * @return the ldapSchema
     */
    public SchemaManager getLdapSchema()
    {
        return ldapSchema;
    }

    
    public void setLdapSchema( SchemaManager ldapSchema )
    {
        this.ldapSchema = ldapSchema;
    }


    public AttributeType getLdapAttributeType( String name )
    {
        return ldapSchema.getAttributeType( name );
    }

    
    public void loadMappings()
    {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream( "escimo-ldap-mapping.xml" );
        loadMappings( in );
    }


    public void loadMappings( InputStream in )
    {
        if ( in == null )
        {
            throw new IllegalArgumentException( "Mapping file inputstream cannot be null" );
        }

        BufferedReader r = new BufferedReader( new InputStreamReader( in ) );

        try
        {
            StringBuilder sb = new StringBuilder();
            String s = null;

            while ( ( s = r.readLine() ) != null )
            {
                sb.append( s )
                    .append( "\n" );
            }

            Document doc = DocumentHelper.parseText( sb.toString() );

            // the entities element
            Element root = doc.getRootElement();
            if ( root.elements().isEmpty() )
            {
                throw new IllegalStateException( "Invalid schema mapping file" );
            }

            List<Element> elmResources = root.elements( "resourceType" );
            List<Element> lstSchema = root.elements( "schema" );
            
            uriToResSchema = new HashMap<String, ResourceSchema>();
            Map<String, AttributeHandler> atHandlersMap = loadAtHandlers( root.element( "atHandlers" ) );
            
            for( Element resElem : elmResources )
            {
                String baseDn = resElem.attributeValue( "baseDn" );
                String filter = resElem.attributeValue( "filter" );
                ResourceSchema rs = new ResourceSchema( baseDn, filter );
                parseResourceSchema( resElem, lstSchema, rs, atHandlersMap );
                
                resourceSchemas.add( rs );
            }
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to load the schema mappings", e );
            throw new RuntimeException( e );
        }
        finally
        {
            if ( r != null )
            {
                try
                {
                    r.close();
                }
                catch ( IOException e )
                {
                    LOG.warn( "Failed to close the inputstream of the schema mapping file", e );
                }
            }
        }
    }


    private void parseResourceSchema( Element elmResourceSchema, List<Element> lstSchema, ResourceSchema resourceSchema, Map<String, AttributeHandler> atHandlersMap )
    {
        Element elmObjectClass = elmResourceSchema.element( "objectClasses" );
        List<Element> elmOcs = elmObjectClass.elements( "objectClass" );
        for( Element el : elmOcs )
        {
            resourceSchema.addObjectClass( el.getText() );
        }
        
        List<Element> lstRef = elmResourceSchema.elements( "schemaRef" );
        
        for ( Element ref : lstRef )
        {
            String refId = ref.attributeValue( "id" );
            for ( Element elmSchema : lstSchema )
            {
                String schemaId = elmSchema.attributeValue( "id" );
                if ( refId.equals( schemaId ) )
                {
                    parseSchema( elmSchema, resourceSchema, atHandlersMap );
                    for( String uri : resourceSchema.getSchemaIds() )
                    {
                        uriToResSchema.put( uri, resourceSchema );
                    }
                    break;
                }
            }
        }
        
        resourceSchema.setName( elmResourceSchema.attributeValue( "name" ) );
        
        Element rdn = elmResourceSchema.element( "rdnAtRef" );
        
        SimpleType rdnType = ( SimpleType ) resourceSchema.getAttribute( rdn.attributeValue( "name" ) );
        resourceSchema.setRdnType( rdnType );
        
        Element uri = elmResourceSchema.element( "reqUri" );
        resourceSchema.setReqUri( uri.attributeValue( "value" ) );
    }


    private void parseSchema( Element schemaRoot, ResourceSchema resourceSchema, Map<String, AttributeHandler> atHandlersMap )
    {
        String uri = schemaRoot.attributeValue( "uri" );

        resourceSchema.addUri( uri );

        JsonSchema json = jsonSchemas.get( uri );
        
        List<Element> simpleAtElmList = schemaRoot.elements( "attribute" );

        for ( Element el : simpleAtElmList )
        {
            SimpleType st = parseSimpleType( el, uri, atHandlersMap );
            if ( st != null )
            {
                st.setReadOnly( json.isReadOnly( st.getName() ) );
                resourceSchema.addAttributeType( st.getName(), st );
            }
        }

        // load complex-attributes
        List<Element> complexAtElmList = schemaRoot.elements( "complex-attribute" );

        for ( Element elmComplex : complexAtElmList )
        {
            String name = elmComplex.attributeValue( "name" );

            if ( Strings.isEmpty( name ) )
            {
                throw new IllegalStateException( "name is missing in the complex-attribute configuration element "
                    + elmComplex.asXML() );
            }

            boolean show = getShowVal( elmComplex );
            
            Element atGrpElm = elmComplex.element( "at-group" );
            SimpleTypeGroup stg = parseAtGroup( atGrpElm, uri, name, atHandlersMap );
            ComplexType ct = null;
            if ( stg != null )
            {
                ct = new ComplexType( uri, name, show, stg );
            }
            
            AttributeHandler handler = getHandlerInstance( elmComplex, atHandlersMap );
            
            // if attribute handler is present then create the type
            if( ( ct == null ) && ( handler != null ) )
            {
                ct = new ComplexType( uri, name, show, null );
            }
            
            if( ct != null )
            {
                ct.setHandler( handler );
                ct.setReadOnly( json.isReadOnly( name ) );
                resourceSchema.addAttributeType( name, ct );
            }
        }

        // load multival-attributes
        List<Element> multivalAtElmList = schemaRoot.elements( "multival-attribute" );

        for ( Element elmMultiVal : multivalAtElmList )
        {
            String name = elmMultiVal.attributeValue( "name" );

            if ( Strings.isEmpty( name ) )
            {
                throw new IllegalStateException( "name is missing in the multival-attribute configuration element "
                    + elmMultiVal.asXML() );
            }

            boolean showMultiVal = getShowVal( elmMultiVal );
            
            MultiValType mt = null;
            
            Element elmAtGroup = elmMultiVal.element( "at-group" );
            if ( elmAtGroup != null )
            {
                SimpleTypeGroup stg = parseAtGroup( elmAtGroup, uri, name, atHandlersMap );
                if ( stg != null )
                {
                    mt = new MultiValType( uri, name, showMultiVal, stg );
                }
            }
            
            AttributeHandler handler = getHandlerInstance( elmMultiVal, atHandlersMap );

            if( ( mt == null ) && ( handler != null ) )
            {
                mt = new MultiValType( uri, name, showMultiVal, ( SimpleTypeGroup ) null );
            }
            
            if( mt != null )
            {
                mt.setHandler( handler );
                mt.setReadOnly( json.isReadOnly( name ) );
                resourceSchema.addAttributeType( name, mt );
            }
        }
    }


    private SimpleTypeGroup parseAtGroup( Element elmAtGroup, String uri, String parentAtName, Map<String, AttributeHandler> atHandlersMap )
    {
        SimpleTypeGroup stg = null;

        List<SimpleType> lstSTypes = null;
        
        JsonSchema json = jsonSchemas.get( uri );
        
        if ( elmAtGroup != null )
        {
            lstSTypes = new ArrayList<SimpleType>();

            List<Element> elmSTypes = elmAtGroup.elements( "attribute" );
            for ( Element elmAt : elmSTypes )
            {
                SimpleType st = parseSimpleType( elmAt, uri, atHandlersMap );
                if ( st != null )
                {
                    st.setReadOnly( json.isReadOnly( parentAtName + "." + st.getName() ) );
                    lstSTypes.add( st );
                }
            }

            if ( !lstSTypes.isEmpty() )
            {
                stg = new SimpleTypeGroup( lstSTypes );
            }
        }

        return stg;
    }


    private SimpleType parseSimpleType( Element el, String uri, Map<String, AttributeHandler> atHandlersMap )
    {
        String name = el.attributeValue( "name" );

        if ( Strings.isEmpty( name ) )
        {
            throw new IllegalStateException( "name is missing in the attribute configuration element " + el.asXML() );
        }

        String mappedTo = el.attributeValue( "mappedTo" );
        String handlerRef = el.attributeValue( "handlerRef" );

        if ( Strings.isEmpty( mappedTo ) && Strings.isEmpty( handlerRef ) )
        {
            LOG.debug( "Neither LDAP attribute or a attribute handler was mapped to the SCIM attribute {}, skipping", name );
            return null;
        }

        boolean show = getShowVal( el );

        SimpleType st = new SimpleType( uri, name, show, mappedTo );
        
        AttributeHandler handler = getHandlerInstance( el, atHandlersMap );
        st.setHandler( handler );
        
        return st;
    }
    
    private boolean getShowVal(Element el)
    {
        String showVal = el.attributeValue( "show" );

        if ( Strings.isEmpty( showVal ) )
        {
            showVal = "true";
        }
        
        return Boolean.parseBoolean( showVal );
    }
    
    
    private Map<String, AttributeHandler> loadAtHandlers(Element atHndlrRoot)
    {
        if( atHndlrRoot == null )
        {
            return Collections.EMPTY_MAP;
        }
        
        Map<String, AttributeHandler> mapHandlers = new HashMap<String, AttributeHandler>();
        
        List<Element> elmHandlerList = atHndlrRoot.elements( "handler" );
        
        for( Element el : elmHandlerList )
        {
            String fqcn = el.attributeValue( "class" );
            String name = el.attributeValue( "name" );
            
            if( Strings.isEmpty( name ) )
            {
                throw new IllegalStateException( "Name is missing in the handler element " + el.asXML() );
            }
            
            try
            {
                AttributeHandler handler = (AttributeHandler ) Class.forName( fqcn ).newInstance();
                mapHandlers.put( name, handler );
            }
            catch( Exception e )
            {
                throw new RuntimeException( "Failed to load the attribute handler " + fqcn, e );
            }
        }
        
        return mapHandlers;
    }

    private AttributeHandler getHandlerInstance( Element elmAttribute, Map<String,AttributeHandler> atHandlersMap )
    {
        String handlerRef = elmAttribute.attributeValue( "handlerRef" );
        String handlerClass = elmAttribute.attributeValue( "handlerClass" );
        List<Element> args = elmAttribute.elements( "handlerArg" );
        
        AttributeHandler handler = null;
        
        if( !Strings.isEmpty( handlerRef ) ) 
        {
            handler = atHandlersMap.get( handlerRef );
        }
        
        if( !Strings.isEmpty( handlerClass ) )
        {
            if( handlerRef != null )
            {
                throw new IllegalArgumentException("An attribute type cannot contain both handlerRef and handlerClass attributes");
            }

            try
            {
                handler = (AttributeHandler ) Class.forName( handlerClass ).newInstance();
                
                if(args != null)
                {
                    for( Element e : args )
                    {
                        String name = e.attributeValue( "name" );
                        String value = e.attributeValue( "value" );
                        
                        Field f = handler.getClass().getDeclaredField( name );
                        f.setAccessible( true );
                        f.set( handler, value );
                    }
                }
            }
            catch( Exception e )
            {
                throw new RuntimeException( "Failed to create handler for the attribute " + elmAttribute.asXML() , e );
            }
        }

        return handler;
    }


    List<ResourceSchema> getResourceSchemas()
    {
        return resourceSchemas;
    }
    
}
