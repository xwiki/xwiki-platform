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
package org.xwiki.rest.internal.representations.tags;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.rest.model.jaxb.Tag;
import org.xwiki.rest.model.jaxb.Tags;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link TextPlainTagsReader}.
 *
 * @version $Id$
 */
class TextPlainTagsReaderTest
{
    private final TextPlainTagsReader reader = new TextPlainTagsReader();

    private InputStream utf8(String value)
    {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void splitsOnSpacesCommasPipesAndNewlines() throws Exception
    {
        Tags tags = this.reader.readFrom(Tags.class, null, null, null, null, utf8("a b,c|d\ne"));

        List<String> names =
            tags.getTags().stream().map(Tag::getName).toList();
        assertEquals(List.of("a", "b", "c", "d", "e"), names);
    }

    @Test
    void emptyBodyYieldsSingleEmptyTag() throws Exception
    {
        // Pin current (quirky) behavior: an empty body still produces one Tag whose name is the
        // empty string, because String#split("") returns a single-element array for an empty
        // input when the separator pattern does not match the empty string.
        Tags tags = this.reader.readFrom(Tags.class, null, null, null, null, utf8(""));

        assertEquals(1, tags.getTags().size());
        assertEquals("", tags.getTags().get(0).getName());
    }
}
