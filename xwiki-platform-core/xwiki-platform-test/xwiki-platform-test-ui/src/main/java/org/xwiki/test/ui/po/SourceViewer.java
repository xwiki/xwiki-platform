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

import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.DocumentReference;

/**
 * Represent the source viewer of a page.
 *
 * @version $Id$
 * @since 17.7.0RC1
 */
public class SourceViewer extends BasePage
{
    @FindBy(className = "revision")
    private WebElement revisionTable;

    /**
     * Go to the viewer xpage of the given document.
     * @param documentReference the document for which to view source.
     * @return an instance of this
     */
    public static SourceViewer gotoPage(DocumentReference documentReference)
    {
        getUtil().gotoPage(documentReference, "view", "viewer=code");
        return new SourceViewer();
    }

    /**
     * @return the total number of lines of the source
     */
    public int getLineNumber()
    {
        return getDriver().findElementsWithoutWaiting(revisionTable, By.className("line")).size();
    }

    /**
     * @return the entire content of the source
     */
    public String getEntireSource()
    {
        return getDriver().findElementsWithoutWaiting(revisionTable, By.className("content"))
            .stream()
            .map(WebElement::getText)
            .collect(Collectors.joining("\n"));
    }

    /**
     * Get the source of a specific line
     * @param lineNumber the number of the line
     * @return the source of the specified line number
     */
    public String getLineSource(int lineNumber)
    {
        return getDriver().findElementWithoutWaiting(revisionTable, By.id(String.valueOf(lineNumber))).getText();
    }
}
