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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

/**
 * Represents the Export administration section (Administer Wiki &gt; Content &gt; Export).
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class ExportAdministrationSectionPage extends AdministrationSectionPage
{
    /**
     * The filters that can be applied on the export tree, from the "Select from" drop-down.
     *
     * @since 18.9.0RC1
     */
    public enum Filter
    {
        /**
         * All pages.
         */
        ALL_PAGES(""),

        /**
         * Created pages: the pages that don't belong to an installed extension.
         */
        CREATED_PAGES("installedExtensionDocument"),

        /**
         * Created and modified pages: the created pages plus the extension pages that have been modified.
         */
        CREATED_AND_MODIFIED_PAGES("pristineInstalledExtensionDocument");

        private final String id;

        Filter(String id)
        {
            this.id = id;
        }

        /**
         * @return the filter id, as sent in the {@code filter} parameter of the export request
         */
        public String getId()
        {
            return this.id;
        }
    }

    private static final String SECTION_ID = "Export";

    private static final String EXPORT_PARAMETERS_ATTRIBUTE = "data-export-parameters";

    private static final String CLASS_ATTRIBUTE = "class";

    @FindBy(id = "export")
    private WebElement form;

    @FindBy(css = "form#export .buttons input[type='submit']")
    private WebElement exportButton;

    @FindBy(id = "targetXWikiVersion")
    private WebElement targetXWikiVersionSelect;

    @FindBy(css = "form#export input#name")
    private WebElement packageNameInput;

    /**
     * Default constructor, waiting for the export tree to be ready.
     */
    public ExportAdministrationSectionPage()
    {
        super(SECTION_ID);
        // The export button is enabled once the export tree is loaded and has pages selected.
        getDriver().waitUntilCondition(ExpectedConditions.elementToBeClickable(this.exportButton));
    }

    /**
     * @return the Export administration section of the current wiki
     */
    public static ExportAdministrationSectionPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION_ID);
        return new ExportAdministrationSectionPage();
    }

    /**
     * @return the tree used to select the pages to export
     */
    public TreeElement getPageTree()
    {
        return new TreeElement(this.form.findElement(By.className("export-tree"))).waitForIt();
    }

    /**
     * Select the filter applied on the export tree (the "Select from" drop-down) and wait for the tree to be reloaded.
     *
     * @param filter the filter to apply
     */
    public void setFilter(Filter filter)
    {
        WebElement filterElement = this.form.findElement(By.className("export-tree-filter"));
        WebElement tree = this.form.findElement(By.className("export-tree"));
        // The tree is refreshed asynchronously when the filter changes: mark the tree as soon as the refresh is done.
        getDriver().executeScript("const tree = jQuery(arguments[0]); tree.attr('data-filter-applied', 'false')"
            + ".one('refresh.jstree', () => tree.attr('data-filter-applied', 'true'));", tree);
        WebElement toggle = filterElement.findElement(By.className("dropdown-toggle"));
        toggle.click();
        filterElement.findElement(By.cssSelector(String.format("a[data-filter='%s']", filter.getId()))).click();
        getDriver().waitUntilCondition(ExpectedConditions.attributeToBe(tree, "data-filter-applied", "true"));
        // The drop-down stays open after choosing a filter, hiding the top of the tree.
        if (filterElement.getDomAttribute(CLASS_ATTRIBUTE).contains("open")) {
            toggle.click();
            getDriver().waitUntilCondition(
                driver -> !filterElement.getDomAttribute(CLASS_ATTRIBUTE).contains("open"));
        }
        getPageTree();
    }

    /**
     * Open the given node and use its "Unselect all children" context menu entry.
     *
     * @param node the export tree node whose child nodes should be unselected
     */
    public void unselectAllChildren(TreeNodeElement node)
    {
        // The context menu entry is disabled as long as the node is closed.
        node.open().waitForIt();
        getDriver().createActions().contextClick(node.getLabelElement()).perform();
        By menuEntry = By.xpath("//ul[contains(@class, 'vakata-context')]//a[contains(., 'Unselect all children')]");
        getDriver().findElement(menuEntry).click();
        getDriver().waitUntilElementDisappears(menuEntry);
    }

    /**
     * @param versionRange the value of the target XWiki version option, e.g. {@code [12.0,)} for "XWiki 12.0 and
     *     later" or {@code (,12.0)} for "Before XWiki 12.0"
     */
    public void setTargetXWikiVersion(String versionRange)
    {
        new Select(this.targetXWikiVersionSelect).selectByValue(versionRange);
    }

    /**
     * @param packageName the name of the exported package (without the {@code .xar} extension)
     */
    public void setPackageName(String packageName)
    {
        this.packageNameInput.clear();
        this.packageNameInput.sendKeys(packageName);
    }

    /**
     * Click the Export button and return the parameters of the export request sent by the browser. The browser saves
     * the exported package in its download directory, out of reach of the test, so tests should send the returned
     * parameters to the export action themselves to get the package content.
     *
     * @return the parameters of the export request (name to values, in the order they are submitted)
     */
    public Map<String, List<String>> export()
    {
        // The export tree adds the selected pages as hidden inputs from its own submit listener. This listener is
        // registered after it, so it sees the complete form.
        getDriver().executeScript("const form = jQuery(arguments[0]); const attribute = arguments[1];"
            + "form.removeAttr(attribute).one('submit', function() {"
            + "  form.attr(attribute, JSON.stringify(Array.from(new FormData(this))));"
            + "});", this.form, EXPORT_PARAMETERS_ATTRIBUTE);
        this.exportButton.click();
        getDriver().waitUntilCondition(driver -> this.form.getDomAttribute(EXPORT_PARAMETERS_ATTRIBUTE) != null);

        @SuppressWarnings("unchecked")
        List<List<String>> entries = (List<List<String>>) getDriver()
            .executeScript("return JSON.parse(arguments[0].getAttribute(arguments[1]));", this.form,
                EXPORT_PARAMETERS_ATTRIBUTE);
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        for (List<String> entry : entries) {
            parameters.computeIfAbsent(entry.get(0), key -> new ArrayList<>()).add(entry.get(1));
        }
        return parameters;
    }
}
