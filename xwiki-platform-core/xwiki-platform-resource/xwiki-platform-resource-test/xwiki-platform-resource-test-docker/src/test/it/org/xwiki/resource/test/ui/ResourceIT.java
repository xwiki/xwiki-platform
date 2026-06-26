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

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the Resource Module (for Use Cases that cannot be easily tested in integration or unit tests).
 *
 * @version $Id$
 */
@UITest
class ResourceIT
{
    @Test
    void accessResources(TestUtils setup)
    {
        // Verify that accessing /view/A/B goes to A.B.WebHome (Nested Documents URL shortcut feature)
        setup.gotoPage(setup.getURL("view", new String[] {"A", "B"}, null));
        // Edit the page and verify the URL
        // The wiki is empty and there's no WYSIWYG so clicking edit will go to the wiki editor
        new ViewPage().edit();
        EditPage ep = new EditPage();
        assertTrue(URI.create(ep.getPageURL()).getPath().endsWith("A/B/WebHome"));

        // Verify that accessing /edit/A/B edits A.B (Nested Documents URL shortcut feature is only for view mode)
        setup.gotoPage(setup.getURL("edit", new String[] {"A", "B"}, null));
        ep = new EditPage();
        assertTrue(URI.create(ep.getPageURL()).getPath().endsWith("A/B"));

        // Verify that the spaceRedirect=false query string parameter and value can be used to disable the automatic
        // space redirect
        setup.gotoPage(setup.getURL("view", new String[] {"A", "B"}, "spaceRedirect=false"));
        new ViewPage().edit();
        ep = new EditPage();
        assertTrue(URI.create(ep.getPageURL()).getPath().endsWith("A/B"));
    }
}
