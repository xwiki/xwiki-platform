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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.index.tree.test.po.DocumentPickerModal;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the location picker.
 * 
 * @version $Id$
 * @since 8.4.3
 * @since 9.0RC1
 */
public class LocationPicker extends BaseElement
{
    private String name;

    public LocationPicker(String name)
    {
        this.name = name;
    }

    public LocationPicker add(String value)
    {
        EntityReference spaceReference = getUtil().resolveSpaceReference(value);
        EntityReference wikiReference = spaceReference.extractReference(EntityType.WIKI);
        if (wikiReference != null) {
            spaceReference = spaceReference.removeParent(wikiReference);
        }
        EntityReference documentReference = new EntityReference("WebHome", EntityType.DOCUMENT, spaceReference);
        String[] path =
            documentReference.getReversedReferenceChain().stream().map(EntityReference::getName).toArray(String[]::new);
        browse().selectDocument(path);
        return waitToDisplayValue(value);
    }

    public LocationPicker remove(String value)
    {
        getRemoveButton(value).click();
        return this;
    }

    public List<String> getValue()
    {
        List<String> values = new ArrayList<>();
        for (WebElement path : getDriver()
            .findElementsWithoutWaiting(By.cssSelector(".path input[name='" + this.name + "'][value]"))) {
            values.add(path.getText());
        }
        return values;
    }

    public LocationPicker setValue(List<String> values)
    {
        clear();
        for (String value : values) {
            add(value);
        }
        return this;
    }

    public LocationPicker clear()
    {
        for (String value : getValue()) {
            remove(value);
        }
        return this;
    }

    public DocumentPickerModal browse()
    {
        getAddButton().click();
        return new DocumentPickerModal(By.cssSelector(".location-picker.modal")).waitForIt();
    }

    public WebElement getAddButton()
    {
        return getDriver()
            .findElementWithoutWaiting(By.cssSelector(".paths input[name='" + this.name + "'] + a[href='#path-add']"));
    }

    public WebElement getRemoveButton(String value)
    {
        return getDriver().findElementWithoutWaiting(By.xpath("//*[@class = 'paths']//input[@name = '" + this.name
            + "' and @value = '" + value + "']/preceding-sibling::a[@class = 'path-delete']"));
    }

    private LocationPicker waitToDisplayValue(String value)
    {
        getDriver().waitUntilElementIsVisible(By.xpath("//*[@class='path']/input[@name='" + this.name + "' and @value='"
            + value + "']/following-sibling::*[@class='breadcrumb']/li[not(@class='loading')]"));
        return this;
    }
}
