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
package org.xwiki.mentions.events;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.mentions.MentionLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link MentionEvent}.
 *
 * @version $Id$
 */
class MentionEventTest
{
    private static final Set<String> TARGETS = Set.of("xwiki:XWiki.U1");

    private static MentionEventParams params()
    {
        return new MentionEventParams()
            .setUserReference("xwiki:XWiki.Author")
            .setDocumentReference("xwiki:XWiki.Page")
            .setLocation(MentionLocation.COMMENT)
            .setAnchor("anchor0")
            .setQuote("Hello @U1");
    }

    @Test
    void matches()
    {
        MentionEvent event = new MentionEvent(TARGETS, params());

        // Any other mention event matches, whatever its targets and parameters.
        assertTrue(event.matches(new MentionEvent(Set.of("xwiki:XWiki.U2"), new MentionEventParams())));

        assertFalse(event.matches(new NewMentionsEvent()));
        assertFalse(event.matches(null));
    }

    @Test
    void equalsAndHashCode()
    {
        MentionEvent event = new MentionEvent(TARGETS, params());
        MentionEvent equalEvent = new MentionEvent(TARGETS, params());

        assertEquals(event, equalEvent);
        assertEquals(event.hashCode(), equalEvent.hashCode());

        assertNotEquals(event, new MentionEvent(Set.of("xwiki:XWiki.U2"), params()));
        assertNotEquals(event, new MentionEvent(TARGETS, new MentionEventParams()));
    }
}
