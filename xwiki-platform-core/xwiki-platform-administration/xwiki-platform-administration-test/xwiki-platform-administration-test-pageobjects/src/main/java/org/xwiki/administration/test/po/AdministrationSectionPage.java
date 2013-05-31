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
package org.xwiki.administration.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.FormElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents common actions available in all Administration pages.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class AdministrationSectionPage extends ViewPage
{
    @FindBy(xpath = "//input[@type='submit'][@name='formactionsac']")
    private WebElement saveButton;

    // The admin-page-content div is being treated as a form since it may contain multiple forms and we want to be able
    // to access elements in them all.
    @FindBy(xpath = "//div[@id='admin-page-content']")
    private WebElement form;

    private final String section;

    public AdministrationSectionPage(String section)
    {
        this.section = section;
    }

    public static AdministrationSectionPage gotoPage(String section)
    {
        AdministrationSectionPage page = new AdministrationSectionPage(section);
        page.getDriver().get(page.getURL());

        return page;
    }

    public String getURL()
    {
        return getUtil().getURL("XWiki", "XWikiPreferences", "admin", "section=" + this.section);
    }

    public void clickSave()
    {
        this.saveButton.click();
    }

    public FormElement getForm()
    {
        return new FormElement(form);
    }
}
