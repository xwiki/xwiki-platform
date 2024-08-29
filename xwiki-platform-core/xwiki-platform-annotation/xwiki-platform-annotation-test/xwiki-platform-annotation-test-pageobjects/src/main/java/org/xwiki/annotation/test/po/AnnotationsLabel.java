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
package org.xwiki.annotation.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Implements the Annotation window that appears when selecting an annotation
 *
 * @version $Id$
 * @since 4.2M1
 */

public class AnnotationsLabel extends BaseElement
{
    @FindBy(xpath = "//span[@class='annotationAuthor']")
    private WebElement annotationAuthor;

    @FindBy(xpath = "annotationDate")
    private WebElement annotationDate;

    private void hoverOnAnnotationByText(String searchText)
    {
        hoverOnAnnotationById(getAnnotationIdByText(searchText));
    }

    private void hoverOnAnnotationById(String annotationId)
    {
        getDriver().findElement(By.id(annotationId)).click();
        getDriver().waitUntilElementIsVisible(By.className("annotation-box-view"));
    }

    private void showAnnotationById(String idText)
    {
        hoverOnAnnotationById(idText);
    }

    /**
     * Shows the annotation associated to the specified text.
     * 
     * @param text the annotated text
     * @since 12.10.9
     * @since 13.4.1
     * @since 13.5RC1
     */
    public void showAnnotationByText(String text)
    {
        hoverOnAnnotationByText(text);
    }

    public void deleteAnnotationByText(String searchText)
    {
        this.deleteAnnotationById(this.getAnnotationIdByText(searchText));
    }

    public void deleteAnnotationById(String idText)
    {
        this.showAnnotationById(idText);
        // The id format is "ID<index>".
        String index = idText.substring(2);
        getDriver().findElement(By.cssSelector("#annotation_view_" + index + " a.delete")).click();
        getDriver().waitUntilElementIsVisible(By.xpath("//input[@value='Yes']"));
        getDriver().findElement(By.xpath("//input[@value='Yes']")).click();
    }

    public String getAnnotationsAuthorByText(String searchText)
    {
        this.hoverOnAnnotationByText(searchText);
        return this.annotationAuthor.getText();
    }

    public String getAnnotationAuthorById(String idText)
    {
        this.showAnnotationById(idText);
        return this.annotationAuthor.getText();
    }

    public String getAnnotationIdByText(String searchText)
    {
        getDriver().waitUntilElementIsVisible(By.xpath("//span[contains(.,'" + searchText + "')]"));
        WebElement annotation = getDriver().findElement(By.xpath("//span[contains(.,'" + searchText + "')]"));
        String classId = annotation.getAttribute("class");
        classId = classId.split("\\s+")[1];
        return classId;
    }

    public String getAnnotationContentByText(String searchText)
    {
        hoverOnAnnotationByText(searchText);
        getDriver().waitUntilElementIsVisible(By.xpath("//div[@class='annotationText']/p"));
        String annotationContent =
            getDriver().findElement(By.xpath("//*[@class='annotation-bubble']//div[@class='annotationText']/p"))
                .getText();
        WebElement body = getDriver().findElement(By.id("body"));

        // It seems that hovering over the small yellow icon sends 2 requests, and one ESC is not enough to make the
        // window disappear
        body.sendKeys(Keys.ESCAPE);
        body.sendKeys(Keys.ESCAPE);
        getDriver().waitUntilElementDisappears(By.className("annotation-box-view"));
        return annotationContent;
    }
}
