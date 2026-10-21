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
package org.xwiki.flamingo.test.docker;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Various tests for verifying the view mode of a page, for example to verify links displayed when a page contains
 * special characters, etc.
 *
 * @version $Id$
 * @since 4.5M1
 */
@UITest
class ViewPageIT
{
    /**
     * See also <a href="https://jira.xwiki.org/browse/XWIKI-8725">XWIKI-8725</a>.
     */
    @Test
    void viewPageWhenSpecialCharactersInName(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();

        // We test a page name containing a space and a dot
        String pageName = testReference.getLastSpaceReference().getName() + " 1.0";
        SpaceReference spaceReference = (SpaceReference) testReference.getLastSpaceReference().getParent();

        // Create the page
        ViewPage vp = setup.createPage(new DocumentReference(pageName, spaceReference), "", pageName);

        // Verify that the page we're on has the correct URL and name
        String expectedURLPart = spaceReference.getName() + "/" + pageName.replace(" ", "%20");
        assertTrue(vp.getPageURL().contains(expectedURLPart),
            String.format("URL [%s] doesn't contain expected part [%s]", vp.getPageURL(), expectedURLPart));
        assertEquals(pageName, vp.getMetaDataValue("page"));
    }
}
