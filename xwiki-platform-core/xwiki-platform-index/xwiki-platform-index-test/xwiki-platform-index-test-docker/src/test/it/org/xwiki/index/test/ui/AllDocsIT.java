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
package org.xwiki.index.test.ui;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.index.test.po.AllDocsPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LiveTableElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the AllDocs page.
 *
 * @version $Id$
 * @since 11.4RC1
 */
@UITest
public class AllDocsIT
{
    @RepeatedTest(30)
    public void verifyAllDocs(TestUtils setup, TestInfo info)
    {
        String testName = info.getTestMethod().get().getName();
        String testClassName = info.getTestClass().get().getSimpleName();

        setup.loginAsSuperAdmin();
        setup.deleteSpace(testClassName);

        // Test 1: Verify that the Action column is displayed only for logged in users
        // Create a test user
        setup.createUserAndLogin(testClassName + "_" + testName, "password");
        AllDocsPage page = AllDocsPage.gotoPage();
        LiveTableElement livetable = page.clickIndexTab();
        assertTrue(livetable.hasColumn("Actions"), "No Actions column found");

        // Logs out to be guest to verify that the Action columns is no longer displayed
        setup.forceGuestUser();

        page = AllDocsPage.gotoPage();
        livetable = page.clickIndexTab();
        assertFalse(livetable.hasColumn("Actions"), "Actions column shouldn't be visible for guests");

        // Test 2: Verify filtering works by filtering on the document name
        livetable = page.clickIndexTab();
        livetable.filterColumn("xwiki-livetable-alldocs-filter-2", testName);
        // We get one result for the user we've created
        assertEquals(1, livetable.getRowCount());
        assertTrue(livetable.hasRow("Title", testClassName + "_" + testName));
    }
}
