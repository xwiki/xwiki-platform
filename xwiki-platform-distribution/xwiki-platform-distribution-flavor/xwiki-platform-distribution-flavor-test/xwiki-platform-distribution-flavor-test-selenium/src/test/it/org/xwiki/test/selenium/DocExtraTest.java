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
package org.xwiki.test.selenium;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.selenium.framework.AbstractXWikiTestCase;

/**
 * Verify the document extra feature of XWiki.
 * 
 * @version $Id$
 */
public class DocExtraTest extends AbstractXWikiTestCase
{
    /**
     * Test document extras presence after a click on the corresponding tabs.
     */
    @Test
    public void testDocExtraLoadingFromTabClicks()
    {
        open("Main", "WebHome");

        clickLinkWithXPath("//a[@id='Attachmentslink']", false);
        waitForDocExtraPaneActive("attachments");

        clickLinkWithXPath("//a[@id='Historylink']", false);
        waitForDocExtraPaneActive("history");

        clickLinkWithXPath("//a[@id='Informationlink']", false);
        waitForDocExtraPaneActive("information");

        clickLinkWithXPath("//a[@id='Commentslink']", false);
        waitForDocExtraPaneActive("comments");
    }

    /**
     * Test document extras presence after pressing the corresponding keyboard shortcuts. This test also verify that the
     * browser scrolls to the bottom of the page.
     * 
     * @throws InterruptedException if selenium fails to simulate keyboard shortcut.
     */
    @Test
    public void testDocExtraLoadingFromKeyboardShortcuts() throws InterruptedException
    {
        open("Main", "WebHome");

        getSkinExecutor().pressKeyboardShortcut("a", false, false, false);
        waitForDocExtraPaneActive("attachments");
        assertDocExtraPaneInView("attachments");
        scrollToPageTop();

        getSkinExecutor().pressKeyboardShortcut("h", false, false, false);
        waitForDocExtraPaneActive("history");
        assertDocExtraPaneInView("history");
        scrollToPageTop();

        getSkinExecutor().pressKeyboardShortcut("i", false, false, false);
        waitForDocExtraPaneActive("information");
        assertDocExtraPaneInView("information");
        scrollToPageTop();

        getSkinExecutor().pressKeyboardShortcut("c", false, false, false);
        waitForDocExtraPaneActive("comments");
        assertDocExtraPaneInView("comments");
    }

    /**
     * Test document extra presence when the user arrives from an URL with anchor. This test also verify that the
     * browser scrolls to the bottom of the page.
     */
    @Test
    public void testDocExtraLoadingFromURLAnchor()
    {
        LocalDocumentReference homePageReference = new LocalDocumentReference("Sandbox", "WebHome");
        LocalDocumentReference otherPageReference = new LocalDocumentReference("Main", "ThisPageDoesNotExist");
        List<String> docExtraPanes = Arrays.asList("attachments", "history", "information", "comments");
        for (String docExtraPane : docExtraPanes) {
            // We have to load a different page first since opening the same page with a new anchor doesn't call
            // our functions (on purpose).
            getUtil().gotoPage(otherPageReference);
            getUtil().gotoPage(homePageReference, "view", null, StringUtils.capitalize(docExtraPane));
            waitForDocExtraPaneActive(docExtraPane);
            assertDocExtraPaneInView(docExtraPane);
        }
    }

    /**
     * Test document extra presence after clicks on links directing to the extra tabs (top menu for Toucan skin for
     * example and shortcuts for Colibri skin for example). This test also verify that the browser scrolls to the bottom
     * of the page.
     */
    @Test
    public void testDocExtraLoadingFromLinks()
    {
        open("Main", "WebHome");

        clickShowAttachments();
        waitForDocExtraPaneActive("attachments");
        assertDocExtraPaneInView("attachments");
        scrollToPageTop();

        clickShowHistory();
        waitForDocExtraPaneActive("history");
        assertDocExtraPaneInView("history");
        scrollToPageTop();

        clickShowInformation();
        waitForDocExtraPaneActive("information");
        assertDocExtraPaneInView("information");
        scrollToPageTop();

        clickShowComments();
        waitForDocExtraPaneActive("comments");
        assertDocExtraPaneInView("comments");
    }

    /**
     * @param paneId valid values: "history", "comments", etc
     */
    private void waitForDocExtraPaneActive(String paneId)
    {
        waitForElement(paneId + "content");
    }

    private void scrollToPageTop()
    {
        getSelenium().getEval("window.scroll(0,0);");
    }

    private void assertDocExtraPaneInView(String paneId)
    {
        String paneContentId = String.format("%scontent", paneId);
        assertElementInView(By.id(paneContentId));
    }
}
