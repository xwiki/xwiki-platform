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
package org.xwiki.extension.security.test.po;

import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page Object for the extension vulnerabilities administration page.
 *
 * @version $Id$
 * @since 15.5
 */
public class ExtensionVulnerabilitiesAdminPage extends ViewPage
{
    /**
     * Go to the extension vulnerabilities section of the administration.
     *
     * @return the corresponding page object
     */
    public static ExtensionVulnerabilitiesAdminPage getToExtensionVulnerabilitiesAdmin()
    {
        getUtil().gotoPage(new LocalDocumentReference("XWiki", "XWikiPreferences"), "admin", Map.of(
            "editor", "globaladmin",
            "section", "XWiki.Extension.Security.Code.Admin"
        ));

        return new ExtensionVulnerabilitiesAdminPage();
    }

    /**
     * Set the url of the scan url field in the configuration section.
     *
     * @param url the url to set to the scan url field
     * @return the current page object
     */
    public ExtensionVulnerabilitiesAdminPage setScanURL(String url)
    {
        WebElement element = getDriver().findElement(By.id("XWiki.Extension.Security.Code.ConfigClass_0_scanURL"));
        element.clear();
        element.sendKeys(url);
        return this;
    }

    /**
     * Save the configuration by clicking on the save button of the configuration form.
     */
    public void saveConfig()
    {
        getDriver().findElement(By.cssSelector("form input.button")).click();
    }

    /**
     * @return the page object for the live data of the page
     */
    public LiveDataElement getLiveData()
    {
        return new LiveDataElement("extension-vulnerabilities-list");
    }
}
