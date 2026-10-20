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
package org.xwiki.blocknote.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the BlockNote editor.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
public class BlockNoteEditor extends BaseElement
{
    /**
     * The interval between two consecutive auto-saves when the edit form doesn't overwrite it, in seconds.
     */
    private static final int DEFAULT_AUTO_SAVE_INTERVAL = 60;

    /**
     * The notification the edit form displays after it saved, see the
     * {@code core.editors.saveandcontinue.notification.done} translation key.
     */
    private static final String SAVED_NOTIFICATION = "Saved";

    private final String name;

    private int autoSaveInterval = DEFAULT_AUTO_SAVE_INTERVAL;

    private WebElement container;

    /**
     * Create a new instance that can be used to interact with the specified BlockNote instance.
     * 
     * @param name the editor field name
     */
    public BlockNoteEditor(String name)
    {
        this.name = name;
        this.waitToLoad();
    }

    /**
     * @return the name of this editor instance (usually matches the name of the form field the editor is attached to)
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Waits for BlockNote to load.
     *
     * @return this editor instance
     */
    public BlockNoteEditor waitToLoad()
    {
        this.container = this.getDriver().findElement(
            By.cssSelector(".xwiki-blocknote-wrapper:has(> .xwiki-blocknote > input[name='" + this.name + "'])"));
        return this;
    }

    /**
     * @return the toolbar of this editor
     */
    public BlockNoteToolBar getToolBar()
    {
        // The toolbar is rendered via FloatingPortal into document.body (outside the component root). It's shown
        // asynchronously, after the text selection is made, so we need to wait for it to be visible before looking
        // it up.
        By toolBarLocator = By.cssSelector(".bn-root .bn-toolbar");
        // Use a longer timeout than the default: showing the toolbar depends on a selection change (or caret move)
        // event being processed and can occasionally be slow under load (e.g. in the Docker test environment).
        this.getDriver().waitUntilElementIsVisible(toolBarLocator, 40);
        return new BlockNoteToolBar(this.getDriver().findElement(toolBarLocator));
    }

    /**
     * Overwrite the interval between two consecutive auto-saves, so that the tests don't have to wait for the default
     * one (a minute). The value is read from the edit form each time a save is scheduled, so this can be called after
     * the editor has loaded.
     *
     * @param seconds the number of seconds between two consecutive auto-saves
     * @return this editor instance
     * @since 18.8.0RC1
     */
    public BlockNoteEditor setAutoSaveInterval(int seconds)
    {
        // The editor is inside the edit form in standalone edit mode, but only associated with it, through the HTML
        // form attribute, when editing in-place. Let the browser resolve the form the editor submits to, rather than
        // looking for an ancestor form, which exists only in standalone edit mode.
        WebElement valueInput = this.container.findElement(By.cssSelector("input[name='" + this.name + "']"));
        getDriver().executeScript("arguments[0].form.dataset.autoSaveInterval = arguments[1];", valueInput,
            String.valueOf(seconds));
        this.autoSaveInterval = seconds;
        return this;
    }

    /**
     * Wait for the edited content to be saved without the user asking for it, i.e. for the edit form to report that
     * it saved. Note that the save notification is displayed only by the client that saved, so when several clients
     * edit together, call this on the one expected to save.
     *
     * @return this editor instance
     * @since 18.9.0RC1
     */
    public BlockNoteEditor waitForAutoSave()
    {
        // A save lands a bit later than the interval: the saver adds a random amount to it, holds the save election
        // for a moment and then waits for the server. Waiting costs nothing when the save does happen, so leave a
        // wide margin rather than risk a flickering test on a loaded machine.
        withTimeout(4 * this.autoSaveInterval, () -> waitForNotificationSuccessMessage(SAVED_NOTIFICATION));
        return this;
    }

    /**
     * Watch the editing session for longer than the auto-save interval, which is always waited out in full unless a
     * save happens, because only then does the absence of a save prove something.
     *
     * @return {@code true} if the edit form reported that it saved during that time, {@code false} otherwise
     * @since 18.9.0RC1
     */
    public boolean isAutoSavedWithinNextInterval()
    {
        try {
            withTimeout(2 * this.autoSaveInterval, () -> waitForNotificationSuccessMessage(SAVED_NOTIFICATION));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * @return the rich text area of this editor
     */
    public BlockNoteRichTextArea getRichTextArea()
    {
        return new BlockNoteRichTextArea(this.container.findElement(By.cssSelector(".bn-container .bn-editor")));
    }

    private void withTimeout(int timeout, Runnable action)
    {
        int originalTimeout = getDriver().getTimeout();
        getDriver().setTimeout(timeout);
        try {
            action.run();
        } finally {
            getDriver().setTimeout(originalTimeout);
        }
    }
}
