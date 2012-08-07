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
package org.xwiki.rest.representations.wikis;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

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
import org.xwiki.rest.model.jaxb.WikiDescriptor;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.representations.wikis.FormUrlEncodedWikiDescriptorReader")
@Provider
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class FormUrlEncodedWikiDescriptorReader implements MessageBodyReader<WikiDescriptor>, XWikiRestComponent
{
    /**
     * Id field name in URL encoded form data.
     */
    private static final String ID_FIELD_NAME = "id";

    /**
     * Owner field name in URL encoded form data.
     */
    private static final String OWNER_FIELD_NAME = "owner";

    /**
     * Pretty name field name in URL encoded form data.
     */
    private static final String PRETTYNAME_FIELD_NAME = "prettyName";

    /**
     * Description field name in URL encoded form data.
     */
    private static final String DESCRIPTION_FIELD_NAME = "description";

    /**
     * Template field name in URL encoded form data.
     */
    private static final String TEMPLATE_FIELD_NAME = "template";

    @Override
    public boolean isReadable(Class< ? > type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return WikiDescriptor.class.isAssignableFrom(type);
    }

    @Override
    public WikiDescriptor readFrom(Class<WikiDescriptor> type, Type genericType, Annotation[] annotations,
        MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
        WebApplicationException
    {
        ObjectFactory objectFactory = new ObjectFactory();
        WikiDescriptor wikiDescriptor = objectFactory.createWikiDescriptor();

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
            wikiDescriptor.setId(httpServletRequest.getParameter(ID_FIELD_NAME));
            wikiDescriptor.setOwner(httpServletRequest.getParameter(OWNER_FIELD_NAME));            
            wikiDescriptor.setPrettyName(httpServletRequest.getParameter(PRETTYNAME_FIELD_NAME));
            wikiDescriptor.setDescription(httpServletRequest.getParameter(DESCRIPTION_FIELD_NAME));
            wikiDescriptor.setTemplate(httpServletRequest.getParameter(TEMPLATE_FIELD_NAME));
        } else {
            wikiDescriptor.setId(form.getFirstValue(ID_FIELD_NAME));
            wikiDescriptor.setOwner(form.getFirstValue(OWNER_FIELD_NAME));            
            wikiDescriptor.setPrettyName(form.getFirstValue(PRETTYNAME_FIELD_NAME));
            wikiDescriptor.setDescription(form.getFirstValue(DESCRIPTION_FIELD_NAME));
            wikiDescriptor.setTemplate(form.getFirstValue(TEMPLATE_FIELD_NAME));
        }

        return wikiDescriptor;
    }

}
