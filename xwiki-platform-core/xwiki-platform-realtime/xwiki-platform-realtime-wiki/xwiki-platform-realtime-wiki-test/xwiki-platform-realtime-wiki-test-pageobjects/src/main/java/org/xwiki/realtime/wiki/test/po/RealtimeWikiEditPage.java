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
package org.xwiki.realtime.wiki.test.po;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.realtime.test.po.RealtimeEditToolbar;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Represents the Realtime Wiki Editor.
 * 
 * @version $Id$
 * @since 15.5.4
 * @since 15.10
 */
public class RealtimeWikiEditPage extends WikiEditPage
{
    private RealtimeEditToolbar toolbar;

    /**
     * Opens the specified wiki page in real-time wiki edit mode.
     *
     * @param pageReference the wiki page to edit in real-time wiki edit mode
     * @return the realtime wiki edit page
     */
    public static RealtimeWikiEditPage gotoPage(EntityReference pageReference)
    {
        WikiEditPage.gotoPage(pageReference);
        return new RealtimeWikiEditPage();
    }

    /**
     * Default constructor.
     */
    public RealtimeWikiEditPage()
    {
        this.toolbar = new RealtimeEditToolbar().waitUntilConnected();
    }

    /**
     * Wait until the editor contains the given text.
     *
     * @param text the text to wait for
     */
    public void waitUntilContentContains(String text)
    {
        getDriver().waitUntilCondition(ExpectedConditions.textToBePresentInElementValue(this.contentText, text));
    }

    @Override
    public void sendKeys(CharSequence... keys)
    {
        // Focus the text area before sending the keys in order to restore the previous selection.
        getDriver().executeScript("arguments[0].focus()", this.contentText);
        super.sendKeys(keys);
    }

    /**
     * @return the edit mode toolbar (holding the button to save the changes)
     * @since 16.10.6
     * @since 17.3.0RC1
     */
    public RealtimeEditToolbar getToolbar()
    {
        return this.toolbar;
    }
}
