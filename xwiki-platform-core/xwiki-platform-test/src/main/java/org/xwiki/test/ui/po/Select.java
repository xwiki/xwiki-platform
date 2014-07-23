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

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.UnexpectedTagNameException;

/**
 * Extends {@link org.openqa.selenium.support.ui.Select} in order to fix some bugs.
 * 
 * @version $Id$
 * @since 6.0M1
 */
public class Select extends org.openqa.selenium.support.ui.Select
{
    /**
     * The wrapped select element.
     */
    private final WebElement element;

    /**
     * Constructor. A check is made that the given element is, indeed, a SELECT tag. If it is not, then an
     * UnexpectedTagNameException is thrown.
     * 
     * @param element SELECT element to wrap
     * @throws UnexpectedTagNameException when element is not a SELECT
     */
    public Select(WebElement element)
    {
        super(element);
        this.element = element;
    }

    @Override
    public void selectByIndex(int index)
    {
        super.selectByIndex(index);
        maybeCloseDropDownList();
    }

    @Override
    public void selectByValue(String value)
    {
        super.selectByValue(value);
        maybeCloseDropDownList();
    }

    @Override
    public void selectByVisibleText(String text)
    {
        super.selectByVisibleText(text);
        maybeCloseDropDownList();
    }

    /**
     * Sometimes the select drop down list remains opened after we select an option. This is a utility method to close
     * the drop down list.
     */
    private void maybeCloseDropDownList()
    {
        if (!isMultiple()) {
            element.sendKeys(Keys.ESCAPE);
        }
    }
}
