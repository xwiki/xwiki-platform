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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.xpn.xwiki.XWikiException;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link TagParamUtils}.
 *
 * @version $Id$
 * @since 8.2M1
 */
public class TagParamUtilsTest
{
    @Test
    public void spacesParameterToList() throws Exception
    {
        assertThat(TagParamUtils.spacesParameterToList("'Space1','Space2'"),
            Matchers.contains("Space1", "Space2"));
        assertThat(TagParamUtils.spacesParameterToList("'Space1', 'Space 2',  'Apo''strophe'"),
            Matchers.contains("Space1", "Space 2", "Apo'strophe"));
        assertThat(TagParamUtils.spacesParameterToList("'single space'"),
            Matchers.contains("single space"));
        assertThat(TagParamUtils.spacesParameterToList(""),
            Matchers.empty());
    }

    @Test
    public void spacesParameterToListExceptions() throws XWikiException
    {
        try {
            TagParamUtils.spacesParameterToList(null);
            fail("npe expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            TagParamUtils.spacesParameterToList("'space1','space2");
            fail("XWikiException expected");
        } catch (XWikiException expected) {
        }
        try {
            TagParamUtils.spacesParameterToList("'space1','space2',");
            fail("XWikiException expected");
        } catch (XWikiException expected) {
        }
        try {
            TagParamUtils.spacesParameterToList("'space1', or 'space2'");
            fail("XWikiException expected");
        } catch (XWikiException expected) {
        }
    }
}
