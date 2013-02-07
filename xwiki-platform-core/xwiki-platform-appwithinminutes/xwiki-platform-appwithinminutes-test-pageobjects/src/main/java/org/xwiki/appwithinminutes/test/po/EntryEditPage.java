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
package org.xwiki.appwithinminutes.test.po;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.FormElement;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.editor.wysiwyg.EditorElement;

/**
 * Represents the actions possible when editing an application entry.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class EntryEditPage extends InlinePage
{
    /**
     * The XPath that locates the label of a form field.
     * <p>
     * NOTE: We use a workaround for the fact that ends-with XPath function is not implemented:
     * 
     * <pre>
     * {@code substring(@id, string-length(@id) - string-length(suffix) + 1)}
     * </pre>
     */
    private static final String LABEL_XPATH_FORMAT = "//label[substring(@for, string-length(@for) - %s - 2) = '_0_%s']";

    /**
     * The XPath that locates a form field.
     */
    private static final String FIELD_XPATH_FORMAT = "//*[substring(@name, string-length(@name) - %s - 2) = '_0_%s']";

    /**
     * Retrieves the label of the specified form field.
     * 
     * @param fieldName the name of a form field
     * @return the label of the specified form field
     */
    public String getLabel(String fieldName)
    {
        String xpath = String.format(LABEL_XPATH_FORMAT, fieldName.length(), fieldName);
        WebElement label = getForm().findElement(By.xpath(xpath));
        return label.getText();
    }

    /**
     * Retrieves the value of the specified form field
     * 
     * @param fieldName the name of a form field
     * @return the value of the specified form field
     */
    public String getValue(String fieldName)
    {
        String xpath = String.format(FIELD_XPATH_FORMAT, fieldName.length(), fieldName);
        return new FormElement(getForm()).getFieldValue(By.xpath(xpath));
    }

    /**
     * Sets the value of the specified form field
     * 
     * @param fieldName the name of a form field
     * @param fieldValue the new value for the specified form field
     */
    public void setValue(String fieldName, String fieldValue)
    {
        String xpath = String.format(FIELD_XPATH_FORMAT, fieldName.length(), fieldName);
        WebElement field = getForm().findElement(By.xpath(xpath));
        if (field.getAttribute("name").equals(field.getAttribute("id"))) {
            new FormElement(getForm()).setFieldValue(field, fieldValue);
        } else {
            xpath = String.format("//*[@name = '%s' and @value = '%s']", field.getAttribute("name"), fieldValue);
            new FormElement(getForm()).setCheckBox(By.xpath(xpath), true);
        }
    }

    /**
     * @return the list of form field names available on this page
     */
    public List<String> getFieldNames()
    {
        List<String> fieldNames = new ArrayList<String>();
        for (WebElement field : getForm().findElements(By.xpath("//*[contains(@name, '_0_')]"))) {
            fieldNames.add(StringUtils.substringAfter(field.getAttribute("name"), "_0_"));
        }
        return fieldNames;
    }

    /**
     * Sets the entry title, if the application class has a Title field.
     * 
     * @param title the entry title
     */
    public void setTitle(String title)
    {
        new FormElement(getForm()).setFieldValue(By.name("title"), title);
    }

    /**
     * @return the value of the title input
     */
    public String getTitle()
    {
        return getForm().findElement(By.name("title")).getAttribute("value");
    }

    /**
     * @return the content editor
     */
    public EditorElement getContentEditor()
    {
        return new EditorElement("content");
    }
}
