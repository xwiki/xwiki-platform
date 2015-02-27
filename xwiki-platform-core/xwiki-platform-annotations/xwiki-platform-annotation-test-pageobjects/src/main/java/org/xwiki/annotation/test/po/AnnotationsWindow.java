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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the Annotation window that appears when selecting a text and pressing CTRL+M.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class AnnotationsWindow extends BaseElement
{
    @FindBy(xpath = "//div[contains(@class, 'annotation-box')]//input[@type='submit']")
    private WebElement submitButton;

    @FindBy(xpath = "//div[contains(@class, 'annotation-box')]//input[@type='reset']")
    private WebElement cancelButton;

    @FindBy(xpath = "//textarea[@id='comment']")
    private WebElement inputText;

    public AnnotationsWindow()
    {
        super();
    }

    public void enterAnnotationText(String annotationText)
    {
        getDriver().waitUntilElementIsVisible(By.className("annotation-box-create"));
        this.inputText.sendKeys(annotationText);
    }

    public void clickSaveAnnotation()
    {
        this.submitButton.click();
    }

    public void clickCancelAnnotation()
    {
        this.cancelButton.click();
    }

    public void addAnnotation(String annotationText)
    {
        enterAnnotationText(annotationText);
        clickSaveAnnotation();
    }
}
