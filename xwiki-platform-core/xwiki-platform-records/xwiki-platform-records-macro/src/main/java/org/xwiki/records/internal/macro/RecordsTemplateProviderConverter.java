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
package org.xwiki.records.internal.macro;

import java.lang.reflect.Type;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.Converter;
import org.xwiki.records.macro.RecordsTemplateProvider;

/**
 * {@link Converter} for {@link RecordsTemplateProvider}, which converts the value as a {@link DocumentReference}.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Singleton
public class RecordsTemplateProviderConverter extends AbstractConverter<RecordsTemplateProvider>
{
    @Inject
    private Converter<DocumentReference> documentReferenceConverter;

    @Override
    protected RecordsTemplateProvider convertToType(Type type, Object value)
    {
        DocumentReference document = this.documentReferenceConverter.convert(DocumentReference.class, value);
        return document == null ? null : new RecordsTemplateProvider(document);
    }

    @Override
    protected String convertToString(RecordsTemplateProvider value)
    {
        return this.documentReferenceConverter.convert(String.class, value);
    }
}
