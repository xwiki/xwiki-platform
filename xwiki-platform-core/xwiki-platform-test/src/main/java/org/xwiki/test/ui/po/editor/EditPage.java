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
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BasePage;
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

    @FindBy(xpath = "//*[@id = 'tmCurrentEditor']//*[@class = 'tme hastype']")
    protected WebElement selectedEditMenuItem;

    @FindBy(id = "xwikidocsyntaxinput2")
    protected WebElement syntaxIdSelect;

    /**
     * Enumerates the available editors.
     */
    public static enum Editor
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
        this.saveandcontinue.click();

        // Wait until the page is really saved
        waitUntilElementIsVisible(By.xpath("//div[contains(@class,'xnotification-done') and text()='Saved']"));
    }

    public ViewPage clickSaveAndView()
    {
        this.save.click();
        return new ViewPage();
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
        return Editor.valueOf(this.selectedEditMenuItem.getText().toUpperCase());
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
     * {@inheritDoc}
     * <p>
     * Overwrite in order to change the behavior of {@code clickEditXXX} methods in edit mode so that they switch from
     * the current editor to the specified one.
     * 
     * @see org.xwiki.test.ui.po.BasePage#clickContentMenuEditSubMenuEntry(String)
     */
    @Override
    protected void clickContentMenuEditSubMenuEntry(String id)
    {
        hoverOverMenu("tmCurrentEditor");
        getDriver().findElement(By.xpath("//a[@id='" + id + "']")).click();
    }
}
