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
package org.xwiki.search.solr;

import java.util.List;

import org.apache.lucene.index.IndexableField;
import org.apache.solr.schema.SchemaField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LowerCaseStrField}.
 *
 * @version $Id$
 */
class LowerCaseStrFieldTest
{
    private LowerCaseStrField field;

    private SchemaField schemaField;

    @BeforeEach
    void setUp()
    {
        this.field = new LowerCaseStrField();
        this.schemaField = mock();
        when(this.schemaField.hasDocValues()).thenReturn(true);
        when(this.schemaField.stored()).thenReturn(true);
        when(this.schemaField.indexed()).thenReturn(true);
        when(this.schemaField.getName()).thenReturn("testField");
        when(this.schemaField.getType()).thenReturn(this.field);
    }

    @Test
    void createFieldsTransformsStringValueToLowerCase()
    {
        Object value = "TeStVaLuE";

        List<IndexableField> fields = this.field.createFields(this.schemaField, value);

        assertEquals("testvalue", fields.get(0).stringValue());
    }

    @Test
    void createFieldsHandlesNonStringValuesWithoutTransformation()
    {
        Object value = 12345;

        List<IndexableField> fields = this.field.createFields(this.schemaField, value);

        assertEquals("12345", fields.get(0).stringValue());
    }

    @Test
    void toInternalConvertsStringToLowerCase()
    {
        String result = this.field.toInternal("TeStVaLuE");

        assertEquals("testvalue", result);
    }

    @Test
    void toInternalHandlesEmptyString()
    {
        String result = this.field.toInternal("");

        assertEquals("", result);
    }

    @Test
    void toInternalHandlesNullValue()
    {
        assertNull(this.field.toInternal(null));
    }
}
