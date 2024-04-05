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
package org.xwiki.realtime.wysiwyg.test.po;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.ckeditor.test.po.CKEditorToolBar;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the realtime CKEditor tool bar.
 * 
 * @version $Id$
 * @since 15.5.4
 * @since 15.9
 */
public class RealtimeCKEditorToolBar extends CKEditorToolBar
{
    private String lastMergeStatus;

    /**
     * Represents a coeditor listed in the tool bar.
     */
    public static class Coeditor extends BaseElement
    {
        private WebElement container;

        public Coeditor(WebElement container)
        {
            this.container = container;
        }

        public String getId()
        {
            return this.container.getAttribute("data-id");
        }

        public String getName()
        {
            return this.container.getText();
        }

        public String getReference()
        {
            return this.container.getAttribute("data-reference");
        }

        public String getURL()
        {
            return this.container.getAttribute("href");
        }

        public String getAvatarURL()
        {
            return getAvatar().getAttribute("src");
        }

        public String getAvatarHint()
        {
            return getAvatar().getAttribute("title");
        }

        public void click()
        {
            this.container.click();
        }

        private WebElement getAvatar()
        {
            return getDriver().findElementWithoutWaiting(this.container, By.className("rt-user-avatar"));
        }
    }

    /**
     * Create a new tool bar instance for the given editor.
     * 
     * @param editor the editor that owns the tool bar
     */
    public RealtimeCKEditorToolBar(RealtimeCKEditor editor)
    {
        super(editor);
        waitToLoad();
    }

    private void waitToLoad()
    {
        // We don't wait for the toolbar to be visible because the toolbar is hidden for the in-place editor when the
        // editor doesn't have the focus (and the editor is not always focused when loaded).
        getDriver().waitUntilCondition(ExpectedConditions.presenceOfNestedElementLocatedBy(getContainer(),
            By.cssSelector(".rt-toolbar[data-user-id]")));
    }

    /**
     * @return the user identifier in the real-time session
     */
    public String getUserId()
    {
        return getDriver().findElementWithoutWaiting(getContainer(), By.className("rt-toolbar"))
            .getAttribute("data-user-id");
    }

    /**
     * @return {@code true} if the user is editing alone, {@code false} otherwise
     */
    public boolean isEditingAlone()
    {
        return "Editing alone"
            .equals(getDriver().findElementWithoutWaiting(getContainer(), By.className("rt-user-list")).getText());
    }

    /**
     * @return the list of coeditors listed in the tool bar
     */
    public List<Coeditor> getCoeditors()
    {
        return getDriver().findElementsWithoutWaiting(getContainer(), By.className("rt-user-link")).stream()
            .map(Coeditor::new).collect(Collectors.toList());
    }

    /**
     * @param coeditorId the coeditor identifier
     * @return the coeditor with the specified identifier or {@code null} if no such coeditor exists
     */
    public Coeditor getCoeditor(String coeditorId)
    {
        return getCoeditors().stream().filter(coeditor -> coeditor.getId().equals(coeditorId)).findFirst().orElse(null);
    }

    /**
     * Wait for the specified coeditor to appear in the tool bar.
     * 
     * @param coeditorId the coeditor identifier
     * @return this tool bar instance
     */
    public RealtimeCKEditorToolBar waitForCoeditor(String coeditorId)
    {
        getDriver().waitUntilElementIsVisible(getContainer(),
            By.cssSelector(".rt-user-link[data-id='" + coeditorId + "']"));
        return this;
    }

    /**
     * Wait for the auto-save to be triggered.
     * 
     * @return the merge status message
     */
    public String waitForAutoSave()
    {
        // Unsaved changes are pushed after 60 seconds, but the check for unsaved changes is done every 20 seconds, so
        // we need to wait at most 100 seconds (adding an error margin of 20 seconds).
        int timeout = 100;

        getDriver().waitUntilCondition(driver -> {
            String newMergeStatus =
                getDriver().findElementWithoutWaiting(getContainer(), By.className("realtime-merge")).getText();
            boolean hasNewMergeStatus = !newMergeStatus.isEmpty() && !newMergeStatus.equals(this.lastMergeStatus);
            this.lastMergeStatus = newMergeStatus;
            return hasNewMergeStatus;
        }, timeout);
        return this.lastMergeStatus;
    }
}
