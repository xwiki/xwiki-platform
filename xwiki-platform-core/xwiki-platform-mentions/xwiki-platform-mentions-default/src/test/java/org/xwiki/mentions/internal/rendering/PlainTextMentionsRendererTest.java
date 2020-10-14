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
package org.xwiki.mentions.internal.rendering;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.mentions.internal.MentionFormatterProvider;
import org.xwiki.mentions.MentionsFormatter;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.mentions.DisplayStyle.FULL_NAME;

/**
 * Test if {@link PlainTextMentionsRenderer}.
 *
 * @version $Id$
 * @since 12.6
 */
@ComponentTest
class PlainTextMentionsRendererTest
{
    @InjectMockComponents
    private PlainTextMentionsRenderer renderer;

    @MockComponent
    private MentionFormatterProvider mentionFormatterProvider;

    private MentionsFormatter formatter;

    @BeforeEach
    void setUp()
    {
        this.formatter = mock(MentionsFormatter.class);
        when(this.mentionFormatterProvider.get(null)).thenReturn(this.formatter);
    }

    @Test
    void onMacroMention()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("reference", "xwiki:XWiki.User");
        parameters.put("style", FULL_NAME.toString());

        this.renderer.setPrinter(new DefaultWikiPrinter());

        this.renderer.onMacro("mention", parameters, "", false);

        verify(this.formatter).formatMention("xwiki:XWiki.User", FULL_NAME);
    }

    @Test
    void onMacroNotMention()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("reference", "xwiki:XWiki.User");
        parameters.put("style", FULL_NAME.toString());

        this.renderer.onMacro("not-mention", parameters, "", false);

        verify(this.formatter, never()).formatMention("xwiki:XWiki.User", FULL_NAME);
    }
}