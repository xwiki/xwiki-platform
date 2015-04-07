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

import org.xwiki.annotation.rest.model.jaxb.AnnotationRequest;
import org.xwiki.annotation.rest.model.jaxb.AnnotationUpdateRequest;
import org.xwiki.annotation.rest.model.jaxb.ObjectFactory;
import org.xwiki.component.annotation.Component;

/**
 * Implementation of the form url encoded reader for the annotation update requests.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Named("org.xwiki.annotation.rest.internal.representations.FormUrlEncodedAnnotationUpdateRequestReader")
@Provider
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Singleton
public class FormUrlEncodedAnnotationUpdateRequestReader extends
    AbstractFormUrlEncodedAnnotationUpdateRequestReader<AnnotationUpdateRequest>
{
    @Override
    protected AnnotationUpdateRequest getReadObjectInstance(ObjectFactory factory)
    {
        AnnotationUpdateRequest updateRequest = factory.createAnnotationUpdateRequest();
        updateRequest.setRequest(new AnnotationRequest.Request());
        updateRequest.setFilter(factory.createAnnotationFieldCollection());
        updateRequest.setAnnotation(factory.createAnnotationFieldCollection());
        
        return updateRequest;
    }

    @Override
    public boolean isReadable(Class< ? > type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        // this reader will only read annotationAddRequests, and none of the superclasses. Superclasses will read
        // themselves
        return type.equals(AnnotationUpdateRequest.class);
    }
}
