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
package org.xwiki.test.ui.po.editor;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Represents the actions possible in WYSIWYG edit mode.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class WYSIWYGEditPage extends PreviewableEditPage
{
    /**
     * The content text area.
     * <p>
     * TODO: We need a common interface for WYSIWYG editors and a way to load the current implementation without
     * hard-coding it (similar to a component).
     */
    @FindBy(id = "content")
    private WebElement contentTextArea;

    /**
     * Go to the passed page in WYSIWYG edit mode.
     */
    public static WYSIWYGEditPage gotoPage(String space, String page)
    {
        return gotoPage(new LocalDocumentReference(space, page));
    }

    /**
     * Go to the passed page in WYSIWYG edit mode.
     */
    public static WYSIWYGEditPage gotoPage(EntityReference targetPageReference)
    {
        getUtil().gotoPage(targetPageReference, "edit", "editor=wysiwyg");
        return new WYSIWYGEditPage();
    }

    /**
     * Get the <code>content</code> of the page.
     */
    public String getContent()
    {
        return this.contentTextArea.getText();
    }

    /**
     * Sets the content of the page.
     * 
     * @param content the content to be set
     */
    public void setContent(String content)
    {
        this.contentTextArea.clear();
        this.contentTextArea.sendKeys(content);
    }
}
