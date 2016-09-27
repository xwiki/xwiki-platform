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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.diff.internal.DefaultDiffManager;
import org.xwiki.logging.LogLevel;
import org.xwiki.test.ComponentManagerRule;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link MergeUtils}.
 *
 * @since 4.5.4
 * @since 5.0.2
 * @since 5.1M1
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
    public void mergeWhenDifferences()
    {
        MergeResult result = new MergeResult();
        assertEquals("content\n", MergeUtils.mergeLines("content", "content\n", "content", result));
        assertTrue(result.isModified());
    }

    @Test
    public void mergeWhenCurrentStringDoesntEndWithNewLine()
    {
        MergeResult result = new MergeResult();
        assertEquals("content", MergeUtils.mergeLines("content", "content", "content", result));
        assertFalse(result.isModified());
    }

    @Test
    public void mergeWhenCurrentStringEndsWithNewLine()
    {
        MergeResult result = new MergeResult();
        assertEquals("content\n", MergeUtils.mergeLines("content\n", "content\n", "content\n", result));
        assertFalse(result.isModified());
    }

    @Test
    public void mergeObjectSimple()
    {
        MergeResult result = new MergeResult();
        assertEquals("new", MergeUtils.mergeOject("old", "new", "old", result));
        assertTrue(result.isModified());
    }

    @Test
    public void mergeObjectAlreadyDone()
    {
        MergeResult result = new MergeResult();
        assertEquals("new", MergeUtils.mergeOject("old", "new", "new", result));
        assertFalse(result.isModified());
    }

    @Test
    public void mergeObjectWhileModified()
    {
        MergeResult result = new MergeResult();
        assertEquals("old modified", MergeUtils.mergeOject("old", "new", "old modified", result));
        assertFalse(result.isModified());
        // conflicts are flagged as errors in the log 
        assertFalse(result.getLog().getLogs(LogLevel.ERROR).isEmpty());
    }

    @Test
    public void mergeListSimple()
    {
        MergeResult result = new MergeResult();
        List<String> current = new ArrayList<String>(Arrays.asList("old1", "old2"));
        MergeUtils.mergeList(Arrays.asList("old1", "old2"), Arrays.asList("new1", "new2"), current, result);
        assertEquals(Arrays.asList("new1", "new2"), current);
        assertTrue(result.isModified());
    }

    @Test
    public void mergeListAlreadyDone()
    {
        MergeResult result = new MergeResult();
        List<String> current = new ArrayList<String>(Arrays.asList("new1", "new2"));
        MergeUtils.mergeList(Arrays.asList("old1", "old2"), Arrays.asList("new1", "new2"), current, result);
        assertEquals(Arrays.asList("new1", "new2"), current);
        assertEquals(Arrays.asList("new1", "new2"), current);
        assertFalse(result.isModified());
    }

    @Test
    public void mergeListWhileModified()
    {
        MergeResult result = new MergeResult();
        List<String> current = new ArrayList<String>(Arrays.asList("old modified1", "old modified2"));
        MergeUtils.mergeList(Arrays.asList("old1", "old2"), Arrays.asList("new1", "new2"), current, result);
        assertEquals(Arrays.asList("old modified1", "old modified2"), current);
        assertFalse(result.isModified());
        // conflicts are flagged as errors in the log 
        assertFalse(result.getLog().getLogs(LogLevel.ERROR).isEmpty());
    }    
}
