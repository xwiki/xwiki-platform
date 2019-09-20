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
package com.xpn.xwiki.internal.converter;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Converter for {@link Document}.
 *
 * @version $Id$
 * @since 11.8RC1
 */
@Component
@Singleton
public class DocumentConverter extends AbstractConverter<Document>
{
    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public <G> G convert(Type targetType, Object sourceValue)
    {
        if (sourceValue == null) {
            return null;
        }
        if (sourceValue instanceof Document) {
            return convertFromType(targetType, sourceValue);
        } else {
            return super.convert(targetType, sourceValue);
        }
    }

    private <G> G convertFromType(Type targetType, Object sourceValue)
    {
        if (targetType.equals(String.class)) {
            return (G) convertToString((Document) sourceValue);
        } else if (EntityReference.class.isAssignableFrom(ReflectionUtils.getTypeClass(targetType))) {
            return (G) convertToEntityReference((Document) sourceValue);
        } else if (targetType == XWikiDocument.class) {
            return (G) convertToXWikiDocument((Document) sourceValue);
        } else {
            throw new ConversionException(String.format("Unsupported target type [%s]", targetType));
        }
    }

    private EntityReference convertToEntityReference(Document document)
    {
        return document.getDocumentReference();
    }

    private XWikiDocument convertToXWikiDocument(Document document)
    {
        return document.getDocument();
    }

    @Override
    protected String convertToString(Document value)
    {
        return this.serializer.serialize(value.getDocumentReference());
    }
}
