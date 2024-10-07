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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the Administration Import Page.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class ImportAdministrationSectionPage extends ViewPage
{
    @FindBy(id = "packagelistcontainer")
    private WebElement packageList;

    @FindBy(id = "xwikiuploadfile")
    private WebElement uploadFileInputField;

    @FindBy(xpath = "//*[@id='attachform']//input[@type='submit']")
    private WebElement uploadFileSubmit;

    @FindBy(xpath = "//input[@value='Import']")
    private WebElement importPackageLink;

    public static ImportAdministrationSectionPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "import", "editor=globaladmin&section=Import");
        return new ImportAdministrationSectionPage();
    }

    public void attachPackage(URL file)
    {
        attachPackage(file.getPath());
    }

    /**
     * @since 12.1RC1
     * @since 11.10.4
     */
    public void attachPackage(File file)
    {
        attachPackage(file.getAbsolutePath());
    }

    /**
     * @since 12.1RC1
     * @since 11.10.4
     */
    public void attachPackage(String file)
    {
        this.uploadFileInputField.sendKeys(file);
    }

    public boolean isPackagePresent(String packageName)
    {
        return getPackageNames().contains(packageName);
    }

    public List<String> getPackageNames()
    {
        List<String> names = new ArrayList<String>();
        for (WebElement element : getDriver()
            .findElementsWithoutWaiting(By.xpath("//div[@id='packagelistcontainer']//a[@class='package']"))) {
            names.add(element.getText());
        }
        return names;
    }

    public void selectPackage(String packageName)
    {
        getDriver().findElement(By.linkText(packageName)).click();
        getDriver().waitUntilElementIsVisible(By.id("packageDescription"));
    }

    public void deletePackage(String packageName)
    {
        String xpath = "//ul[@class='xlist']//a[@class='package' and contains(.,'%s')]/..//a[@class='deletelink']";
        this.packageList.findElement(By.xpath(String.format(xpath, packageName))).click();
        // Click on ok button
        ConfirmationModal confirmationModal = new ConfirmationModal();
        confirmationModal.clickOk();
        getDriver()
            .waitUntilElementIsVisible(By.xpath("//div[contains(@class,'xnotification-done') and text()='Done!']"));
        getDriver().findElement(By.xpath("//div[contains(@class,'xnotification-done') and text()='Done!']")).click();
    }

    public void importPackage()
    {
        // Click submit
        this.importPackageLink.click();
        // Wait for the "Import successful message"
        getDriver().waitUntilElementIsVisible(By.xpath(
            "//div[@id='packagecontainer']/div[contains(@class, 'infomessage')]"));
    }

    public ViewPage clickImportedPage(String pageName)
    {
        getDriver().waitUntilElementIsVisible(By.linkText(pageName));
        getDriver().findElement(By.linkText(pageName)).click();
        return new ViewPage();
    }

    public void selectReplaceHistoryOption()
    {
        getDriver().findElement(By.xpath("//input[@name='historyStrategy' and @value='replace']")).click();
    }

    /**
     * @since 11.10.4
     * @since 12.1RC1
     */
    public WebElement getImportAsBackupElement()
    {
        return getDriver().findElement(By.name("importAsBackup"));
    }

    /**
     * @since 11.10.4
     * @since 12.1RC1
     */
    public boolean isImportAsBackup()
    {
        return getImportAsBackupElement().isSelected();
    }

    /**
     * @since 11.10.4
     * @since 12.1RC1
     */
    public boolean clickImportAsBackup()
    {
        WebElement importAsBackup = getImportAsBackupElement();

        importAsBackup.click();

        return importAsBackup.isSelected();
    }
}
