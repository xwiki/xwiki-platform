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
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents the widget used to select a page template.
 * 
 * @version $Id$
 * @since 12.9RC1
 */
public class PageTypePicker extends XWikiSelectWidget
{
    public PageTypePicker()
    {
        super(By.cssSelector(".xwiki-select.page-type"), "type");
    }

    public PageTypePicker(WebElement container)
    {
        super(container, "type");
    }

    private List<WebElement> getAvailableTemplateInputs()
    {
        return getOptionInputsStream().filter(input -> "template".equals(input.getAttribute("data-type")))
            .collect(Collectors.toList());
    }

    public int countAvailableTemplates()
    {
        return getAvailableTemplateInputs().size();
    }

    public List<String> getAvailableTemplates()
    {
        List<String> availableTemplates = new ArrayList<String>();
        List<WebElement> templateInputs = getAvailableTemplateInputs();
        for (WebElement input : templateInputs) {
            if (input.getAttribute("value").length() > 0) {
                availableTemplates.add(input.getAttribute("value"));
            }
        }

        return availableTemplates;
    }

    public void selectTemplateByValue(String template)
    {
        List<WebElement> templates = getAvailableTemplateInputs();
        for (WebElement templateInput : templates) {
            if (templateInput.getAttribute("value").equals(template)) {
                // Get the label corresponding to the input so we can click on it
                WebElement label = getDriver()
                    .findElementWithoutWaiting(By.xpath("//label[@for = '" + templateInput.getAttribute("id") + "']"));
                label.click();
                return;
            }
        }
        throw new RuntimeException("Failed to find template [" + template + "]");
    }

    /**
     * @param template the value of the template option
     * @return the icon name displayed for the specified template option, extracted from the {@code <img>} element's
     *         {@code src} attribute, or an empty string if no icon is displayed
     * @since 18.3RC1
     */
    public String getTemplateIcon(String template)
    {
        WebElement option = getOptionByValue(template);
        WebElement iconSpan = option.findElement(By.className("xwiki-select-option-icon"));
        List<WebElement> iconImages = iconSpan.findElements(By.tagName("img"));
        if (iconImages.isEmpty()) {
            return "";
        }
        String src = iconImages.getFirst().getAttribute("src");
        String fileName = src.substring(src.lastIndexOf('/') + 1);
        return fileName.contains(".") ? fileName.substring(0, fileName.indexOf('.')) : fileName;
    }

    /**
     * @param template the value of the template option
     * @return the description text displayed for the specified template option, or an empty string if none
     * @since 18.3.0RC1
     */
    public String getTemplateDescription(String template)
    {
        WebElement option = getOptionByValue(template);
        List<WebElement> hints = option.findElements(By.className("xHint"));
        if (hints.isEmpty()) {
            return "";
        }
        return hints.getFirst().getText();
    }
}
