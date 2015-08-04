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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.converter.AbstractConverter;

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
    private EntityReferenceSerializer<String> serialier;

    @Override
    protected DocumentReference convertToType(Type type, Object value)
    {
        if (value == null) {
            return null;
        }

        DocumentReference reference;

        if (value instanceof EntityReference) {
            reference = this.referenceResolver.resolve((EntityReference) value);
        } else {
            reference = this.stringResolver.resolve(value.toString());
        }

        return reference;
    }

    @Override
    protected String convertToString(DocumentReference value)
    {
        if (value == null) {
            return null;
        }

        return this.serialier.serialize(value);
    }
}
