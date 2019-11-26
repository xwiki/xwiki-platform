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
package com.xpn.xwiki.internal.model.reference;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.properties.converter.Converter;

/**
 * Converter that converts a value into a {@link DocumentReference} object. Relative references are resolved using
 * "current" {@link DocumentReferenceResolver}.
 * 
 * @version $Id$
 * @since 5.2RC1
 */
@Component
@Singleton
public class DocumentReferenceConverter extends AbstractConverter<DocumentReference>
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringResolver;

    @Inject
    @Named("currentgetdocument")
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private ConverterManager converterManager;

    @Inject
    private Logger logger;

    @Override
    protected DocumentReference convertToType(Type type, Object value)
    {
        DocumentReference result;
        if (value == null) {
            result = null;
        } else if (value instanceof EntityReference) {
            result = this.referenceResolver.resolve((EntityReference) value);
        }  else {
            Converter<Object> converter = this.converterManager.getConverter(value.getClass());

            if (converter != null) {
                try {
                    result = converter.convert(DocumentReference.class, value);
                } catch (ConversionException e) {
                    logger.warn("The type [{}] cannot be converted natively to DocumentReference, "
                        + "falling back on using toString to convert it.", value.getClass().getName());
                    result = this.stringResolver.resolve(value.toString());
                }
            } else {
                result = this.stringResolver.resolve(value.toString());
            }
        }

        return result;
    }

    @Override
    protected String convertToString(DocumentReference value)
    {
        if (value == null) {
            return null;
        }

        return this.serializer.serialize(value);
    }
}
