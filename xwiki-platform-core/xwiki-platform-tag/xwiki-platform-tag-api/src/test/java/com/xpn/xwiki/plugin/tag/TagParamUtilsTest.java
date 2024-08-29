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

package com.xpn.xwiki.plugin.tag;

import org.junit.jupiter.api.Test;

import com.xpn.xwiki.XWikiException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link TagParamUtils}.
 *
 * @version $Id$
 * @since 8.2M1
 */
class TagParamUtilsTest
{
    @Test
    void spacesParameterToList() throws Exception
    {
        assertThat(TagParamUtils.spacesParameterToList("'Space1','Space2'"), contains("Space1", "Space2"));
        assertThat(TagParamUtils.spacesParameterToList("'Space1', 'Space 2',  'Apo''strophe'"),
            contains("Space1", "Space 2", "Apo'strophe"));
        assertThat(TagParamUtils.spacesParameterToList("'single space'"), contains("single space"));
        assertThat(TagParamUtils.spacesParameterToList(""), empty());
    }

    @Test
    void spacesParameterToListExceptions()
    {
        assertThrows(IllegalArgumentException.class, () -> TagParamUtils.spacesParameterToList(null));
        assertThrows(XWikiException.class, () -> TagParamUtils.spacesParameterToList("'space1','space2"));
        assertThrows(XWikiException.class, () -> TagParamUtils.spacesParameterToList("'space1','space2',"));
        assertThrows(XWikiException.class, () -> TagParamUtils.spacesParameterToList("'space1', or 'space2'"));
    }
}
