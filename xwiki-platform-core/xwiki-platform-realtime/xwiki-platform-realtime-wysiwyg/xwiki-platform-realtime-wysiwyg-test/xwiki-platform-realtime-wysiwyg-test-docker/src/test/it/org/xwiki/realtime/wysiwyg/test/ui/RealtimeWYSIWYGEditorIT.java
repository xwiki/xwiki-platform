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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.WYSIWYGEditorAdministrationSectionPage;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeCKEditor;
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
        RealtimeWYSIWYGEditPage editPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor editor = editPage.getContenEditor();
        editor.getRichTextArea().sendKeys("test");
        ViewPage viewPage = editPage.clickSaveAndView();
        assertEquals("test", viewPage.getContent());
    }
}
