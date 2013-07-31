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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.scim.SchemaMapper;
import org.apache.directory.scim.ldap.schema.BaseType;
import org.apache.directory.scim.ldap.schema.ComplexType;
import org.apache.directory.scim.ldap.schema.MultiValType;
import org.apache.directory.scim.ldap.schema.ResourceSchema;
import org.apache.directory.scim.ldap.schema.SimpleType;
import org.apache.directory.scim.ldap.schema.SimpleTypeGroup;
import org.apache.directory.scim.ldap.schema.TypedType;
import org.apache.directory.scim.ldap.schema.UserSchema;
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

    private Map<String, BaseType> groupSchema = new HashMap<String, BaseType>();

    private UserSchema userSchema;
    
    public LdapSchemaMapper()
    {
    }

    public LdapSchemaMapper( LdapConnection connection )
    {
        try
        {
            connection.loadSchema();
        }
        catch ( LdapException e )
        {
            LOG.debug( "Failed to load schema from the server", e );
            LOG.info( "Could not load schema from the LDAP server, disabling schema checks" );
        }

        ldapSchema = connection.getSchemaManager();
    }


    /**
     * @return the userSchema
     */
    public UserSchema getUserSchema()
    {
        return userSchema;
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
            
            Element elmUser = root.element( "userType" );
            
            String baseDn = elmUser.attributeValue( "baseDn" );
            String filter = elmUser.attributeValue( "filter" );

            userSchema = new UserSchema( baseDn, filter );
            
            List<Element> lstSchema = root.elements( "schema" );
            List<Element> lstRef = elmUser.elements( "schemaRef" );
            parseResourceSchema( lstRef, lstSchema, userSchema );
            
            /*
            Element elmGroup = root.element( "groupType" );
            
            String baseDn = elmGroup.attributeValue( "baseDn" );
            String filter = elmGroup.attributeValue( "filter" );

            userSchema = new GroupSchema( baseDn, filter );
            
            List<Element> lstSchema = elmGroup.elements( "schema" );
            List<Element> lstRef = elmGroup.elements( "schemaRef" );
            */
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

    private void parseResourceSchema( List<Element> lstRef, List<Element> lstSchema, ResourceSchema resourceSchema )
    {
        for( Element ref : lstRef )
        {
            String refId = ref.attributeValue( "id" );
            for( Element elmSchema : lstSchema )
            {
                String schemaId = elmSchema.attributeValue( "id" );
                if(refId.equals( schemaId ))
                {
                    parseSchema( elmSchema, resourceSchema );
                    break;
                }
            }
        }

    }
    

    private void parseSchema( Element schemaRoot, ResourceSchema resourceSchema )
    {
        String uri = schemaRoot.attributeValue( "uri" );
        
        resourceSchema.addUri( uri );
        
        List<Element> simpleAtElmList = schemaRoot.elements( "attribute" );

        for ( Element el : simpleAtElmList )
        {
            SimpleType st = parseSimpleType( el, uri );
            if ( st != null )
            {
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


            List<SimpleType> stList = new ArrayList<SimpleType>();
            
            Element atGrpElm = elmComplex.element( "at-group" );
            SimpleTypeGroup stg = parseAtGroup( atGrpElm, uri );
            if( stg != null )
            {
                ComplexType ct = new ComplexType( name, uri, stg );
                resourceSchema.addAttributeType( name, ct );
            }

        }
        
        // load multival-attributes
        List<Element> multivalAtElmList = schemaRoot.elements( "multival-attribute" );

        for ( Element elmComplex : multivalAtElmList )
        {
            String name = elmComplex.attributeValue( "name" );

            if ( Strings.isEmpty( name ) )
            {
                throw new IllegalStateException( "name is missing in the multival-attribute configuration element "
                    + elmComplex.asXML() );
            }

            String baseDn = elmComplex.attributeValue( "baseDn" );
            String filter = elmComplex.attributeValue( "filter" );
            
            Element elmAtGroup = elmComplex.element( "at-group" );
            if ( elmAtGroup != null )
            {
                SimpleTypeGroup stg = parseAtGroup( elmAtGroup, uri );
                if ( stg != null )
                {
                    MultiValType ct = new MultiValType( name, uri, stg, baseDn, filter );
                    resourceSchema.addAttributeType( name, ct );
                }

            }
            else
            {
                List<Element> lstElmTypes = elmComplex.elements( "type" );

                List<TypedType> lstTypes = new ArrayList<TypedType>();

                for ( Element elmType : lstElmTypes )
                {
                    Element elmTypeAtGroup = elmType.element( "at-group" );
                    SimpleTypeGroup stg = parseAtGroup( elmTypeAtGroup, uri );

                    String show = elmType.attributeValue( "show" );

                    if ( Strings.isEmpty( show ) )
                    {
                        show = "true";
                    }

                    String primary = elmType.attributeValue( "primary" );

                    if ( Strings.isEmpty( primary ) )
                    {
                        primary = "false";
                    }

                    String typeName = elmType.attributeValue( "name" );

                    if ( Strings.isEmpty( typeName ) )
                    {
                        throw new IllegalArgumentException( "name is missing in the type element " + elmType.asXML() );
                    }

                    TypedType tt = new TypedType( typeName, stg, Boolean.parseBoolean( show ), Boolean.parseBoolean( primary ), uri );
                    lstTypes.add( tt );
                }

                MultiValType ct = new MultiValType( name, uri, lstTypes, baseDn, filter );
                resourceSchema.addAttributeType( name, ct );
            }
        }
    }


    private SimpleTypeGroup parseAtGroup( Element elmAtGroup, String uri )
    {
        SimpleTypeGroup stg = null;

        List<SimpleType> lstSTypes = null;

        if ( elmAtGroup != null )
        {
            lstSTypes = new ArrayList<SimpleType>();

            List<Element> elmSTypes = elmAtGroup.elements( "attribute" );
            for ( Element elmAt : elmSTypes )
            {
                SimpleType st = parseSimpleType( elmAt, uri );
                if ( st != null )
                {
                    lstSTypes.add( st );
                }
            }

            String format = null;

            Element elmFormat = elmAtGroup.element( "formatted" );
            if ( elmFormat != null )
            {
                format = elmFormat.attributeValue( "format" );
            }

            if ( !lstSTypes.isEmpty() )
            {
                stg = new SimpleTypeGroup( lstSTypes, format );
            }
        }

        return stg;
    }


    private SimpleType parseSimpleType( Element el, String uri )
    {
        String name = el.attributeValue( "name" );

        if ( Strings.isEmpty( name ) )
        {
            throw new IllegalStateException( "name is missing in the attribute configuration element " + el.asXML() );
        }

        String mappedTo = el.attributeValue( "mappedTo" );

        if ( Strings.isEmpty( mappedTo ) )
        {
            LOG.debug( "No LDAP attribute was mapped to the SCIM attribute {}, skipping", name );
            return null;
        }

        boolean show = Boolean.parseBoolean( el.attributeValue( "show" ) );

        return new SimpleType( name, mappedTo, uri, show );
    }
    
    public static void main( String[] args )
    {
        LdapSchemaMapper lsm = new LdapSchemaMapper();
        lsm.loadMappings();
    }
}
