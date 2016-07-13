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
package org.xwiki.resource.test.ui;

import java.net.URL;

import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;

import static org.junit.Assert.*;

/**
 * Functional tests for the Resource Module (for Use Cases that cannot be easily tested in integration or unit tests).
 *
 * @version $Id$
 * @since 7.2M1
 */
public class ResourceTest extends AbstractTest
{
    @Test
    public void accessResources() throws Exception
    {
        // Verify that accessing /view/A/B goes to A.B.WebHome (Nested Documents URL shortcut feature)
        getUtil().gotoPage(getUtil().getURL("view", new String[] {"A", "B"}, null));
        // Edit the page and verify the URL
        // The wiki is empty and there's no WYSIWYG so clicking edit will go to the wiki editor
        new ViewPage().edit();
        EditPage ep = new EditPage();
        assertTrue(new URL(ep.getPageURL()).getPath().endsWith("A/B/WebHome"));

        // Verify that accessing /edit/A/B edits A.B (Nested Documents URL shortcut feature is only for view mode)
        getUtil().gotoPage(getUtil().getURL("edit", new String[] {"A", "B"}, null));
        ep = new EditPage();
        assertTrue(new URL(ep.getPageURL()).getPath().endsWith("A/B"));

        // Verify that the spaceRedirect=false query string parameter and value can be used to disable the automatic
        // space redirect
        getUtil().gotoPage(getUtil().getURL("view", new String[] {"A", "B"}, "spaceRedirect=false"));
        new ViewPage().edit();
        ep = new EditPage();
        assertTrue(new URL(ep.getPageURL()).getPath().endsWith("A/B"));
    }
}
