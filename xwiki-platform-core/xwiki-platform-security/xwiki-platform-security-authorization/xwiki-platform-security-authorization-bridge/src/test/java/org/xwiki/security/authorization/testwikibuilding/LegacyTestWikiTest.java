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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.security.authorization.AbstractLegacyWikiTest;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyTestWikiTest extends AbstractLegacyWikiTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @Test
    void legacyWikiBuilding() throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getComponentManager(), "test.xml", true);

        setContext(testWiki.getXWikiContext());
        testWiki.getXWikiContext().setWikiId("xwiki");

        assertTrue(
            getLegacyImpl().hasAccessLevel("view", "AllanSvensson", "Main.WebHome", testWiki.getXWikiContext()));

        assertTrue(
            getCachingImpl().hasAccessLevel("view", "AllanSvensson", "Main.WebHome", testWiki.getXWikiContext()));

        assertFalse(
            getCachingImpl().hasAccessLevel("edit", "AllanSvensson", "Main.ScriptDocument",
                testWiki.getXWikiContext()));

        assertTrue(
            getCachingImpl().hasAccessLevel("edit", "AllanSvensson", "Main.EditableScriptDocument",
                testWiki.getXWikiContext()));

        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to find hibernate configuration file corresponding to path [/WEB-INF/hibernate.cfg.xml]",
            this.logCapture.getMessage(0));
    }
}
