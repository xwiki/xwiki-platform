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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.LocaleUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BasePage;
import org.xwiki.test.ui.po.BootstrapSelect;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the common actions possible on all Pages when using the "edit" action.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class EditPage extends BasePage
{
    @FindBy(name = "action_saveandcontinue")
    protected WebElement saveandcontinue;

    @FindBy(name = "action_save")
    protected WebElement save;

    @FindBy(name = "action_cancel")
    protected WebElement cancel;

    @FindBy(id = "doAutosave")
    protected WebElement autoSaveCheckbox;

    @FindBy(id = "editcolumn")
    protected WebElement currentEditorDiv;

    @FindBy(id = "xwikidocsyntaxinput2")
    protected WebElement syntaxIdSelect;

    @FindBy(name = "parent")
    private WebElement parentInput;

    @FindBy(id = "xwikidoctitleinput")
    private WebElement titleField;

    @FindBy(id = "xwikidoclanguageinput2")
    private WebElement defaultLanguageField;

    /**
     * The top floating edit menu bar.
     */
    @FindBy(id = "editmenu")
    private WebElement editMenuBar;

    /**
     * The entry on the edit menu bar that displays the current editor and allows us to switch the editor.
     */
    @FindBy(id = "tmCurrentEditor")
    private WebElement currentEditorMenu;

    @FindBy(id = "csrf-warning-modal")
    private WebElement csrfWarningModal;

    @FindBy(id = "cancel-save-csrf")
    private WebElement cancelCSRFWarningButton;

    @FindBy(id = "force-save-csrf")
    private WebElement forceSaveCSRFButton;

    /**
     * Enumerates the available editors.
     */
    public enum Editor
    {
        WYSIWYG("WYSIWYG"),
        WIKI("Wiki"),
        RIGHTS("Access Rights"),
        OBJECT("Objects"),
        CLASS("Class");

        /**
         * The mapping between pretty names and editors.
         */
        private static final Map<String, Editor> BY_PRETTY_NAME = new HashMap<String, Editor>();

        static {
            // NOTE: We cannot refer to a static enum field within the initializer because enums are initialized before
            // any static initializers are run so we are forced to use a static block to build the map.
            for (Editor editor : values()) {
                BY_PRETTY_NAME.put(editor.getPrettyName(), editor);
            }
        }

        /**
         * The string used to display the name of the editor on the edit menu.
         */
        private final String prettyName;

        /**
         * Defines a new editor with the given pretty name.
         *
         * @param prettyName the string used to display the name of the editor on the edit menu
         */
        Editor(String prettyName)
        {
            this.prettyName = prettyName;
        }

        /**
         * @return the string used to display the name of the editor on the edit menu
         */
        public String getPrettyName()
        {
            return this.prettyName;
        }

        /**
         * @param prettyName the string used to display the name of the editor on the edit menu
         * @return the editor corresponding to the given pretty name, {@code null} if no editor matches the given pretty
         *         name
         */
        public static Editor byPrettyName(String prettyName)
        {
            return BY_PRETTY_NAME.get(prettyName);
        }
    }

    public void clickSaveAndContinue()
    {
        this.clickSaveAndContinue(true);
    }

    /**
     * Clicks on the Save and Continue button. Use this instead of {@link #clickSaveAndContinue()} when you want to wait
     * for a different message (e.g. an error message).
     *
     * @param wait {@code true} to wait for the page to be saved, {@code false} otherwise
     */
    public void clickSaveAndContinue(boolean wait)
    {
        this.getSaveAndContinueButton().click();

        if (wait) {
            // Wait until the page is really saved.
            waitForNotificationSuccessMessage("Saved");
        }
    }

    /**
     * Use this method instead of {@link #clickSaveAndContinue()} and call {@link WebElement#click()} when you know that
     * the next page is not a standard XWiki {@link InlinePage}.
     *
     * @return the save and continue button used to submit the form.
     */
    public WebElement getSaveAndContinueButton()
    {
        return saveandcontinue;
    }

    public <T extends ViewPage> T clickSaveAndView()
    {
        clickSaveAndView(true);

        return (T) new ViewPage();
    }

    /**
     * Useful when the save and view operation could fail on the client side and a reload (the view part) might thus not
     * take place.
     *
     * @param wait if we should wait for the page to be reloaded
     * @since 7.4M2
     */
    public void clickSaveAndView(boolean wait)
    {
        if (wait) {
            getDriver().addPageNotYetReloadedMarker();
        }

        this.getSaveAndViewButton().click();

        if (wait) {
            // Since we might have a loading step between clicking Save&View and the view page actually loading
            // (specifically when using templates that have child documents associated), we need to wait for the save to
            // finish and for the redirect to occur.
            getDriver().waitUntilPageIsReloaded();
        }
    }

    /**
     * Use this method instead of {@link #clickSaveAndView()} and call {@link WebElement#click()} when you know that the
     * next page is not a standard XWiki {@link InlinePage}.
     *
     * @return the save and view button used to submit the form.
     */
    public WebElement getSaveAndViewButton()
    {
        return save;
    }

    /**
     * @return the checkbox used to toggle auto-save
     */
    public WebElement getAutoSaveCheckbox()
    {
        return this.autoSaveCheckbox;
    }

    public ViewPage clickCancel()
    {
        this.cancel.click();
        return new ViewPage();
    }

    /**
     * @return the editor being used on this page
     */
    public Editor getEditor()
    {
        String editor = "";
        String[] CSSClasses = this.currentEditorDiv.getAttribute("class").split(" ");
        for (String cssClasse : CSSClasses) {
            if (cssClasse.startsWith("editor-")) {
                editor = cssClasse.substring(7);
                break;
            }
        }
        return Editor.valueOf(editor.toUpperCase());
    }

    /**
     * @return the syntax if of the page
     * @since 3.2M3
     */
    public String getSyntaxId()
    {
        return this.syntaxIdSelect.getAttribute("value");
    }

    /**
     * @since 3.2M3
     */
    public void setSyntaxId(String syntaxId)
    {
        Select select = new Select(this.syntaxIdSelect);
        select.selectByValue(syntaxId);
    }

    /**
     * @return the value of the parent field.
     * @since 7.2M2
     */
    public String getParent()
    {
        return this.parentInput.getAttribute("value");
    }

    /**
     * @since 7.2M2
     */
    @Override
    public String getDocumentTitle()
    {
        return this.titleField.getAttribute("value");
    }

    protected Set<Locale> getExistingLocales(List<WebElement> elements)
    {
        Set<Locale> locales = new HashSet<>(elements.size());
        for (WebElement element : elements) {
            locales.add(LocaleUtils.toLocale(element.getText()));
        }

        return locales;
    }

    /**
     * @return a list of the locales already translated for this document
     * @since 9.0RC1
     */
    public Set<Locale> getExistingLocales()
    {
        List<WebElement> elements =
            getDriver().findElementsWithoutWaiting(By.xpath("//p[starts-with(text(), 'Existing translations:')]//a"));

        return getExistingLocales(elements);
    }

    /**
     * @return a list of the supported locales not yet translated for this document
     * @since 9.0RC1
     */
    public Set<Locale> getNotExistingLocales()
    {
        List<WebElement> elements =
            getDriver().findElementsWithoutWaiting(By.xpath("//p[starts-with(text(), 'Translate this page in:')]//a"));

        return getExistingLocales(elements);
    }

    /**
     * @param locale the locale to translate to
     * @return the target locale edit page
     * @since 9.0RC1
     */
    public WikiEditPage clickTranslate(String locale)
    {
        WebElement element;
        if ("default".equals(locale)) {
            element = getDriver().findElement(By.linkText("default"));
        } else {
            element = getDriver().findElementWithoutWaiting(
                By.xpath("//p[starts-with(text(), 'Translate this page in:')]//a[text()='" + locale + "']"));
        }

        element.click();

        return new WikiEditPage();
    }

    /**
     * Set the default language input field.
     * 
     * @param defaultLanguage the string to fill the input.
     * @since 11.3RC1
     */
    public void setDefaultLanguage(String defaultLanguage)
    {
        // Select the parent of the default language field because we're using the Bootstrap select widget.
        WebElement parent = this.defaultLanguageField.findElement(By.xpath("./.."));
        BootstrapSelect select = new BootstrapSelect(parent, getDriver());
        select.selectByValue(defaultLanguage);
    }

    public String getDefaultLanguage()
    {
        return new Select(this.defaultLanguageField).getFirstSelectedOption().getAttribute("value");
    }

    public boolean isCSRFWarningDisplayed()
    {
        try {
            return this.csrfWarningModal.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void clickCancelCSRFWarningButton()
    {
        this.cancelCSRFWarningButton.click();
    }

    public void clickForceSaveCSRFButton()
    {
        this.forceSaveCSRFButton.click();
    }

    /**
     * Cancel the edition by using keyboard shortcut.
     * @return a new {@link ViewPage}
     * @since 11.9RC1
     */
    public ViewPage useShortcutKeyForCancellingEdition()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().createActions().keyDown(Keys.ALT).sendKeys("c").keyUp(Keys.ALT).perform();
        getDriver().waitUntilPageIsReloaded();
        return new ViewPage();
    }
}
