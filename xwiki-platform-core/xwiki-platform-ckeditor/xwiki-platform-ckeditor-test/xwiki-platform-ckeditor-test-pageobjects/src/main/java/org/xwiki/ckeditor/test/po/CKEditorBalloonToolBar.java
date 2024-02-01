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
package org.xwiki.ckeditor.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents the CKEditor balloon tool bar that provides context actions for the selected element.
 *
 * @version $Id$
 * @since 15.10.6
 * @since 16.0.0RC1
 */
public class CKEditorBalloonToolBar extends CKEditorToolBar
{
    /**
     * Create a new balloon tool bar instance for the given editor.
     * 
     * @param editor the editor that owns the balloon tool bar
     */
    public CKEditorBalloonToolBar(CKEditor editor)
    {
        super(editor);
    }

    @Override
    protected WebElement findContainer(CKEditor editor)
    {
        // Look for the first displayed balloon tool bar. CKEditor keeps all the balloon tool bars in the DOM but
        // displays at most one, depending on the selection.
        return getDriver().findElementsWithoutWaiting(By.className("cke_balloontoolbar")).stream()
            .filter(WebElement::isDisplayed).findFirst().orElseThrow();
    }

    /**
     * Edit the currently focused macro.
     * 
     * @return the macro edit modal
     */
    public MacroDialogEditModal editMacro()
    {
        clickButton("xwiki-macro-edit");
        return new MacroDialogEditModal().waitUntilReady();
    }
}
