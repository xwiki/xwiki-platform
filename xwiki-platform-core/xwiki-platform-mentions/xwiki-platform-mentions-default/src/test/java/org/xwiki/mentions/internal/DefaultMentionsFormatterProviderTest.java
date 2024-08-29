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

package org.xwiki.mentions.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.mentions.MentionsFormatter;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.test.LogLevel.DEBUG;

/**
 * Tests of {@link DefaultMentionsFormatterProvider}.
 *
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
class DefaultMentionsFormatterProviderTest
{
    @InjectMockComponents
    private DefaultMentionsFormatterProvider provider;

    @MockComponent
    private ComponentManager componentManager;

    @MockComponent
    private MentionsFormatter defaultFormatter;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(DEBUG);

    @Test
    void get() throws Exception
    {
        MentionsFormatter expectedMentionsFormatter = mock(MentionsFormatter.class);
        when(this.componentManager.getInstance(MentionsFormatter.class, "ap")).thenReturn(expectedMentionsFormatter);
        MentionsFormatter userMentionFormatter = this.provider.get("ap");
        assertEquals(expectedMentionsFormatter, userMentionFormatter);
    }

    @Test
    void getNull() throws Exception
    {
        MentionsFormatter expectedMentionsFormatter = mock(MentionsFormatter.class);
        when(this.componentManager.getInstance(MentionsFormatter.class, "user")).thenReturn(expectedMentionsFormatter);
        assertEquals(expectedMentionsFormatter, this.provider.get(null));
    }

    @Test
    void getNotFound() throws Exception
    {
        when(this.componentManager.getInstance(MentionsFormatter.class, "testhint"))
            .thenThrow(ComponentLookupException.class);
        MentionsFormatter user = this.provider.get("testhint");
        assertEquals(this.defaultFormatter, user);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.DEBUG, this.logCapture.getLogEvent(0).getLevel());
        assertEquals(
            "Unable to find a formatter with type [testhint]. Fallback to the default formatter. "
                + "Cause: [ComponentLookupException: ]",
            this.logCapture.getMessage(0));
    }
}