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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Represents a container element holding one or several forms and provide utility methods to get/set form data. This
 * container element can be a FORM element. It can also be some DIV element wrapping several FORM elements.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class FormContainerElement extends BaseElement
{
    private final WebElement formContainer;

    public FormContainerElement(WebElement formContainer)
    {
        this.formContainer = formContainer;
    }

    protected WebElement getFormContainer()
    {
        return this.formContainer;
    }

    public void fillFieldsByName(Map<String, String> valuesByNames)
    {
        Map<WebElement, String> valuesByElements = new HashMap<>((int) (valuesByNames.size() / 0.75));

        for (String name : valuesByNames.keySet()) {
            valuesByElements.put(getFormContainer().findElement(By.name(name)), valuesByNames.get(name));
        }
        fillFieldsByElements(valuesByElements);
    }

    public void fillFieldsByElements(Map<WebElement, String> valuesByElements)
    {
        for (WebElement el : valuesByElements.keySet()) {
            try {
                setFieldValue(el, valuesByElements.get(el));
            } catch (Exception e) {
                throw new WebDriverException(String.format("Couldn't set field [%s] to value [%s]",
                    el.getAttribute("name"), valuesByElements.get(el)), e);
            }
        }
    }

    public String getFieldValue(By findElementBy)
    {
        return getFormContainer().findElement(findElementBy).getAttribute("value");
    }

    public void setFieldValue(By findElementBy, String value)
    {
        setFieldValue(getFormContainer().findElement(findElementBy), value);
    }

    public boolean hasField(By findFieldBy)
    {
        return getDriver().hasElementWithoutWaiting(this.formContainer, findFieldBy);
    }

    public void setFieldValue(WebElement fieldElement, String value)
    {
        if ("checkbox".equals(fieldElement.getAttribute("type"))) {
            setCheckBox(fieldElement, value.equals("true"));
        } else if ("select".equals(fieldElement.getTagName())) {
            Select select = new Select(fieldElement);
            select.selectByValue(value);
        } else {
            fieldElement.clear();
            fieldElement.sendKeys(value);
        }
    }

    public void setCheckBox(By findElementBy, boolean checked)
    {
        setCheckBox(getFormContainer().findElement(findElementBy), checked);
    }

    public void setCheckBox(WebElement checkBoxElement, boolean checked)
    {
        // TODO: this hack should probably be removed for Selenium 2/3
        int x = 0;
        while (checkBoxElement.isSelected() != checked) {
            checkBoxElement.click();
            if (x == 100) {
                throw new WebDriverException(String.format("Unable to set checkbox at [%s] to [%s]",
                    checkBoxElement.getAttribute("name"), checked));
            }
            x++;
        }
    }

    public String getFormAction()
    {
        return getDriver().findElementWithoutWaiting(this.formContainer, By.tagName("form")).getAttribute("action");
    }

    public SelectElement getSelectElement(By by)
    {
        return this.new SelectElement(getFormContainer().findElement(by));
    }

    public class SelectElement extends BaseElement
    {
        private final WebElement select;

        private Map<String, WebElement> optionsByValue;

        public SelectElement(WebElement select)
        {
            if (!select.getTagName().toLowerCase().equals("select")) {
                throw new WebDriverException("Can only create a select element from a webelement of tag name select.");
            }
            this.select = select;
        }

        public Set<String> getOptions()
        {
            return getOptionsByValue().keySet();
        }

        private Map<String, WebElement> getOptionsByValue()
        {
            if (this.optionsByValue != null) {
                return this.optionsByValue;
            }
            List<WebElement> elements = this.select.findElements(By.tagName("option"));
            this.optionsByValue = new HashMap<>((int) (elements.size() / 0.75));
            for (WebElement el : elements) {
                this.optionsByValue.put(el.getAttribute("value"), el);
            }
            return this.optionsByValue;
        }

        public void select(List<String> valuesToSelect)
        {
            if (valuesToSelect.size() > 1 && this.select.getAttribute("multiple") != "multiple") {
                throw new WebDriverException("Cannot select multiple elements in drop down menu.");
            }
            Map<String, WebElement> optionsByValue = getOptionsByValue();
            if (!optionsByValue.keySet().containsAll(valuesToSelect)) {
                throw new WebDriverException(String.format("Select Element(s) [%s] not found.",
                    optionsByValue.keySet().retainAll(valuesToSelect)));
            }
            for (String label : valuesToSelect) {
                optionsByValue.get(label).click();
            }
        }

        public void select(final String value)
        {
            select(new ArrayList<String>()
            {
                {
                    add(value);
                }
            });
        }

        public void unSelect(List<String> valuesToUnSelect)
        {
            Map<String, WebElement> optionsByValue = getOptionsByValue();
            if (!optionsByValue.keySet().containsAll(valuesToUnSelect)) {
                throw new WebDriverException(String.format("Select Element(s) to unselect [%s] not found.",
                    optionsByValue.keySet().retainAll(valuesToUnSelect)));
            }
            for (String label : valuesToUnSelect) {
                if (optionsByValue.get(label).isSelected()) {
                    optionsByValue.get(label).click();
                }
            }
        }

        public void unSelect(final String value)
        {
            unSelect(new ArrayList<String>()
            {
                {
                    add(value);
                }
            });
        }
    }
}
