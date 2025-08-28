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
package org.xwiki.security.authorization.testwikibuilding;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.security.authorization.AbstractLegacyWikiTestCase;
import org.xwiki.security.authorization.internal.XWikiCachingRightService;

import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;

public class TestLegacyTestWiki extends AbstractLegacyWikiTestCase
{
    @Test
    public void testLegacyWikiBuilding() throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), "test.xml", true);

        XWikiRightServiceImpl legacyImpl = new XWikiRightServiceImpl();

        testWiki.getXWikiContext().setWikiId("xwiki");

        Assert.assertTrue(
            legacyImpl.hasAccessLevel("view", "AllanSvensson", "Main.WebHome", testWiki.getXWikiContext()));

        XWikiCachingRightService cachingImpl = new XWikiCachingRightService();

        Assert.assertTrue(
            cachingImpl.hasAccessLevel("view", "AllanSvensson", "Main.WebHome", testWiki.getXWikiContext()));

        Assert.assertFalse(
            cachingImpl.hasAccessLevel("edit", "AllanSvensson", "Main.ScriptDocument", testWiki.getXWikiContext())
        );

        Assert.assertTrue(
            cachingImpl.hasAccessLevel("edit", "AllanSvensson", "Main.EditableScriptDocument", testWiki.getXWikiContext())
        );
    }
}
