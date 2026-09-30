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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.Select;

/**
 * The "Presentation" administration section.
 *
 * @version $Id$
 * @since 17.7.0RC1
 * @since 17.4.3
 * @since 16.10.10
 */
public class PresentationAdministrationSectionPage extends AdministrationSectionPage
{
    private static final String VALUE = "value";

    /**
     * Enumeration representing the options for showing or hiding tabs in the administration section.
     */
    public enum ShowTabValue
    {
        /**
         * Show the tab.
         */
        YES("1"),
        /**
         * Hide the tab.
         */
        NO("0"),
        /**
         * Default value.
         */
        DEFAULT("");

        private final String value;

        ShowTabValue(String value)
        {
            this.value = value;
        }

        /**
         * @return the value of the selected option in the select element
         */
        public String getValue()
        {
            return this.value;
        }

        /**
         * Returns the {@link ShowTabValue} corresponding to the given string value.
         *
         * @param value the string value to match
         * @return the matching {@link ShowTabValue}
         * @throws IllegalArgumentException if no matching {@link ShowTabValue} is found
         */
        public static ShowTabValue fromString(String value)
        {
            for (ShowTabValue tabValue : ShowTabValue.values()) {
                if (tabValue.value.equals(value)) {
                    return tabValue;
                }
            }

            throw new IllegalArgumentException("No ShowTabValue found for value: " + value);
        }
    }

    @FindBy(id = "XWiki.XWikiPreferences_0_showdocumenttabs")
    private WebElement showDocumentTabs;

    @FindBy(id = "XWiki.XWikiPreferences_0_showannotations")
    private WebElement showAnnotations;

    @FindBy(id = "XWiki.XWikiPreferences_0_showcomments")
    private WebElement showComments;

    @FindBy(id = "XWiki.XWikiPreferences_0_showattachments")
    private WebElement showAttachments;

    @FindBy(id = "XWiki.XWikiPreferences_0_showhistory")
    private WebElement showHistory;

    @FindBy(id = "XWiki.XWikiPreferences_0_showinformation")
    private WebElement showInformation;

    @FindBy(id = "XWiki.XWikiPreferences_0_webcopyright")
    private WebElement copyright;

    @FindBy(id = "XWiki.XWikiPreferences_0_version")
    private WebElement version;

    /**
     * Default constructor.
     */
    public PresentationAdministrationSectionPage()
    {
        super("Presentation");
    }

    /**
     * @return the value of the "Show Annotations" option
     */
    public ShowTabValue getShowAnnotations()
    {
        return ShowTabValue.fromString(new Select(this.showAnnotations).getFirstSelectedOption().getAttribute(VALUE));
    }

    /**
     * @param value the value to set for the "Show Annotations" option
     */
    public void setShowAnnotations(ShowTabValue value)
    {
        new Select(this.showAnnotations).selectByValue(value.getValue());
    }

    /**
     * @return the value of the "Show Comments" option
     */
    public ShowTabValue getShowComments()
    {
        return ShowTabValue.fromString(new Select(this.showComments).getFirstSelectedOption().getAttribute(VALUE));
    }

    /**
     * @param value the value to set for the "Show Comments" option
     */
    public void setShowComments(ShowTabValue value)
    {
        new Select(this.showComments).selectByValue(value.getValue());
    }

    /**
     * @return the value of the "Show Attachments" option
     */
    public ShowTabValue getShowAttachments()
    {
        return ShowTabValue.fromString(new Select(this.showAttachments).getFirstSelectedOption().getAttribute(VALUE));
    }

    /**
     * @param value the value to set for the "Show Attachments" option
     */
    public void setShowAttachments(ShowTabValue value)
    {
        new Select(this.showAttachments).selectByValue(value.getValue());
    }

    /**
     * @return the value of the "Show History" option
     */
    public ShowTabValue getShowHistory()
    {
        return ShowTabValue.fromString(new Select(this.showHistory).getFirstSelectedOption().getAttribute(VALUE));
    }

