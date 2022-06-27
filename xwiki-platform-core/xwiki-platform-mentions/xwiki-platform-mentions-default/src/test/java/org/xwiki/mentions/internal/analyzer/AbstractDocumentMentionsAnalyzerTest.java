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
package org.xwiki.mentions.internal.analyzer;

import org.junit.jupiter.api.Test;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.rendering.block.MacroBlock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of {@link AbstractDocumentMentionsAnalyzer}.
 *
 * @version $Id$
 * @since 13.10.7
 * @since 14.4.2
 * @since 14.5
 */
class AbstractDocumentMentionsAnalyzerTest
{
    private final AbstractDocumentMentionsAnalyzer analyzer = new AbstractDocumentMentionsAnalyzer()
    {
    };

    @Test
    void findDisplayStyle()
    {
        MacroBlock mention0 = new MacroBlock("mention", emptyMap(), true);
        MacroBlock mention1 = new MacroBlock("mention", emptyMap(), true);
        mention1.setParameter("reference", "test");
        MacroBlock mention2 = new MacroBlock("mention", emptyMap(), true);
        mention2.setParameter("reference", "test");
        mention2.setParameter("anchor", "anchor");
        mention2.setParameter("style", "LOGIN");
        DisplayStyle actual = this.analyzer.findDisplayStyle(asList(mention0, mention1, mention2), "test", "anchor");
        assertEquals(DisplayStyle.LOGIN, actual);
    }

    @Test
    void findDisplayStyleStyleMissing()
    {
        MacroBlock mention0 = new MacroBlock("mention", emptyMap(), true);
        MacroBlock mention1 = new MacroBlock("mention", emptyMap(), true);
        mention1.setParameter("reference", "test");
        MacroBlock mention2 = new MacroBlock("mention", emptyMap(), true);
        mention2.setParameter("reference", "test");
        mention2.setParameter("anchor", "anchor");
        DisplayStyle actual = this.analyzer.findDisplayStyle(asList(mention0, mention1, mention2), "test", "anchor");
        assertEquals(DisplayStyle.FULL_NAME, actual);
    }

    @Test
    void findDisplayStyleStyleUnknown()
    {
        MacroBlock mention0 = new MacroBlock("mention", emptyMap(), true);
        MacroBlock mention1 = new MacroBlock("mention", emptyMap(), true);
        mention1.setParameter("reference", "test");
        MacroBlock mention2 = new MacroBlock("mention", emptyMap(), true);
        mention2.setParameter("reference", "test");
        mention2.setParameter("anchor", "anchor");
        DisplayStyle actual = this.analyzer.findDisplayStyle(asList(mention0, mention1, mention2), "test", "anchor");
        assertEquals(DisplayStyle.FULL_NAME, actual);
    }
}
