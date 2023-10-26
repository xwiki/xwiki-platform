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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.xwiki.administration.test.po.WYSIWYGEditorAdministrationSectionPage;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeCKEditor;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeRichTextAreaElement;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeRichTextAreaElement.CoeditorPosition;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeWYSIWYGEditPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Functional tests for the real-time WYSIWYG editor.
 * 
 * @version $Id$
 * @since 14.10.19
 * @since 15.5.4
 * @since 15.9
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
        "org.xwiki.platform:xwiki-platform-websocket",

        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",

        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index",

        // Solr search is used to get suggestions for the link quick action.
        "org.xwiki.platform:xwiki-platform-search-solr-query"
    },
    resolveExtraJARs = true
)
class RealtimeWYSIWYGEditorIT
{
    @BeforeAll
    static void configure(TestUtils setup)
    {
        // Enable the real-time WYSIWYG editor.
        setup.loginAsSuperAdmin();
        WYSIWYGEditorAdministrationSectionPage.gotoPage().setDefaultWYSIWYGEditor("Realtime CKEditor");

        // Test with a simple user.
        setup.createUserAndLogin("John", "pass", "editor", "Wysiwyg");
    }

    @Test
    @Order(1)
    void editAlone(TestReference testReference, TestUtils setup)
    {
        // Start fresh.
        setup.deletePage(testReference);

        RealtimeWYSIWYGEditPage editPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor editor = editPage.getContenEditor();

        // Verify that the Allow Realtime Collaboration checkbox is checked.
        assertTrue(editPage.isRealtimeEditing());

        // Verify that the Preview button is hidden.
        assertFalse(editPage.hasPreviewButton());

        // The Autosave checkbox is also hidden because autosave is done by the realtime editor (you can't disable it
        // while editing in realtime).
        assertFalse(editPage.getAutoSaveCheckbox().isDisplayed());

        // Verify that we're editing alone.
        assertTrue(editor.getToolBar().isEditingAlone());

        RealtimeRichTextAreaElement textArea = editor.getRichTextArea();
        textArea.sendKeys("one");

        // Verify the cursor indicator on the left of the editing area.
        List<CoeditorPosition> coeditorPositions = textArea.getCoeditorPositions();
        assertEquals(1, coeditorPositions.size());

        CoeditorPosition selfPosition = coeditorPositions.get(0);
        assertEquals("John", selfPosition.getAvatarHint());
        assertTrue(selfPosition.getAvatarURL().contains("noavatar.png"),
            "Unexpected avatar URL: " + selfPosition.getAvatarURL());
        selfPosition.waitForLocation(new Point(3, 18));

        // Verify that the cursor indicator is updated when typing.
        textArea.sendKeys(Keys.ENTER, "two");
        selfPosition.waitForLocation(new Point(3, 48));

        // Verify the action buttons (Save and Cancel).
        editPage.clickSaveAndContinue();
        textArea.sendKeys(Keys.ENTER, "three");
        ViewPage viewPage = editPage.clickCancel();
        assertEquals("one\ntwo", viewPage.getContent());

        // Edit again and verify the Save and View button.
        viewPage.edit();
        editPage = new RealtimeWYSIWYGEditPage();
        editPage.getContenEditor().getRichTextArea().sendKeys(Keys.ARROW_DOWN, Keys.END, Keys.ENTER, "three");
        viewPage = editPage.clickSaveAndView();
        assertEquals("one\ntwo\nthree", viewPage.getContent());
    }
}
