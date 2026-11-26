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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.converter.Converter;
import org.xwiki.records.macro.RecordsTemplateProvider;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RecordsTemplateProviderConverter}.
 *
 * @version $Id$
 */
@ComponentTest
class RecordsTemplateProviderConverterTest
{
    private static final DocumentReference PROVIDER =
        new DocumentReference("xwiki", "Clients", "ProjectTemplateProvider");

    @InjectMockComponents
    private RecordsTemplateProviderConverter converter;

    @MockComponent
    private Converter<DocumentReference> documentReferenceConverter;

    @Test
    void convertFromString()
    {
        when(this.documentReferenceConverter.<DocumentReference>convert(DocumentReference.class,
            "Clients.ProjectTemplateProvider")).thenReturn(PROVIDER);

        Object templateProvider =
            this.converter.convert(RecordsTemplateProvider.class, "Clients.ProjectTemplateProvider");

        assertInstanceOf(RecordsTemplateProvider.class, templateProvider);
        assertEquals(PROVIDER, templateProvider);
    }

    @Test
    void convertNoValue()
    {
        assertNull(this.converter.convert(RecordsTemplateProvider.class, null));
    }

    @Test
    void convertToString()
    {
        RecordsTemplateProvider templateProvider = new RecordsTemplateProvider(PROVIDER);
        when(this.documentReferenceConverter.<String>convert(String.class, templateProvider))
            .thenReturn("xwiki:Clients.ProjectTemplateProvider");

        assertEquals("xwiki:Clients.ProjectTemplateProvider", this.converter.convert(String.class, templateProvider));
    }
}
