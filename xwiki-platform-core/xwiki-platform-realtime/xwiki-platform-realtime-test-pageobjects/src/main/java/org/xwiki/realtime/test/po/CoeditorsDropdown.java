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
package org.xwiki.realtime.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The dropdown that lists all the users that are currently editing the page in realtime.
 * 
 * @version $Id$
 * @since 16.10.6
 * @since 17.3.0RC1
 */
public class CoeditorsDropdown extends BaseElement
{
    /**
     * The dropdown is displayed only when there are more than N (4 by default) users connected to the editing session.
     * 
     * @return {@code true} if the dropdown is displayed, {@code false} otherwise
     */
    public boolean isDisplayed()
    {
        List<WebElement> usersDropdown = getDriver().findElements(By.className("realtime-users-dropdown"));
        return !usersDropdown.isEmpty() && usersDropdown.get(0).isDisplayed();
    }

    /**
     * Clicks on the dropdown toggle to open the dropdown.
     * 
     * @return this instance
     */
    public CoeditorsDropdown open()
    {
        getToggle().click();
        return this;
    }

    /**
     * Uses the ESCAPE key to close the dropdown.
     * 
     * @return this instance
     */
    public CoeditorsDropdown close()
    {
        getToggle().sendKeys(Keys.ESCAPE);
        return this;
    }

    /**
     * @return the dropdown label indicating how many more users are connected beyond those listed directly on the
     *         toolbar
     */
    public String getMoreCount()
    {
        return getToggle().getDomAttribute("data-more");
    }

    private WebElement getToggle()
    {
        return getDriver().findElement(By.cssSelector(".realtime-users-dropdown > .dropdown-toggle"));
    }

    /**
     * @return the list of users that are currently editing the page in realtime
     */
    public List<CoeditorElement> getCoeditors()
    {
        return getDriver().findElements(By.cssSelector(".realtime-users-dropdown .realtime-user")).stream()
            .map(CoeditorElement::new).toList();
    }

    /**
     * @param coeditorId the coeditor identifier
     * @return the coeditor with the specified identifier
     */
    public CoeditorElement getCoeditor(String coeditorId)
    {
        return new CoeditorElement(
            getDriver().findElement(By.cssSelector(".realtime-users-dropdown[data-id='" + coeditorId + "']")));
    }
}
