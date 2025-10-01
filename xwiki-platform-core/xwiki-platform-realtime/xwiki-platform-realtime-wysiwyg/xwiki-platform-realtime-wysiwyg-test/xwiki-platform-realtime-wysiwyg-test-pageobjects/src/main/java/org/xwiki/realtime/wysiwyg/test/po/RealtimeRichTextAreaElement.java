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
import java.util.Map;
import java.util.function.IntConsumer;

import org.apache.commons.lang3.Strings;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the realtime rich text area.
 * 
 * @version $Id$
 * @since 15.5.4
 * @since 15.9
 */
public class RealtimeRichTextAreaElement extends RichTextAreaElement
{
    /**
     * Represents the position of a coeditor in the rich text area (displayed on the left side).
     */
    public class CoeditorPosition extends BaseElement
    {
        private String id;

        public CoeditorPosition(String id)
        {
            this(id, false);
        }

        public CoeditorPosition(String id, boolean wait)
        {
            this.id = id;

            // Wait for the specified coeditor position to be available in the DOM.
            if (wait) {
                getDriver().waitUntilCondition(driver -> getFromEditedContent(this::getContainer));
            }
        }

        public String getCoeditorId()
        {
            return Strings.CS.removeStart(id, "rt-user-");
        }

        public String getAvatarURL()
        {
            return getFromEditedContent(() -> getContainer().getDomAttribute("src"));
        }

        public String getAvatarHint()
        {
            return getFromEditedContent(() -> getContainer().getDomAttribute("title"));
        }

        public Point getLocation()
        {
            return getFromEditedContent(() -> getContainer().getLocation());
        }

        public CoeditorPosition waitForLocation(Point point)
        {
            getDriver().waitUntilCondition(driver -> getFromEditedContent(() -> {
                try {
                    return getContainer().getLocation().equals(point);
                } catch (StaleElementReferenceException e) {
                    // The coeditor position (caret indicator) can be updated while we're waiting for it.
                    return false;
                }
            }));
            return this;
        }

        /**
         * @return {@code true} if this caret indicator is visible in the rich text area
         */
        @SuppressWarnings("unchecked")
        public boolean isVisible()
        {
            return getFromEditedContent(() -> {
                WebElement root = getDriver().findElementWithoutWaitingWithoutScrolling(By.tagName("html"));
                int viewportHeight = Integer.parseInt(root.getDomProperty("clientHeight"));
                int viewportWidth = Integer.parseInt(root.getDomProperty("clientWidth"));
                Map<String, Long> position = (Map<String, Long>) getDriver()
                    .executeScript("return arguments[0].getBoundingClientRect()", getContainer());
                return position.get("y") >= 0 && position.get("x") >= 0
                    && (position.get("y") + position.get("height")) <= viewportHeight
                    && (position.get("x") + position.get("width")) <= viewportWidth;
            });
        }

        private WebElement getContainer()
        {
            // Make sure we don't scroll the page when locating the coeditor position.
            return getDriver().findElementWithoutWaitingWithoutScrolling(By.id(this.id));
        }
    }

    /**
     * Creates a new realtime rich text area element.
     * 
     * @param editor the CKEditor instance that owns this rich text area
     * @param wait whether to wait or not for the content to be editable
     */
    public RealtimeRichTextAreaElement(CKEditor editor, boolean wait)
    {
        super(editor, wait);
    }

    /**
     * @return the position of the coeditors inside the rich text area
     */
    public List<CoeditorPosition> getCoeditorPositions()
    {
        return getFromEditedContent(() -> getDriver().findElementsWithoutWaiting(By.className("realtime-user-position"))
            .stream().map(element -> element.getDomAttribute("id")).map(CoeditorPosition::new).toList());
    }

    /**
     * @param coeditorId the coeditor identifier
     * @return the location where the specified coeditor is currently editing
     */
    public CoeditorPosition getCoeditorPosition(String coeditorId)
    {
        return new CoeditorPosition("rt-user-" + coeditorId, true);
    }

    @Override
    public void waitUntilTextContains(String textFragment)
    {
        repeatedWait(timeout -> waitUntilTextContains(textFragment, timeout));
    }

    @Override
    public void waitUntilContentContains(String html)
    {
        repeatedWait(timeout -> waitUntilContentContains(html, timeout));
    }

    /**
     * Wait for local changes to be pushed to the server. This only guarantees that the server has aknowledged receiving
     * the changes, not that other users have received them. Use this in tests when you need to force a specific order
     * of changes.
     *
     * @since 17.8.0
     * @since 17.4.5
     * @since 16.10.12
     */
    public void waitUntilLocalChangesArePushed()
    {
        StringBuilder script = new StringBuilder();
        script.append("const name = arguments[0];\n");
        script.append("const callback = arguments[1];\n");
        script.append("const editor = CKEDITOR.instances[name]._realtime.editor;\n");
        // Commit local changes, push to the server and wait for aknowledgement.
        script.append("editor._flushUncommittedWork().finally(callback);\n");

        getDriver().executeAsyncScript(script.toString(), this.editor.getName());
    }

    /**
     * When testing realtime editing we often need to switch between the browser tabs. The problem is that browsers are
     * throttling (slowing down) timers in inactive tabs, and sometimes they can even suspend the inactive tabs. This
     * means we can have a situation where the test is switching the tab very fast, immediately after typing something
     * in the rich text area, before the change event is handled, so before the change is propagated to the other users.
     * Waiting on the other tab for the change to arrive can lead to a timeout in this case. The overcome this problem
     * we use this strategy:
     * <ul>
     * <li>wait for a short period of time (a fraction of the default timeout, e.g. 2s instead of 10)</li>
     * <li>temporarily reactivate each of the existing browser tabs by switching to each of them (to give them a chance
     * to process the change)</li>
     * <li>go back to the initial tab and repeat</li>
     * </ul>
     * Alternatively we could have waited for each change to be propagated when it happened (e.g. each time the user
     * typed something) but we felt this was reducing the realtime editing aspect (making it more a turn-based editing).
     * 
     * @param wait the wait to perform
     */
    private void repeatedWait(IntConsumer wait)
    {
        int interval = 2;
        int repetitions = 5;
        for (int i = 0; i < repetitions - 1; i++) {
            try {
                wait.accept(interval);
                return;
            } catch (Exception e) {
                // Ignore the exception and try again after reactivating each of the browser windows.
            }
            reactivateEachBrowserWindow();
        }
        wait.accept(interval);
    }

    private void reactivateEachBrowserWindow()
    {
        String initialHandle = getDriver().getWindowHandle();
        getDriver().getWindowHandles().forEach(handle -> {
            if (!handle.equals(initialHandle)) {
                getDriver().switchTo().window(handle);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        getDriver().switchTo().window(initialHandle);
    }
}
