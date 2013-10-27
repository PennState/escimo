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
package org.apache.directory.scim.ldap.handlers;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.scim.MultiValAttribute;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.ServerResource;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.SimpleAttributeGroup;
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
 * TODO PhotosAttributeHandler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PhotosAttributeHandler extends LdapAttributeHandler
{

    private static final Logger LOG = LoggerFactory.getLogger( PhotosAttributeHandler.class );


    @Override
    public void read( BaseType bt, Object srcResource, RequestContext ctx ) throws Exception
    {
        checkHandler( bt, "photos", this );

        ServerResource user = ctx.getCoreResource();

        Entry entry = ( Entry ) srcResource;

        MultiValAttribute mv = null;

        MultiValType mt = ( MultiValType ) bt;

        SimpleTypeGroup stg = mt.getAtGroup();

        String photoUrlBase = ctx.getUriInfo().getBaseUri().toString();
        photoUrlBase += "Users/photo?atName=%s&id=%s";

        if ( stg != null )
        {
            SimpleAttribute sa = getPhotoUrlValue( stg, entry, photoUrlBase, user );
            if ( sa != null )
            {
                SimpleAttributeGroup sg = new SimpleAttributeGroup();
                sg.addAttribute( sa );
                mv = new MultiValAttribute( bt.getName() );
                mv.addAtGroup( sg );
            }
        }

        if ( mv != null )
        {
            user.addAttribute( bt.getUri(), mv );
        }
    }


    private SimpleAttribute getPhotoUrlValue( SimpleTypeGroup stg, Entry entry, String photoUrlBase, ServerResource user ) throws Exception
    {
        SimpleType valType = stg.getValueType();
        if ( valType != null )
        {
            Attribute photoAt = entry.get( valType.getMappedTo() );
            if ( photoAt != null )
            {
                Value<?> val = photoAt.get();
                String url = formatPhotoUrl( photoUrlBase, photoAt.getId(), user.getId(), val.getBytes() );
                SimpleAttribute sa = new SimpleAttribute( valType.getName(), url );
                return sa;
            }
        }

        return null;
    }


    private String formatPhotoUrl( String url, String atName, String userId, byte[] photoBytes ) throws Exception
    {
        String enc = "UTF-8";

        atName = URLEncoder.encode( atName, enc );
        userId = URLEncoder.encode( userId, enc );
        
        return String.format( url, atName, userId );
    }


    @Override
    public void write( BaseType atType, JsonElement jsonData, Object targetEntry, RequestContext ctx ) throws Exception
    {
        checkHandler( atType, "photos", this );
        
        MultiValType mt = ( MultiValType ) atType;
        
        SimpleType st = mt.getAtGroup().getValueType();
        
        JsonArray photos = ( JsonArray ) jsonData;
        
        Entry entry = ( Entry ) targetEntry;
        
        Attribute ldapAt = entry.get( st.getMappedTo() );
        
        // fetch the URL and insert the photo
        for( JsonElement je : photos )
        {
            String url = null;
            
            // for the cases where multivalued attribute comes as an array of primitives
            // e.x "photos":['http://example.com/p1', 'http://example.com/p2']
            if( je.isJsonPrimitive() )
            {
                url = je.getAsString();
            }
            else
            {
                JsonObject jo = ( JsonObject ) je;
                url = jo.get( "value" ).getAsString();
            }
            
            byte[] data = fetchPhoto( url );
            
            
            if( ldapAt == null )
            {
                ldapAt = new DefaultAttribute( st.getMappedTo(), data );
                entry.add( ldapAt );
            }
            else
            {
                ldapAt.add( data );
            }
        }
    }

    
    private byte[] fetchPhoto( String url ) throws IOException
    {
        InputStream in = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        try
        {
            in = new URL( url ).openStream();
            byte[] buf = new byte[1024];
            
            while( true )
            {
                int read = in.read( buf );
                
                if( read <= 0 )
                {
                    break;
                }
                
                bout.write( buf, 0, read );
            }
            
            return bout.toByteArray();
        }
        finally
        {
            if( in != null )
            {
                in.close();
            }

            bout.close();
        }
    }
}
