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
    @FindBy(id = "ExtensionCode.RepositoryConfigClass_0_defaultIdPrefix")
    private WebElement defaultIdPrefix;

    @FindBy(xpath = "//input[@type='submit'][@name='action_saveandcontinue']")
    private WebElement updateButton;

    public static RepositoryAdminPage gotoPage()
    {
        RepositoryAdminPage page = new RepositoryAdminPage();
        page.getDriver().get(page.getURL());

        return page;
    }

    public RepositoryAdminPage()
    {
        super("Repository");
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
