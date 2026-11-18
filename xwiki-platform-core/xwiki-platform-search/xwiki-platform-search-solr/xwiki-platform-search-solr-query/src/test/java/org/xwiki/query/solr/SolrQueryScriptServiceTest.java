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
package org.xwiki.query.solr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.properties.ConverterManager;
import org.xwiki.query.solr.internal.SafeSolrQueryValidator;
import org.xwiki.search.solr.internal.DefaultSolrUtils;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SolrQueryScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ DefaultSolrUtils.class, SafeSolrQueryValidator.class })
class SolrQueryScriptServiceTest
{
    @InjectMockComponents
    private SolrQueryScriptService scriptService;

    /**
     * Converter needed for DefaultSolrUtils.
     */
    @MockComponent
    private ConverterManager converterManager;

    @Test
    void escape()
    {
        assertEquals("My\\ \\\"Space\\\"\\:\\{\\!term\\}", this.scriptService.escape("My \"Space\":{!term}"));
        assertEquals("a\\\\b\\*", this.scriptService.escape("a\\b*"));
        assertEquals("cats\\ \\AND\\ dogs", this.scriptService.escape("cats AND dogs"));
        assertEquals("\"\"", this.scriptService.escape(""));
        assertEquals("42", this.scriptService.escape(42));
        assertNull(this.scriptService.escape(null));
    }

    @Test
    void quote()
    {
        assertEquals("\"My\\ \\\"Space\\\"\\:\\{\\!term\\}\"", this.scriptService.quote("My \"Space\":{!term}"));
        assertEquals("\"a\\\\b\\\\\\\"\"", this.scriptService.quote("a\\b\\\""));
        assertEquals("\"$\\{q\\}\"", this.scriptService.quote("${q}"));
        assertEquals("\"line\\\nbreak\"", this.scriptService.quote("line\nbreak"));
        assertEquals("\"\"", this.scriptService.quote(""));
        assertNull(this.scriptService.quote(null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "name", "title_sort", "space_exact", "property.My$20Space.Été.prop_sortString",
        "property.My-App.Code.TagClass.tags_string" })
    void fieldName(String name)
    {
        assertTrue(this.scriptService.isFieldName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "some field", "$qf", "sum(1,2)", "[docid]", "name asc", "_query_:x", "1", "_query_", "_val_" })
    void notFieldName(String name)
    {
        assertFalse(this.scriptService.isFieldName(name));
    }

    @Test
    void nullFieldName()
    {
        assertFalse(this.scriptService.isFieldName(null));
    }
}
