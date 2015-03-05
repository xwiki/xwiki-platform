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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Displays the differences between two versions of a document.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ChangesPane extends BaseElement
{
    /**
     * The element that wraps all the changes.
     */
    @FindBy(id = "changescontent")
    private WebElement container;

    /**
     * The summary of the older version
     */
    @FindBy(id = "changes-info-box-from")
    private WebElement fromVersionSummary;

    /**
     * The summary of the newer version.
     */
    @FindBy(id = "changes-info-box-to")
    private WebElement toVersionSummary;

    /**
     * The comment for the to version.
     */
    @FindBy(id = "changes-info-comment")
    private WebElement changeComment;

    /**
     * @return the summary of the from version
     */
    public String getFromVersionSummary()
    {
        return fromVersionSummary.getText();
    }

    /**
     * @return the summary of the to version
     */
    public String getToVersionSummary()
    {
        return toVersionSummary.getText();
    }

    /**
     * @return the comment of the to version
     */
    public String getChangeComment()
    {
        return changeComment.getText();
    }

    /**
     * @return the list of meta data properties that have been changed
     */
    public List<String> getChangedMetaData()
    {
        By xpath = By.xpath("dl[preceding-sibling::*[1][. = 'Metadata changes']]/dt");
        return getText(getDriver().findElementsWithoutWaiting(container, xpath));
    }

    /**
     * @param propertyLabel the label of a document meta data property
     * @return the changes displayed either as a unified diff (for multi-line values) or as an in-line diff (for
     *         single-line values)
     */
    public String getMetaDataChanges(String propertyLabel)
    {
        String xpath =
            ".//dd[preceding-sibling::dt[1][. = '" + propertyLabel + "'] and "
                + "parent::dl[preceding-sibling::*[1][. = 'Metadata changes']]]/*[1]";
        return getDiff(getDriver().findElementWithoutWaiting( container, By.xpath(xpath)));
    }

    /**
     * @param element an element displaying either an inline diff or a unified diff
     * @return the diff text
     */
    private String getDiff(WebElement element)
    {
        if ("diff-line".equals(element.getAttribute("class"))) {
            return (String) getDriver().executeJavascript("return arguments[0].innerHTML", element);
        } else {
            By xpath = By.xpath(".//td[3]");
            StringBuilder diff = new StringBuilder();
            for (WebElement line : getDriver().findElementsWithoutWaiting(element, xpath)) {
                if (diff.length() > 0) {
                    diff.append('\n');
                }
                if (getDriver().findElementsWithoutWaiting(line, By.tagName("ins")).size() > 0
                    || getDriver().findElementsWithoutWaiting(line, By.tagName("del")).size() > 0) {
                    diff.append(getDriver().executeJavascript("return arguments[0].innerHTML", line));
                } else {
                    diff.append(line.getText());
                }
            }
            return diff.toString();
        }
    }

    /**
     * @return the content changes displayed as a unified diff
     */
    public String getContentChanges()
    {
        By xpath = By.xpath("div[@class = 'diff-container' and preceding-sibling::*[1][. = 'Content changes']]");
        List<WebElement> found = getDriver().findElementsWithoutWaiting(container, xpath);
        return found.size() > 0 ? getDiff(found.get(0)) : null;
    }

    /**
     * @return the list of attachment changes
     */
    public List<String> getAttachmentChanges()
    {
        By xpath = By.xpath("ul[preceding-sibling::*[1][. = 'Attachment changes']]/li");
        return getText(getDriver().findElementsWithoutWaiting(container, xpath));
    }

    /**
     * @param commentNumber the comment number
     * @param propertyLabel the label of a comment property (e.g. 'Author', 'Date', 'Comment content')
     * @return the changes displayed either as a unified diff (for multi-line values) or as an in-line diff (for
     *         single-line values)
     */
    public String getCommentChanges(int commentNumber, String propertyLabel)
    {
        String xpath =
            ".//dd[preceding-sibling::dt[1][. = '" + propertyLabel + "'] and "
                + "parent::dl[preceding-sibling::*[1][starts-with(., 'Comment number " + commentNumber + "')]]]/*[1]";
        return getDiff(getDriver().findElementWithoutWaiting(container, By.xpath(xpath)));
    }

    /**
     * @return the list of summaries for comment changes
     */
    public List<String> getCommentChangeSummaries()
    {
        By xpath = By.xpath("*[starts-with(name(), 'H') and starts-with(., 'Comment number ')]");
        return getText(getDriver().findElementsWithoutWaiting(container, xpath));
    }

    /**
     * @param objectType the type of object
     * @param objectNumber the object number
     * @param propertyLabel the label of an object property
     * @return the changes displayed either as a unified diff (for multi-line values) or as an in-line diff (for
     *         single-line values)
     */
    public String getObjectChanges(String objectType, int objectNumber, String propertyLabel)
    {
        String xpath =
            ".//dd[preceding-sibling::dt[1][. = '" + propertyLabel + "'] and "
                + "parent::dl[preceding-sibling::*[1][starts-with(., 'Object number " + objectNumber + " of type "
                + objectType + "')]]]/*[1]";
        return getDiff(getDriver().findElementWithoutWaiting(container, By.xpath(xpath)));
    }

    /**
     * @return the list of summaries for object changes
     */
    public List<String> getObjectChangeSummaries()
    {
        By xpath = By.xpath("*[starts-with(name(), 'H') and starts-with(., 'Object number ')]");
        return getText(getDriver().findElementsWithoutWaiting(container, xpath));
    }

    /**
     * @return the list of class changes
     */
    public List<String> getClassChanges()
    {
        By xpath = By.xpath("ul[preceding-sibling::*[1][. = 'Class changes']]/li");
        return getText(getDriver().findElementsWithoutWaiting(container, xpath));
    }

    /**
     * @param elements a list of elements
     * @return the list of inner text of the given elements
     */
    private List<String> getText(List<WebElement> elements)
    {
        List<String> text = new ArrayList<String>();
        for (WebElement element : elements) {
            text.add(element.getText());
        }
        return text;
    }

    /**
     * @return {@code true} if the "No changes" message is displayed, {@code false} otherwise
     */
    public boolean hasNoChanges()
    {
        return getDriver().findElementsWithoutWaiting(container,
            By.xpath("div[@class = 'infomessage' and . = 'No changes']")).size() > 0;
    }
}
