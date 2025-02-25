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
package org.xwiki.search.solr.internal.metadata;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link XWikiSolrInputDocument}.
 *
 * @version $Id$
 */
class XWikiSolrInputDocumentTest
{
    protected static final String FIELD_1 = "field1";

    protected static final String VALUE_1 = "value1";

    @Test
    void testAddField()
    {
        XWikiSolrInputDocument doc = new XWikiSolrInputDocument();
        doc.addField(FIELD_1, VALUE_1);
        doc.addField(FIELD_1, VALUE_1);
        assertEquals(List.of(VALUE_1, VALUE_1), doc.getFieldValues(FIELD_1));
    }

    @Test
    void testAddFieldOnce()
    {
        XWikiSolrInputDocument doc = new XWikiSolrInputDocument();
        // Basic test: adding a value twice should only add it once.
        doc.addFieldOnce(FIELD_1, VALUE_1);
        doc.addFieldOnce(FIELD_1, VALUE_1);
        assertEquals(List.of(VALUE_1), doc.getFieldValues(FIELD_1));
        // Adding a different value should add it, and it's unique even when added without the "once" method first.
        String value2 = "value2";
        doc.addField(FIELD_1, value2);
        doc.addFieldOnce(FIELD_1, value2);
        assertEquals(List.of(VALUE_1, value2), doc.getFieldValues(FIELD_1));
        // Setting the field removes the previous values.
        doc.setField(FIELD_1, value2);
        assertEquals(value2, doc.getFieldValue(FIELD_1));
        // Adding a value after setting the field should add it.
        doc.addFieldOnce(FIELD_1, VALUE_1);
        assertEquals(List.of(value2, VALUE_1), doc.getFieldValues(FIELD_1));
        // Removing the field should allow adding the value again.
        doc.removeField(FIELD_1);
        doc.addFieldOnce(FIELD_1, VALUE_1);
        assertEquals(List.of(VALUE_1), doc.getFieldValues(FIELD_1));
        // Adding the value to another field should not affect the first field.
        String field2 = "field2";
        doc.addFieldOnce(field2, VALUE_1);
        assertEquals(List.of(VALUE_1), doc.getFieldValues(FIELD_1));
        assertEquals(List.of(VALUE_1), doc.getFieldValues(field2));
        // Clearing the document should remove all fields.
        doc.clear();
        assertNull(doc.getFieldValue(FIELD_1));
        assertNull(doc.getFieldValue(field2));
        // Adding a value after clearing should add it.
        doc.addFieldOnce(FIELD_1, VALUE_1);
        assertEquals(List.of(VALUE_1), doc.getFieldValues(FIELD_1));
    }

    @Test
    void setField()
    {
        XWikiSolrInputDocument doc = new XWikiSolrInputDocument();
        doc.setField(FIELD_1, VALUE_1);
        assertEquals(VALUE_1.length(), doc.getLength());
        doc.setField(FIELD_1, VALUE_1);
        assertEquals(2 * VALUE_1.length(), doc.getLength());
        doc.setField(FIELD_1, VALUE_1.getBytes());
        assertEquals(3 * VALUE_1.length(), doc.getLength());
    }
}
