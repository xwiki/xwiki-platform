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
package org.xwiki.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Various tests for verifying the view mode of a page, for example to verify links displayed when a page contains
 * special characters, etc.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class ViewTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(testUtils);

    /**
     * See also <a href="https://jira.xwiki.org/browse/XWIKI-8725">XWIKI-8725</a>.
     */
    @Test
    @Order(1)
    void viewPageWhenSpecialCharactersInName(TestUtils testUtils, TestReference testReference) throws Exception
    {
        String testClassName = testReference.getLastSpaceReference().getName();
        // We test a page name containing a space and a dot
        String pageName = testReference.getName() + " 1.0";

        // Delete page since we create it below and want to start the test clean
        testUtils.rest().deletePage(testClassName, pageName);

        // Create the page
        ViewPage vp = testUtils.createPage(testClassName, pageName, "", pageName);

        // Verify that the page we're on has the correct URL and name
        String expectedURLPart = testClassName + "/" + pageName.replaceAll(" ", "%20");
        assertTrue(vp.getPageURL().contains(expectedURLPart),
            String.format("URL [%s] doesn't contain expected part [%s]", vp.getPageURL(), expectedURLPart));
        assertEquals(pageName, vp.getMetaDataValue("page"));
    }
}
