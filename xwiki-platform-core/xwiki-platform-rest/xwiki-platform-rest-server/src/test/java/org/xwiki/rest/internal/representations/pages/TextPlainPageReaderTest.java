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
package org.xwiki.rest.internal.representations.pages;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.xwiki.rest.model.jaxb.Page;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link TextPlainPageReader}.
 *
 * @version $Id$
 */
class TextPlainPageReaderTest
{
    private final TextPlainPageReader reader = new TextPlainPageReader();

    private InputStream utf8(String value)
    {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsAsciiContent() throws Exception
    {
        Page page = this.reader.readFrom(Page.class, null, null, null, null, utf8("some content"));

        assertEquals("some content", page.getContent());
    }

    @Test
    void readsUtf8ContentWithoutCorruption() throws Exception
    {
        String content = "café — 日本語 — €";

        Page page = this.reader.readFrom(Page.class, null, null, null, null, utf8(content));

        assertEquals(content, page.getContent());
    }

    @Test
    void nullStreamYieldsEmptyContent() throws Exception
    {
        Page page = this.reader.readFrom(Page.class, null, null, null, null, null);

        assertEquals("", page.getContent());
    }
}
