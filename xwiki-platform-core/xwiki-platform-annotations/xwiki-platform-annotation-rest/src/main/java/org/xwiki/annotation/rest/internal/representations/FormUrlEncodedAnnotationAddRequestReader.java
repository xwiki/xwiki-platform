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
package org.xwiki.annotation.rest.internal.representations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.xwiki.annotation.rest.model.jaxb.AnnotationAddRequest;
import org.xwiki.annotation.rest.model.jaxb.AnnotationRequest;
import org.xwiki.annotation.rest.model.jaxb.ObjectFactory;
import org.xwiki.component.annotation.Component;

/**
 * Implementation of the form url encoded reader for the annotation creation requests.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Named("org.xwiki.annotation.rest.internal.representations.FormUrlEncodedAnnotationAddRequestReader")
@Provider
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Singleton
public class FormUrlEncodedAnnotationAddRequestReader extends
    AbstractFormUrlEncodedAnnotationUpdateRequestReader<AnnotationAddRequest>
{
    // TODO: should send selection, contextLeft and contextRight
    /**
     * Name of the selection context field submitted by a form.
     */
    private static final String SELECTION_CONTEXT_FIELD_NAME = "selectionContext";

    /**
     * Name of the field holding the selection offset in context submitted by a form.
     */
    private static final String SELECTION_OFFSET_FIELD_NAME = "selectionOffset";

    /**
     * Name of the selection context field submitted by a form.
     */
    private static final String SELECTION_FIELD_NAME = "selection";

    @Override
    public boolean isReadable(Class< ? > type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        // this reader will only read annotationAddRequests, and none of the superclasses. Superclasses will read
        // themselves
        return type.equals(AnnotationAddRequest.class);
    }

    @Override
    protected AnnotationAddRequest getReadObjectInstance(ObjectFactory factory)
    {
        AnnotationAddRequest addRequest = factory.createAnnotationAddRequest();
        // and initialize it
        addRequest.setRequest(new AnnotationRequest.Request());
        addRequest.setFilter(factory.createAnnotationFieldCollection());
        addRequest.setAnnotation(factory.createAnnotationFieldCollection());
        
        return addRequest;
    }

    @Override
    protected boolean saveField(AnnotationAddRequest annotationAddRequest, String key, String value,
        ObjectFactory objectFactory)
    {
        // check this key against the 'known fields'
        if (SELECTION_FIELD_NAME.equals(key)) {
            annotationAddRequest.setSelection(value);
            return true;
        }
        if (SELECTION_CONTEXT_FIELD_NAME.equals(key)) {
            annotationAddRequest.setSelectionContext(value);
            return true;
        }
        if (SELECTION_OFFSET_FIELD_NAME.equals(key)) {
            // use the parameter as a string and parse it as an integer
            int offset = 0;
            try {
                offset = Integer.parseInt(value);
            } catch (NumberFormatException exc) {
                // nothing, will leave the 0 value
            }
            annotationAddRequest.setSelectionOffset(offset);
            return true;
        }
        // if none matched, handle as an annotation update field
        return super.saveField(annotationAddRequest, key, value, objectFactory);
    }
}
