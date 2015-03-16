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
package org.xwiki.rest.internal.representations.pages;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.Constants;
import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.Page;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.representations.pages.FormUrlEncodedPageReader")
@Provider
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Singleton
public class FormUrlEncodedPageReader implements MessageBodyReader<Page>, XWikiRestComponent
{
    private static String TITLE_FIELD_NAME = "title";

    private static String PARENT_FIELD_NAME = "parent";

    private static String CONTENT_FIELD_NAME = "content";

    @Override
    public boolean isReadable(Class< ? > type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return Page.class.isAssignableFrom(type);
    }

    @Override
    public Page readFrom(Class<Page> type, Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
        WebApplicationException
    {
        ObjectFactory objectFactory = new ObjectFactory();
        Page page = objectFactory.createPage();

        HttpServletRequest httpServletRequest =
            (HttpServletRequest) Context.getCurrent().getAttributes().get(Constants.HTTP_REQUEST);

        Representation representation =
            new InputRepresentation(entityStream, org.restlet.data.MediaType.APPLICATION_WWW_FORM);
        Form form = new Form(representation);

        /*
         * If the form is empty then it might have happened that some filter has invalidated the entity stream. Try to
         * read data using getParameter()
         */
        if (form.getNames().isEmpty()) {
            page.setTitle(httpServletRequest.getParameter(TITLE_FIELD_NAME));
            page.setParent(httpServletRequest.getParameter(PARENT_FIELD_NAME));
            page.setContent(httpServletRequest.getParameter(CONTENT_FIELD_NAME));
        } else {
            page.setTitle(form.getFirstValue(TITLE_FIELD_NAME));
            page.setParent(form.getFirstValue(PARENT_FIELD_NAME));
            page.setContent(form.getFirstValue(CONTENT_FIELD_NAME));
        }

        return page;
    }

}
