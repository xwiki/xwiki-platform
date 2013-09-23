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

import org.xwiki.annotation.rest.model.jaxb.AnnotationField;
import org.xwiki.annotation.rest.model.jaxb.AnnotationUpdateRequest;
import org.xwiki.annotation.rest.model.jaxb.ObjectFactory;

/**
 * Partial implementation of a reader that reads an annotation update request, extending the request reader with the
 * read of the annotation fields.
 * 
 * @param <T> the type read from the url encoded form
 * @version $Id$
 * @since 2.3M1
 */
public abstract class AbstractFormUrlEncodedAnnotationUpdateRequestReader<T extends AnnotationUpdateRequest> extends
    AbstractFormUrlEncodedAnnotationRequestReader<T>
{
    @Override
    protected boolean saveField(T readObject, String key, String value, ObjectFactory objectFactory)
    {
        // first try to pass this through the super reader, to read the specific request fields
        boolean read = super.saveField(readObject, key, value, objectFactory);
        // if the field was not read then it's a custom field, store it as an annotation extra field
        if (!read) {
            // use xwiki convention that first value is always the one used and ignore a field if it has already been
            // read
            boolean contains = false;
            for (AnnotationField readField : readObject.getAnnotation().getFields()) {
                if (readField.getName().equals(key)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                AnnotationField extraField = objectFactory.createAnnotationField();
                extraField.setName(key);
                extraField.setValue(value);
                readObject.getAnnotation().getFields().add(extraField);
            }
        }
        // always return true since this will always be able to store the extra fields
        return true;
    }
}
