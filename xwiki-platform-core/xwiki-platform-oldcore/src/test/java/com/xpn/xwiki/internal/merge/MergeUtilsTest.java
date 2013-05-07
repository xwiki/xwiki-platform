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
package com.xpn.xwiki.internal.merge;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.diff.internal.DefaultDiffManager;
import org.xwiki.test.ComponentManagerRule;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.web.Utils;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MergeUtils}.
 *
 * @since 4.5.4, 5.0.2, 5.1M1
 */
@ComponentList({
    DefaultDiffManager.class
})
public class MergeUtilsTest
{
    @Rule
    public ComponentManagerRule componentManager = new ComponentManagerRule();

    @Before
    public void setUp()
    {
        Utils.setComponentManager(this.componentManager);
    }

    @Test
    public void mergeWhenCurrentStringEndsWithNewLine()
    {
        MergeResult result = new MergeResult();
        assertEquals("content\n", MergeUtils.mergeLines("content\n", "content\n", "content\n", result));
        assertFalse(result.isModified());
    }
}
