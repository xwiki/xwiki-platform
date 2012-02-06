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
package org.xwiki.test.ui.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the common actions possible on all Pages when using the "inline" action.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class InlinePage extends ViewPage
{
    @FindBy(name = "action_preview")
    private WebElement preview;

    @FindBy(name = "action_saveandcontinue")
    private WebElement saveandcontinue;

    @FindBy(name = "action_save")
    private WebElement save;

    @FindBy(name = "action_cancel")
    private WebElement cancel;

    @FindBy(id = "inline")
    private WebElement form;

    public void clickPreview()
    {
        preview.click();
    }

    public void clickSaveAndContinue()
    {
        saveandcontinue.click();
    }

    public <T extends ViewPage> T clickSaveAndView()
    {
        save.click();
        return createViewPage();
    }

    public <T extends ViewPage> T clickCancel()
    {
        cancel.click();
        return createViewPage();
    }

    /**
     * Can be overridden to return extended {@link ViewPage}.
     */
    protected <T extends ViewPage> T createViewPage()
    {
        return (T) new ViewPage();
    }

    @Override
    public String getContent()
    {
        return form.getText();
    }

    /**
     * @return the form element
     */
    protected WebElement getForm()
    {
        return form;
    }
}
