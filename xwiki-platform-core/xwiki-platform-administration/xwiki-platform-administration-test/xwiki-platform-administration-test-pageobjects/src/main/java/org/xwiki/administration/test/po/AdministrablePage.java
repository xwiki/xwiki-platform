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
import org.xwiki.test.ui.po.ViewPage;

/**
 * Extends the ViewPage to add support for Administration action.
 *
 * @version $Id$
 * @since 4.3M1
 */
public class AdministrablePage extends ViewPage
{
    @FindBy(id = "tmAdminWiki")
    private WebElement administerWikiLink;

    /**
     * Clicks on the "Administer Wiki" menu item.
     *
     * @return the wiki administration PO
     */
    public AdministrationPage clickAdministerWiki()
    {
        getDrawerMenu().toggle();
        this.administerWikiLink.click();
        return new AdministrationPage();
    }

    /**
     * Clicks on the "Administer Page" menu item.
     *
     * @return the page administration PO
     * @since 12.10
     */
    public AdministrationPage clickAdministerPage()
    {
        clickMoreActionsSubMenuEntry("tmAdminSpace");
        return new AdministrationPage();
    }
}
