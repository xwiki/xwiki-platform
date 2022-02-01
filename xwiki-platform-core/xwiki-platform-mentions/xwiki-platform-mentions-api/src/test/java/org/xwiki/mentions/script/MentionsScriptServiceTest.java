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
package org.xwiki.mentions.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.mentions.MentionsConfiguration;
import org.xwiki.mentions.MentionsFormatter;
import org.xwiki.mentions.internal.MentionFormatterProvider;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link MentionsScriptService}.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@ComponentTest
class MentionsScriptServiceTest
{
    @InjectMockComponents
    private MentionsScriptService mentionsScriptService;

    @MockComponent
    private MentionFormatterProvider mentionFormatterProvider;
    
    @Mock
    private MentionsFormatter mentionsFormatter;

    @BeforeEach
    void setUp()
    {
        when(this.mentionFormatterProvider.get(any())).thenReturn(this.mentionsFormatter);
    }

    @Test
    void format()
    {
        this.mentionsScriptService.format("actorReference", DisplayStyle.FIRST_NAME, "actorType");
        verify(this.mentionFormatterProvider).get("actorType");
        verify(this.mentionsFormatter).formatMention("actorReference", DisplayStyle.FIRST_NAME);
    }

    @Test
    void formatEmptyType()
    {
        this.mentionsScriptService.format("actorReference", DisplayStyle.FIRST_NAME, "");
        verify(this.mentionFormatterProvider).get(MentionsConfiguration.USER_MENTION_TYPE);
        verify(this.mentionsFormatter).formatMention("actorReference", DisplayStyle.FIRST_NAME);
    }
}