    /**
     * @param value the value to set for the "Show History" option
     */
    public void setShowHistory(ShowTabValue value)
    {
        new Select(this.showHistory).selectByValue(value.getValue());
    }

    /**
     * @return the value of the "Show Information" option
     */
    public ShowTabValue getShowInformation()
    {
        return ShowTabValue.fromString(new Select(this.showInformation).getFirstSelectedOption().getAttribute(VALUE));
    }

    /**
     * @param value the value to set for the "Show Information" option
     */
    public void setShowInformation(ShowTabValue value)
    {
        new Select(this.showInformation).selectByValue(value.getValue());
    }

    /**
     * @return the value of the "Show page tabs" option, which hides the whole tab area when set to
     *         {@link ShowTabValue#NO}
     * @since 18.9.0RC1
     */
    public ShowTabValue getShowDocumentTabs()
    {
        return ShowTabValue.fromString(new Select(this.showDocumentTabs).getFirstSelectedOption().getAttribute(VALUE));
    }

    /**
     * @param value the value to set for the "Show page tabs" option
     * @since 18.9.0RC1
     */
    public void setShowDocumentTabs(ShowTabValue value)
    {
        new Select(this.showDocumentTabs).selectByValue(value.getValue());
    }

    /**
     * @param tabId the identifier of a tab contributed to the {@code org.xwiki.plaftorm.template.docextra} extension
     *            point
     * @return the value of the field displayed for that tab
     * @since 18.9.0RC1
     */
    public ShowTabValue getCustomTabVisibility(String tabId)
    {
        String value = new Select(getCustomTabField(tabId)).getFirstSelectedOption().getAttribute(VALUE);
        if (value.isEmpty()) {
            return ShowTabValue.DEFAULT;
        }

        return value.startsWith("+") ? ShowTabValue.YES : ShowTabValue.NO;
    }

    /**
     * @param tabId the identifier of a tab contributed to the {@code org.xwiki.plaftorm.template.docextra} extension
     *            point
     * @param value the visibility to set for that tab
     * @since 18.9.0RC1
     */
    public void setCustomTabVisibility(String tabId, ShowTabValue value)
    {
        Select select = new Select(getCustomTabField(tabId));
        switch (value) {
            case YES -> select.selectByValue('+' + tabId);
            case NO -> select.selectByValue('-' + tabId);
            default -> select.selectByValue("");
        }
    }

    /**
     * Click the "Show all tabs" button, which sets every tab field of the section to "Yes".
     *
     * @since 18.9.0RC1
     */
    public void showAllTabs()
    {
        getDriver().findElement(By.cssSelector("[data-documenttabs-bulk='show']")).click();
    }

    /**
     * Click the "Hide all tabs" button, which sets every tab field of the section to "No".
     *
     * @since 18.9.0RC1
     */
    public void hideAllTabs()
    {
        getDriver().findElement(By.cssSelector("[data-documenttabs-bulk='hide']")).click();
    }

    /**
     * @return {@code true} when the section warns that the per tab settings are not applied
     * @since 18.9.0RC1
     */
    public boolean isHiddenNoticeDisplayed()
    {
        return getDriver().findElement(By.cssSelector(".documentTabsHiddenNotice")).isDisplayed();
    }

    private WebElement getCustomTabField(String tabId)
    {
        List<WebElement> fields = getDriver().findElements(By.cssSelector("select.documentTabVisibility"));
        for (WebElement field : fields) {
            if (!field.findElements(By.cssSelector(String.format("option[value='+%s']", tabId))).isEmpty()) {
                return field;
            }
        }

        throw new IllegalArgumentException("No document tab field found for tab: " + tabId);
    }

    /**
     * @param value the value to set for the "Copyright" option
     */
    public void setCopyright(String value)
    {
        this.copyright.clear();
        this.copyright.sendKeys(value);
    }

    /**
     * @param value the value to set for the "Version" option
     */
    public void setVersion(String value)
    {
        this.version.clear();
        this.version.sendKeys(value);
    }
}
