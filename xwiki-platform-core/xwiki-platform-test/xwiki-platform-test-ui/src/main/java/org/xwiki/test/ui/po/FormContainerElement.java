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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Represents a form element and provide utility methods to get/set form data. This
 * container element can be a FORM element. It can also be some DIV element wrapping several FORM elements.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class FormContainerElement extends BaseElement
{
    private static final String CLASS_ATTRIBUTE = "class";
    private final WebElement formElement;

    /**
     * The form element is retrieved through the given selector.
     * This allow to retrieve a specific form when the container contains multiples.
     * @param formSelector the selector used to retrieve the form
     * @since 11.5RC1
     * @since 11.3.1
     */
    public FormContainerElement(By formSelector)
    {
        this.formElement = getDriver().findElementWithoutWaiting(formSelector);
    }

    /**
     * The form element is retrieved through its container.
     * Note that this constructor shouldn't be used in case of several forms.
     * @param formContainer the container containing the form.
     */
    public FormContainerElement(WebElement formContainer)
    {
        this.formElement = getDriver().findElementWithoutWaiting(formContainer, By.tagName("form"));
    }

    protected WebElement getFormElement()
    {
        return this.formElement;
    }

    public void fillFieldsByName(Map<String, String> valuesByNames)
    {
        Map<WebElement, String> valuesByElements = new LinkedHashMap<>();
        
        WebElement lastElement = null;
        for (String name : valuesByNames.keySet()) {
            lastElement = getFormElement().findElement(By.name(name));
            valuesByElements.put(lastElement, valuesByNames.get(name));
        }
        fillFieldsByElements(valuesByElements);
        
        /* Register password confirmation is usually the last element that needs to be validated by liveValidation.
          This wait allows to solve a race condition between the form submission and the computation of the status of
          those fields. We force the status to be solved before we try anything else, especially submitting the form.
          Unfortunately in Java17 we do not have lastEntry() from LinkedHashMaps, 
          so we use a few non optimized operations instead. 
          This is okay because the Map should not contain a lot of elements.
          */
        if(!valuesByElements.isEmpty() && lastElement != null) {
            WebElement finalLastElement = lastElement;
            getDriver().waitUntilCondition(driver -> !finalLastElement.getAttribute(CLASS_ATTRIBUTE).isEmpty());
        }
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
        return getDriver().scrollTo(getFormElement().findElement(findElementBy)).getAttribute("value");
    }

    public void setFieldValue(By findElementBy, String value)
    {
        setFieldValue(getFormElement().findElement(findElementBy), value);
    }

    public boolean hasField(By findFieldBy)
    {
        return getDriver().hasElementWithoutWaiting(getFormElement(), findFieldBy);
    }

    public void setFieldValue(WebElement fieldElement, String value)
    {
        getDriver().scrollTo(fieldElement);
        if ("checkbox".equals(fieldElement.getAttribute("type"))) {
            setCheckBox(fieldElement, value.equals("true"));
        } else if ("select".equals(fieldElement.getTagName())) {
            // if a select uses selectized then we should use a SuggestInputElement.
            if (fieldElement.getAttribute(CLASS_ATTRIBUTE).contains("selectized")) {
                SuggestInputElement suggestInputElement = new SuggestInputElement(fieldElement);
                suggestInputElement.clearSelectedSuggestions();

                // If the select accepts multiple values and the value contains "|" to separate multiple values,
                // we split the values and add them all.
                // We reuse the multiple check from selenium.support.ui.Select.
                if (fieldElement.getAttribute("multiple") != null
                    && !"false".equals(fieldElement.getAttribute("multiple"))
                    && value.contains("|")) {
                    for (String singleValue : value.split("\\|")) {
                        suggestInputElement.sendKeys(singleValue).selectTypedText();
                    }
                } else {
                    suggestInputElement.sendKeys(value).selectTypedText();
                }

            } else {
                Select select = new Select(fieldElement);
                select.selectByValue(value);
            }
        } else {
            List<String> classes = Arrays.asList(fieldElement.getAttribute(CLASS_ATTRIBUTE).split("\\s+"));
            // If the field is a date time picker, calling sendKeys after clear triggers a focus and the field,
            // and the picker fills the field with the current date and time before sendKeys is calls, leading to an
            // invalid content field. In this case, we use a javascript expression to set the value without interacting
            // with the UI.
            if (classes.contains("datetime")) {
                getDriver().executeJavascript("arguments[0].value = arguments[1]", fieldElement, value);
            } else {
                fieldElement.clear();
                fieldElement.sendKeys(value);
            }
        }
    }

    public void setCheckBox(By findElementBy, boolean checked)
    {
        setCheckBox(getFormElement().findElement(findElementBy), checked);
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
        return this.formElement.getAttribute("action");
    }

    public SelectElement getSelectElement(By by)
    {
        WebElement element = getFormElement().findElement(by);
        getDriver().scrollTo(element);
        return this.new SelectElement(element);
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
