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
package org.xwiki.ckeditor.test.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

/**
 * Tests how CKEditor filters the content.
 * 
 * @version $Id$
 */
@UITest
class FilterIT extends AbstractCKEditorIT
{
    @BeforeEach
    void beforeEach(TestUtils setup, TestReference testReference)
    {
        edit(setup, testReference);
    }

    @AfterEach
    void afterEach(TestUtils setup, TestReference testReference)
    {
        maybeLeaveEditMode(setup, testReference);
    }

    @Test
    @Order(1)
    void escapeStyleContent()
    {
        StringBuilder source = new StringBuilder();
        source.append("before\n");
        source.append("\n");
        source.append("{{html clean=\"false\"}}\n");
        source.append("<style>\n");
        source.append("p::before {\n");
        source.append("  content: '<';\n");
        source.append("}\n");
        source.append("</style>\n");
        source.append("<div>inside</div>\n");
        source.append("<style id=\"second\">\n");
        source.append("div::after {\n");
        source.append("  content: '<';\n");
        source.append("}\n");
        source.append("</style>\n");
        source.append("{{/html}}\n");
        source.append("\n");
        source.append("after");

        setSource(source.toString());
        String html = this.textArea.getContent();
        assertTrue(html.contains("<style>"), "Unexpected content: " + html);
        assertTrue(html.contains("content: '<'"), "Unexpected content: " + html);

        this.textArea.sendKeys(" end");
        // Verify that the origial style content is preserved.
        assertSourceContains(source.toString() + " end");
    }
}
