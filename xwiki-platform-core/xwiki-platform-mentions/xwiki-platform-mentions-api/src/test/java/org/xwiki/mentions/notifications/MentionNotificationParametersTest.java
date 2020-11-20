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
package org.xwiki.mentions.notifications;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.xwiki.mentions.MentionLocation.DOCUMENT;

/**
 * Test of {@link MentionNotificationParameters}.
 *
 * @version $Id$
 * @since 12.10
 */
class MentionNotificationParametersTest
{
    public static final String AUTHOR_REFERENCE = "xwiki:XWiki.Author";

    public static final DocumentReference ENTITY_REFERENCE = new DocumentReference("xwiki", "XWiki", "Page");

    public static final String VERSION = "1.0";

    public static final MentionNotificationParameter MENTION_U1 =
        new MentionNotificationParameter("xwiki:XWiki.U1", "anchor0", DisplayStyle.FIRST_NAME);

    public static final MentionNotificationParameter MENTION_U2 =
        new MentionNotificationParameter("xwiki:XWiki.U2", "anchor1", DisplayStyle.FIRST_NAME);

    public static final MentionNotificationParameter MENTION_U3 =
        new MentionNotificationParameter("xwiki:XWiki.U3", "anchor0", DisplayStyle.FIRST_NAME);

    public static final MentionNotificationParameter MENTION_U4 =
        new MentionNotificationParameter("xwiki:XWiki.U4", "anchor1", DisplayStyle.FIRST_NAME);

    public static final String TYPE_A = "a";

    public static final String TYPE_B = "b";

    @Test
    void mentionNotificationParameters()
    {
        MentionNotificationParameters mentionNotificationParameters =
            new MentionNotificationParameters(AUTHOR_REFERENCE, ENTITY_REFERENCE, DOCUMENT, VERSION);

        assertEquals(AUTHOR_REFERENCE, mentionNotificationParameters.getAuthorReference());
        assertEquals(ENTITY_REFERENCE, mentionNotificationParameters.getEntityReference());
        assertEquals(DOCUMENT, mentionNotificationParameters.getLocation());
        assertEquals(VERSION, mentionNotificationParameters.getVersion());
        assertEquals(new HashMap<>(), mentionNotificationParameters.getNewMentions());
        assertEquals(new HashMap<>(), mentionNotificationParameters.getMentions());

        mentionNotificationParameters.addMention(TYPE_A, MENTION_U1);
        mentionNotificationParameters.addNewMention(TYPE_B, MENTION_U2);

        assertEquals(AUTHOR_REFERENCE, mentionNotificationParameters.getAuthorReference());
        assertEquals(ENTITY_REFERENCE, mentionNotificationParameters.getEntityReference());
        assertEquals(DOCUMENT, mentionNotificationParameters.getLocation());
        assertEquals(VERSION, mentionNotificationParameters.getVersion());
        Map<Object, Object> expected = new HashMap<>();
        Set<Object> value = new HashSet<>();
        value.add(MENTION_U2);
        expected.put("b", value);
        assertEquals(expected, mentionNotificationParameters.getNewMentions());
        Map<Object, Object> expected1 = new HashMap<>();
        Set<Object> value1 = new HashSet<>();
        value1.add(MENTION_U1);
        expected1.put("a", value1);
        assertEquals(expected1, mentionNotificationParameters.getMentions());
    }

    @Test
    void mentionNotificationParametersGetMentionsNotModifiable()
    {
        Map<String, Set<MentionNotificationParameter>> mentions =
            new MentionNotificationParameters(AUTHOR_REFERENCE, ENTITY_REFERENCE, DOCUMENT, VERSION).getMentions();
        assertThrows(UnsupportedOperationException.class, () -> mentions.put("a", null));
    }

    @Test
    void mentionNotificationParametersGetNewMentionsNotModifiable()
    {
        Map<String, Set<MentionNotificationParameter>> newMentions =
            new MentionNotificationParameters(AUTHOR_REFERENCE, ENTITY_REFERENCE, DOCUMENT, VERSION).getNewMentions();
        assertThrows(UnsupportedOperationException.class, () -> newMentions.put("a", null));
    }
}