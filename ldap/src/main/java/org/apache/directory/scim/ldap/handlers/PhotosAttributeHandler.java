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


import java.net.URLEncoder;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.scim.AttributeHandler;
import org.apache.directory.scim.CoreResource;
import org.apache.directory.scim.MultiValAttribute;
import org.apache.directory.scim.RequestContext;
import org.apache.directory.scim.SimpleAttribute;
import org.apache.directory.scim.SimpleAttributeGroup;
import org.apache.directory.scim.User;
import org.apache.directory.scim.ldap.schema.MultiValType;
import org.apache.directory.scim.ldap.schema.SimpleType;
import org.apache.directory.scim.ldap.schema.SimpleTypeGroup;
import org.apache.directory.scim.ldap.schema.TypedType;
import org.apache.directory.scim.schema.BaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO PhotosAttributeHandler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PhotosAttributeHandler implements AttributeHandler
{

    private static final Logger LOG = LoggerFactory.getLogger( PhotosAttributeHandler.class );


    @Override
    public void handle( BaseType bt, Object srcResource, RequestContext ctx )
    {
        if ( !bt.getName().equals( "photos" ) )
        {
            LOG.debug( "PhotosAttributeHandler can  only be called on the photos attribute, invalid attribute name {}",
                bt.getName() );
            return;
        }

        CoreResource user = ctx.getCoreResource();

        Entry entry = ( Entry ) srcResource;

        MultiValAttribute mv = null;

        MultiValType mt = ( MultiValType ) bt;

        SimpleTypeGroup stg = mt.getStGroup();

        List<TypedType> ttList = mt.getTypedList();

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
        else if ( ttList != null )
        {
            for ( TypedType tt : ttList )
            {
                SimpleTypeGroup typeStg = tt.getAtGroup();
                SimpleAttribute sa = getPhotoUrlValue( typeStg, entry, photoUrlBase, user );

                if ( sa != null )
                {
                    SimpleAttributeGroup sg = new SimpleAttributeGroup();
                    sg.addAttribute( sa );

                    SimpleAttribute atType = new SimpleAttribute( "type", tt.getName() );
                    sg.addAttribute( atType );

                    if ( tt.isPrimary() )
                    {
                        SimpleAttribute atPrimary = new SimpleAttribute( "primary", true );
                        sg.addAttribute( atPrimary );
                    }

                    if ( mv == null )
                    {
                        mv = new MultiValAttribute( bt.getName() );
                    }

                    mv.addAtGroup( sg );
                }
            }
        }

        if ( mv != null )
        {
            user.addAttribute( bt.getUri(), mv );
        }
    }


    private SimpleAttribute getPhotoUrlValue( SimpleTypeGroup stg, Entry entry, String photoUrlBase, CoreResource user )
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


    private String formatPhotoUrl( String url, String atName, String userId, byte[] photoBytes )
    {
        String enc = "UTF-8";
        try
        {
            atName = URLEncoder.encode( atName, enc );
            userId = URLEncoder.encode( userId, enc );

            return String.format( url, atName, userId );
        }
        catch ( Exception e )
        {
            // if happens blow up 
            throw new RuntimeException( e );
        }
    }

}
