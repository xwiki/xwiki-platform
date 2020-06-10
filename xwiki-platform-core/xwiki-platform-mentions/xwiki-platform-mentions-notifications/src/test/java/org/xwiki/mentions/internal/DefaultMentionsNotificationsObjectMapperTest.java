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

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.events.MentionEventParams;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link DefaultMentionsNotificationsObjectMapper}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@ComponentTest
class DefaultMentionsNotificationsObjectMapperTest
{
    @InjectMockComponents
    private DefaultMentionsNotificationsObjectMapper objectMapper;

    @Test
    void unserializeMinimalObject()
    {
        Optional<MentionEventParams> actual =
            this.objectMapper.unserialize("{}");
        assertTrue(actual.isPresent());
        assertEquals(new MentionEventParams(), actual.get());
    }

    @Test
    void unserialize()
    {
        Optional<MentionEventParams> actual =
            this.objectMapper.unserialize(
                "{\"userReference\":\"xwiki:XWiki.User\","
                    + "\"documentReference\":\"xwiki:XWiki.Doc\","
                    + "\"location\":\"COMMENT\","
                    + "\"anchor\":\"myAnchor\"}");
        assertTrue(actual.isPresent());
        MentionEventParams expected =
            new MentionEventParams()
                .setDocumentReference("xwiki:XWiki.Doc")
                .setUserReference("xwiki:XWiki.User")
                .setLocation(MentionLocation.COMMENT)
                .setAnchor("myAnchor");
        assertEquals(expected, actual.get());
    }

    @Test
    void serialize()
    {
        Optional<String> actual = this.objectMapper.serialize(
            new MentionEventParams()
                .setLocation(MentionLocation.COMMENT)
                .setUserReference("xwiki:XWiki.User")
                .setDocumentReference("xwiki:XWiki.Doc")
                .setAnchor("anchor"));
        assertTrue(actual.isPresent());
        assertEquals(
            "{\"userReference\":\"xwiki:XWiki.User\","
                + "\"documentReference\":\"xwiki:XWiki.Doc\","
                + "\"location\":\"COMMENT\","
                + "\"anchor\":\"anchor\"}",
            actual.get());
    }
}