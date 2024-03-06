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
package org.xwiki.realtime.wysiwyg.test.ui;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.ckeditor.test.po.CKEditorConfigurationPane;
import org.xwiki.ckeditor.test.ui.AbstractCKEditorIT;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.ui.TestUtils;

/**
 * Base class for real-time WYSIWYG editor tests.
 * 
 * @version $Id$
 * @since 15.5.4
 * @since 15.9
 */
@ExtendWith(RealtimeTestDebugger.class)
abstract class AbstractRealtimeWYSIWYGEditorIT extends AbstractCKEditorIT
{
    protected static String firstTabHandle;

    @BeforeAll
    static void beforeAll(TestUtils setup)
    {
        // Store the ID of the current browser tab because we're going to open new tabs which will have to be closed
        // after each test, and we need this to avoid closing all tabs.
        firstTabHandle = setup.getDriver().getWindowHandle();

        // Enable the real-time WYSIWYG editor.
        setup.loginAsSuperAdmin();
        CKEditorConfigurationPane ckeditorConfig = CKEditorConfigurationPane.open();
        List<String> disabledPlugins = new ArrayList<>(ckeditorConfig.getDisabledPlugins());
        disabledPlugins.remove("xwiki-realtime");
        ckeditorConfig.setDisabledPlugins(disabledPlugins).clickSave();

        // Test with a simple user.
        setup.createUserAndLogin("John", "pass", "editor", "Wysiwyg");
    }

    @AfterEach
    void afterEach(TestUtils setup, TestReference testReference)
    {
        // Handle the edit mode leave confirmation modal (when ther are unsaved changes).
        setup.getDriver().getWindowHandles().forEach(handle -> {
            setup.getDriver().switchTo().window(handle);
            maybeLeaveEditMode(setup, testReference);
        });

        // Close all tabs except the first one.
        setup.getDriver().getWindowHandles().stream().filter(handle -> !handle.equals(firstTabHandle))
            .forEach(handle -> setup.getDriver().switchTo().window(handle).close());

        // Switch back to the first tab.
        setup.getDriver().switchTo().window(firstTabHandle);
    }
}
