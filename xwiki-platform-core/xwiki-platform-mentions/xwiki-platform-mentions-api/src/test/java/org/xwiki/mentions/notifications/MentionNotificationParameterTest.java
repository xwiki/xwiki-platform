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

import org.junit.jupiter.api.Test;
import org.xwiki.mentions.DisplayStyle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Test of {@link MentionNotificationParameter}.
 *
 * @version $Id$
 */
class MentionNotificationParameterTest
{
    private static final String REFERENCE = "xwiki:XWiki.U1";

    private static final String ANCHOR_ID = "anchor0";

    private static MentionNotificationParameter parameter()
    {
        return new MentionNotificationParameter(REFERENCE, ANCHOR_ID, DisplayStyle.FIRST_NAME);
    }

    @Test
    void equalsAndHashCode()
    {
        MentionNotificationParameter parameter = parameter();
        MentionNotificationParameter equalParameter = parameter();

        assertEquals(parameter, equalParameter);
        assertEquals(parameter.hashCode(), equalParameter.hashCode());

        assertNotEquals(parameter,
            new MentionNotificationParameter("xwiki:XWiki.U2", ANCHOR_ID, DisplayStyle.FIRST_NAME));
        assertNotEquals(parameter, new MentionNotificationParameter(REFERENCE, "anchor1", DisplayStyle.FIRST_NAME));
        assertNotEquals(parameter, new MentionNotificationParameter(REFERENCE, ANCHOR_ID, DisplayStyle.LOGIN));
    }
}
