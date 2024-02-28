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
package org.xwiki.test.ui.po;

import java.util.List;
import java.util.Locale;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * PO for Page Information pane.
 *
 * @since 11.10
 * @version $Id$
 */
public class InformationPane extends BaseElement
{
    private static final By ORIGINAL_LOCALE_SELECTOR = By.cssSelector("dd[data-key='originalLocale']");

    @FindBy(id = "informationcontent")
    private WebElement pane;

    public boolean isOpened()
    {
        return this.pane.isDisplayed();
    }

    /**
     * @return the locale of the current page as shown on the Information tab
     * @since 12.10.6
     * @since 13.2RC1
     */
    public String getLocale()
    {
        return this.pane.findElement(By.cssSelector("dd[data-key='locale']")).getText();
    }

    /**
     * @return {@code true} if the Information tab says that the original locale of the current page is currently being
     *         viewed, {@code false} otherwise
     * @since 12.10.6
     * @since 13.2RC1
     */
    public boolean isOriginalLocale()
    {
        return getDriver().findElementsWithoutWaiting(this.pane, ORIGINAL_LOCALE_SELECTOR).isEmpty();
    }

    /**
     * @return the original locale of the current page as shown on the Information tab
     * @since 12.10.6
     * @since 13.2RC1
     */
    public String getOriginalLocale()
    {
        return this.pane.findElement(ORIGINAL_LOCALE_SELECTOR).getText();
    }

    /**
     * @return the list of supported locales the current page is translated into, as shown by the Information tab
     * @since 12.10.6
     * @since 13.2RC1
     */
    public List<String> getAvailableTranslations()
    {
        return getDriver().findElementsWithoutWaiting(this.pane, By.cssSelector("dd[data-key='translations'] > a"))
            .stream().map(link -> link.getText()).toList();
    }

    /**
     * @return the list of supported locales for which the current page doesn't have a translation yet
     * @since 12.10.6
     * @since 13.2RC1
     */
    public List<String> getMissingTranslations()
    {
        return getDriver()
            .findElementsWithoutWaiting(this.pane, By.cssSelector("dd[data-key='translations'] .wikicreatelink a"))
            .stream().map(link -> link.getText()).toList();
    }

    /**
     * Clicks on the translation link that corresponds to the given locale.
     * 
     * @param locale the locale to click on
     * @since 12.10.6
     * @since 13.2RC1
     */
    public void clickTranslationLink(Locale locale)
    {
        this.pane.findElement(By.cssSelector("a[data-locale='" + locale + "']")).click();
    }

    /**
     * Clicks on the translation link with the specified label (locale pretty name).
     * 
     * @param label the locale pretty name
     * @since 12.10.6
     * @since 13.2RC1
     */
    public void clickTranslationLink(String label)
    {
        this.pane.findElement(By.xpath(".//a[@data-locale and . = '" + label + "']")).click();
    }

    public String getSyntax()
    {
        return this.pane.findElement(By.xpath(".//label[. = 'Syntax']/parent::dt/following-sibling::dd")).getText();
    }

    public DocumentSyntaxPropertyPane editSyntax()
    {
        return new DocumentSyntaxPropertyPane().clickEdit();
    }
}
