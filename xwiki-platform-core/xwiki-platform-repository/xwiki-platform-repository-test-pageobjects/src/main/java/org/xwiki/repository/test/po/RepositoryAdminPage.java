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
package org.xwiki.repository.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.AdministrationSectionPage;

/**
 * @version $Id$
 * @since 4.2M1
 */
public class RepositoryAdminPage extends AdministrationSectionPage
{
    public static final String ADMINISTRATION_SECTION_ID = "Repository";

    @FindBy(id = "ExtensionCode.RepositoryConfigClass_0_defaultIdPrefix")
    private WebElement defaultIdPrefix;

    @FindBy(xpath = "//input[@type='submit'][@name='action_saveandcontinue']")
    private WebElement updateButton;

    public static RepositoryAdminPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(ADMINISTRATION_SECTION_ID);
        return new RepositoryAdminPage();
    }

    public RepositoryAdminPage()
    {
        super(ADMINISTRATION_SECTION_ID);
    }

    public void setDefaultIdPrefix(String defaultIdPrefix)
    {
        this.defaultIdPrefix.clear();
        this.defaultIdPrefix.sendKeys(defaultIdPrefix);
    }

    public String getDefaultIdPrefix()
    {
        return this.defaultIdPrefix.getAttribute("value");
    }

    public void clickUpdateButton()
    {
        this.updateButton.click();
    }
}
