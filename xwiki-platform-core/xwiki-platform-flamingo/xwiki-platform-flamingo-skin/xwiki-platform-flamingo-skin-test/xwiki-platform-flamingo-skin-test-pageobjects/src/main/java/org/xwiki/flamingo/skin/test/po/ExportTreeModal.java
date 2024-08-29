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
package org.xwiki.flamingo.skin.test.po;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.test.ui.po.BaseModal;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;

/**
 * Represents the Export Tree Modal.
 * 
 * @version $Id$
 * @since 14.10
 */
public class ExportTreeModal extends BaseModal
{
    private static final By SELECTOR = By.id("exportTreeModal");

    private final TreeElement pageTree;

    /**
     * Default constructor.
     */
    public ExportTreeModal()
    {
        super(SELECTOR);
        WebElement treeContainer = this.container.findElement(By.className("export-tree"));
        this.pageTree = new TreeElement(treeContainer).waitForIt();
        getDriver().waitUntilCondition(ExpectedConditions.attributeToBe(treeContainer, "data-ready", "true"));
    }

    /**
     * Opens the export tree modal for the given page and export format.
     * 
     * @param viewPage the page for which to open the export tree modal
     * @param exportFormat the export format to choose; should be an export format that supports multipage export
     * @return the export tree modal
     */
    public static ExportTreeModal open(ViewPage viewPage, String exportFormat)
    {
        ExportModal.open(viewPage).exportAs(exportFormat);

        return new ExportTreeModal();
    }

    /**
     * @return {@code true} if the export tree modal is present, {@code false} otherwise
     */
    public static boolean isPresent()
    {
        return !getUtil().getDriver().findElements(SELECTOR).isEmpty();
    }

    /**
     * @return the page tree
     */
    public TreeElement getPageTree()
    {
        return this.pageTree;
    }

    /**
     * Click on the export button.
     */
    public void export()
    {
        this.container.findElement(By.cssSelector(".modal-footer button.btn-primary")).click();
    }

    /**
     * @return all the values of the "pages" hidden input fields aggregated in a list
     */
    public List<String> getPagesValues()
    {
        return getValues(getForm().findElements(By.cssSelector("input[type=hidden][name=pages]")));
    }

    /**
     * @return all the values of the "excludes" hidden input fields aggregated in a list
     */
    public List<String> getExcludesValues()
    {
        return getValues(getForm().findElements(By.cssSelector("input[type=hidden][name=excludes]")));
    }

    private List<String> getValues(List<WebElement> webElements)
    {
        return webElements.stream().map(webElement -> webElement.getAttribute("value")).collect(Collectors.toList());
    }

    /**
     * @return the URL where the modal is submitted
     */
    public String getAction()
    {
        return getForm().getAttribute("action");
    }

    private WebElement getForm()
    {
        return getDriver().findElement(By.id("export-modal-form"));
    }

    /**
     * @return the {@link WebElement} that contains the export tree modal
     */
    public WebElement getContainer()
    {
        return this.container;
    }
}
