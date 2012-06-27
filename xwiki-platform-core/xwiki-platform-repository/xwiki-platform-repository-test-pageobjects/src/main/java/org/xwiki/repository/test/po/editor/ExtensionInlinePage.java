package org.xwiki.repository.test.po.editor;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.repository.test.po.ExtensionPage;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.editor.wysiwyg.EditorElement;
import org.xwiki.test.ui.po.editor.wysiwyg.RichTextAreaElement;

/**
 * @version $Id$
 * @since 3.3M1
 */
public class ExtensionInlinePage extends InlinePage
{
    @FindBy(id = "ExtensionCode.ExtensionClass_0_name")
    private WebElement nameInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_type")
    private WebElement typeInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_summary")
    private WebElement summaryInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_authors")
    private WebElement authorsInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_licenseName")
    private WebElement licenseNameList;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_source")
    private WebElement sourceInput;

    @FindBy(id = "ExtensionCode.ExtensionClass_0_icon")
    private WebElement iconInput;

    private final EditorElement descriptionEditor = new EditorElement("ExtensionCode.ExtensionClass_0_description");

    @FindBy(id = "ExtensionCode.ExtensionClass_0_customInstallationOnly")
    private WebElement customInstallationOnlyCheckBox;

    private final EditorElement installationEditor = new EditorElement("ExtensionCode.ExtensionClass_0_installation");

    public void setName(String name)
    {
        this.nameInput.clear();
        this.nameInput.sendKeys(name);
    }

    public String getName()
    {
        return this.nameInput.getAttribute("value");
    }

    public void setType(String type)
    {
        Select select = new Select(this.typeInput);
        select.selectByValue(type);
    }

    public void setSummary(String summary)
    {
        this.summaryInput.clear();
        this.summaryInput.sendKeys(summary);
    }

    public void setAuthors(String author)
    {
        this.authorsInput.clear();
        this.authorsInput.sendKeys(author);
    }

    public void setLicenseName(String licenseName)
    {
        Select select = new Select(this.licenseNameList);
        select.selectByValue(licenseName);
    }

    public void setSource(String source)
    {
        this.sourceInput.clear();
        this.sourceInput.sendKeys(source);
    }

    public void setIcon(String icon)
    {
        this.iconInput.clear();
        this.iconInput.sendKeys(icon);
    }

    public void setDescription(String description)
    {
        RichTextAreaElement richTextArea = this.descriptionEditor.waitToLoad().getRichTextArea();
        richTextArea.clear();
        richTextArea.sendKeys(description);
    }

    public void setCustomInstallationOnly(boolean customInstallationOnly)
    {
        Select select = new Select(this.customInstallationOnlyCheckBox);
        select.selectByValue(customInstallationOnly ? "1" : "0");
    }

    public void setInstallation(String installation)
    {
        RichTextAreaElement richTextArea = this.installationEditor.waitToLoad().getRichTextArea();
        richTextArea.clear();
        richTextArea.sendKeys(installation);
    }

    @Override
    protected ExtensionPage createViewPage()
    {
        return new ExtensionPage();
    }
}
