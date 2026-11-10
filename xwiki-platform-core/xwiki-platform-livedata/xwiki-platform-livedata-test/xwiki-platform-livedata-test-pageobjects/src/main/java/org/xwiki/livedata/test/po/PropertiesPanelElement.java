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

package org.xwiki.livedata.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents the advanced panel listing the properties of a live data, from which a property is displayed as a column
 * or hidden. The panel offers the properties the live data declares, each one ticked when it is displayed.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
public class PropertiesPanelElement extends AbstractLiveDataAdvancedPanelElement
{
    private static final By PROPERTY_NAME = By.className("property-name");

    private static final By NO_COVER = By.cssSelector(".no-cover input[type='radio']");

    private static final By COVER_RADIO = By.cssSelector(".cover input[type='radio']");

    /**
     * Default constructor.
     *
     * @param liveData the live data of the panel
     * @param container the container of the panel
     */
    public PropertiesPanelElement(LiveDataElement liveData, WebElement container)
    {
        super(liveData, container);
    }

    /**
     * @return the label of every property the panel offers, in the order they are offered
     */
    public List<String> getPropertyNames()
    {
        return getProperties().stream().map(property -> property.findElement(PROPERTY_NAME).getText()).toList();
    }

    /**
     * @param propertyName the label of a property
     * @return {@code true} if the panel offers that property, {@code false} otherwise
     */
    public boolean hasProperty(String propertyName)
    {
        return getPropertyNames().contains(propertyName);
    }

    /**
     * @param propertyName the label of a property the panel offers
     * @return {@code true} if that property is displayed as a column, {@code false} if it is hidden
     */
    public boolean isPropertyDisplayed(String propertyName)
    {
        return getCheckbox(propertyName).isSelected();
    }

    /**
     * Displays or hides the column of a property, the way ticking or unticking its checkbox does.
     *
     * @param propertyName the label of a property the panel offers
     * @param displayed {@code true} to display the property as a column, {@code false} to hide it
     */
    public void setPropertyDisplayed(String propertyName, boolean displayed)
    {
        if (isPropertyDisplayed(propertyName) != displayed) {
            getCheckbox(propertyName).click();
        }
    }

    /**
     * The cover option is only offered by the card layout.
     *
     * @return {@code true} if the panel offers to display a cover on the cards, {@code false} otherwise
     * @since 18.10.0RC1
     */
    public boolean hasCoverOption()
    {
        return !getDriver().findElementsWithoutWaiting(this.container, NO_COVER).isEmpty();
    }

    /**
     * Removes the card cover, the way selecting the "No cover" option does.
     *
     * @since 18.10.0RC1
     */
    public void removeCover()
    {
        this.container.findElement(NO_COVER).click();
    }

    /**
     * @return the label of the property used as the card cover, or {@code null} when the cards don't have a cover
     * @since 18.10.0RC1
     */
    public String getCoverPropertyName()
    {
        return getProperties().stream()
            .filter(property -> getDriver().findElementsWithoutWaiting(property, COVER_RADIO).stream()
                .anyMatch(WebElement::isSelected))
            .map(property -> property.findElement(PROPERTY_NAME).getText())
            .findFirst()
            .orElse(null);
    }

    /**
     * A hidden property can't be used as the card cover.
     *
     * @param propertyName the label of a property the panel offers
     * @return {@code true} if the property can be selected as the card cover, {@code false} otherwise
     * @since 18.10.0RC1
     */
    public boolean isCoverPropertyEnabled(String propertyName)
    {
        return getProperty(propertyName).findElement(COVER_RADIO).isEnabled();
    }

    /**
     * Selects the property used as the card cover.
     *
     * @param propertyName the label of a property the panel offers
     * @since 18.10.0RC1
     */
    public void setCoverProperty(String propertyName)
    {
        getProperty(propertyName).findElement(COVER_RADIO).click();
    }

    private WebElement getCheckbox(String propertyName)
    {
        return getProperty(propertyName).findElement(By.cssSelector("input[type='checkbox']"));
    }

    private WebElement getProperty(String propertyName)
    {
        return getProperties().stream()
            .filter(property -> property.findElement(PROPERTY_NAME).getText().equals(propertyName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("The properties panel does not offer any property named [%s]. It offers %s.",
                    propertyName, getPropertyNames())));
    }

    private List<WebElement> getProperties()
    {
        return this.container.findElements(By.className("property"));
    }
}
