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

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

/**
 * The pane used to display and edit in-place the document required rights.
 * 
 * @version $Id$
 * @since 15.6RC1
 */
public class DocumentRequiredRightsPropertyPane extends EditablePropertyPane<String>
{
    /**
     * Default constructor.
     */
    public DocumentRequiredRightsPropertyPane()
    {
        super("requiredRights");
    }

    /**
     * Sets the property value while the property is being edited.
     *
     * @param value the index of the property to select (e.g., "0" for the fist select option)
     * @return this property pane
     */
    @Override
    public DocumentRequiredRightsPropertyPane setValue(String value)
    {
        Select select = new Select(getDriver().findElementWithoutWaiting(this.editor, By.cssSelector("select")));
        select.selectByIndex(Integer.parseInt(value));
        return this;
    }

    @Override
    public DocumentRequiredRightsPropertyPane clickEdit()
    {
        return (DocumentRequiredRightsPropertyPane) super.clickEdit();
    }
}
