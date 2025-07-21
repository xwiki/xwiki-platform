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
}
