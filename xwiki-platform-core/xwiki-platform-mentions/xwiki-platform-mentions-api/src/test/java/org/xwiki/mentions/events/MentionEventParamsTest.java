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

import org.junit.jupiter.api.Test;
import org.xwiki.mentions.MentionLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Test of {@link MentionEventParams}.
 *
 * @version $Id$
 */
class MentionEventParamsTest
{
    private static final String USER_REFERENCE = "xwiki:XWiki.U1";

    private static final String DOCUMENT_REFERENCE = "xwiki:XWiki.Page";

    private static final String ANCHOR = "anchor0";

    private static final String QUOTE = "Hello @U1";

    private static MentionEventParams initializedParams()
    {
        return new MentionEventParams()
            .setUserReference(USER_REFERENCE)
            .setDocumentReference(DOCUMENT_REFERENCE)
            .setLocation(MentionLocation.COMMENT)
            .setAnchor(ANCHOR)
            .setQuote(QUOTE);
    }

    @Test
    void equalsAndHashCode()
    {
        MentionEventParams params = initializedParams();
        MentionEventParams equalParams = initializedParams();

        assertEquals(params, equalParams);
        assertEquals(params.hashCode(), equalParams.hashCode());

        assertNotEquals(params, new MentionEventParams());
        assertNotEquals(params, initializedParams().setUserReference("xwiki:XWiki.U2"));
        assertNotEquals(params, initializedParams().setDocumentReference("xwiki:XWiki.OtherPage"));
        assertNotEquals(params, initializedParams().setLocation(MentionLocation.DOCUMENT));
        assertNotEquals(params, initializedParams().setAnchor("anchor1"));
        assertNotEquals(params, initializedParams().setQuote("Hello @U2"));
    }
}
