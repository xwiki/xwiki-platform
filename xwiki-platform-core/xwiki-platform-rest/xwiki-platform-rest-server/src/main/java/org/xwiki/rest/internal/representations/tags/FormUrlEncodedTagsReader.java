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
package org.xwiki.rest.internal.representations.tags;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.regex.Pattern;

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
import org.xwiki.rest.model.jaxb.Tag;
import org.xwiki.rest.model.jaxb.Tags;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.representations.tags.FormUrlEncodedTagsReader")
@Provider
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Singleton
public class FormUrlEncodedTagsReader implements MessageBodyReader<Tags>, XWikiRestComponent
{
    private static final String TAGS_FIELD_NAME = "tags";

    private static final String TAG_FIELD_NAME = "tag";

    private static final Pattern TAG_SEPARATORS = Pattern.compile(" |,|\\|");

    @Inject
    private JAXRSUtils jaxrs;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return Tags.class.isAssignableFrom(type);
    }

    @Override
    public Tags readFrom(Class<Tags> type, Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException
    {
        MultivaluedMap<String, String> form = this.jaxrs.readForm(mediaType, annotations, entityStream);

        ObjectFactory objectFactory = new ObjectFactory();
        Tags tags = objectFactory.createTags();

        String text = form.getFirst(TAGS_FIELD_NAME);
        if (text != null) {
            String[] tagNames = TAG_SEPARATORS.split(text);

            for (String tagName : tagNames) {
                Tag tag = objectFactory.createTag();
                tag.setName(tagName);
                tags.getTags().add(tag);
            }
        }

        List<String> tagNames = form.get(TAG_FIELD_NAME);
        if (tagNames != null) {
            for (String tagName : tagNames) {
                Tag tag = objectFactory.createTag();
                tag.setName(tagName);
                tags.getTags().add(tag);
            }
        }

        return tags;
    }

}
