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
package org.xwiki.gwt.user.client;

import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for {@link StringUtils}.
 * 
 * @version $Id$
 */
public class StringUtilsTest extends UserTestCase
{
    /**
     * Unit test for {@link StringUtils#join(java.util.Collection, String)}.
     */
    public void testJoin()
    {
        assertNull(StringUtils.join(null, ""));
        assertEquals("", StringUtils.join(Collections.EMPTY_LIST, null));
        assertEquals("ab", StringUtils.join(Arrays.asList("a", "b"), null));
        assertEquals("1,2,3", StringUtils.join(Arrays.asList(1, 2, 3), ","));
    }
}
