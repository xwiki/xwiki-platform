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
package org.xwiki.mentions;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.xwiki.mentions.DisplayStyle.FIRST_NAME;
import static org.xwiki.mentions.DisplayStyle.FULL_NAME;
import static org.xwiki.mentions.DisplayStyle.LOGIN;

/**
 * Test of {@link DisplayStyle}.
 *
 * @version $Id$
 * @since 13.10.7
 * @since 14.4.2
 * @since 14.5
 */
@ComponentTest
class DisplayStyleTest
{
    @Test
    void getOrDefault()
    {
        assertEquals(FULL_NAME, DisplayStyle.getOrDefault(null));
        assertEquals(FULL_NAME, DisplayStyle.getOrDefault("FULL_NAME"));
        assertEquals(FIRST_NAME, DisplayStyle.getOrDefault("FIRST_NAME"));
        assertEquals(LOGIN, DisplayStyle.getOrDefault("LOGIN"));
        assertThrows(IllegalArgumentException.class, () -> DisplayStyle.getOrDefault("UNKNOWN"));
    }
}
