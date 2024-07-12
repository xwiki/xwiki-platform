/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rest.internal.representations.objects;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.JAXRSUtils;
import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.Property;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.representations.objects.FormUrlEncodedPropertyReader")
@Provider
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Singleton
public class FormUrlEncodedPropertyReader implements MessageBodyReader<Property>, XWikiRestComponent
{
    private static final String PROPERTY_PREFIX = "property#";

    @Inject
    private JAXRSUtils jaxrs;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return Property.class.isAssignableFrom(type);
    }

    @Override
    public Property readFrom(Class<Property> type, Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException
    {
        MultivaluedMap<String, String> form = this.jaxrs.readForm(mediaType, annotations, entityStream);

        ObjectFactory objectFactory = new ObjectFactory();
        Property property = objectFactory.createProperty();

        for (String name : form.keySet()) {
            if (name.startsWith(PROPERTY_PREFIX)) {
                property.setName(name.replace(PROPERTY_PREFIX, ""));
                property.setValue(form.getFirst(name));
                break;
            }
        }

        return property;
    }
}
