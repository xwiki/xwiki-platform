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
package org.xwiki.rendering.internal.macro.rss;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.xml.html.DefaultHTMLCleanerComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the {@link FeedItemContentCleaner}.
 *
 * @version $Id$
 * @since 14.6RC1
 */
@ComponentTest
@DefaultHTMLCleanerComponentList
class FeedItemContentCleanerTest
{
    @InjectMockComponents
    private FeedItemContentCleaner contentCleaner;

    @Test
    void linkTargetScript()
    {
        assertEquals(new RawBlock("<p><a>XSS</a></p>", Syntax.HTML_5_0),
            this.contentCleaner.cleanContent("<a href=\"javascript:alert(1)\">XSS</a>"));
    }

    @Test
    void iframe()
    {
        assertEquals(new RawBlock("<p></p>", Syntax.HTML_5_0),
            this.contentCleaner.cleanContent(
                "<iframe src=\"https://www.xwiki.org\" width=\"200\" height=\"400\">Iframe</iframe>")
        );
    }
}
