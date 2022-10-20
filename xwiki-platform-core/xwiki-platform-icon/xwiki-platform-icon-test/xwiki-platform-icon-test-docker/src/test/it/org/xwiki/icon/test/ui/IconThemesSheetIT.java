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
package org.xwiki.icon.test.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests of the icon theme sheet.
 *
 * @version $Id$
 */
@UITest
class IconThemesSheetIT
{
    @BeforeEach
    void setUp(TestUtils testUtils)
    {
        // Login as superadmin to define the preferences. 
        testUtils.loginAsSuperAdmin();
    }

    @Test
    @Order(1)
    void createIconTheme(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.createPage(testReference, ")))**not bold**");
        testUtils.addObject(testReference, "IconThemesCode.IconThemeClass");

        testUtils.gotoPage(testReference);

        String content = testUtils.getDriver().findElementWithoutWaiting(By.id("xwikicontent")).getText();

        // Make sure the wiki syntax was not interpreted
        assertTrue(content.contains("**not bold**"));
    }
}
