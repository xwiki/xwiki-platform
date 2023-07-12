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
package org.xwiki.requiredrights.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page Object for the required rights on the edit side panel.
 *
 * @version $Id$
 * @since 15.6RC1
 */
public class RequiredRightsEditPanel extends ViewPage
{
    /**
     * @param index the index of the option to select (starting at {@code 0})
     */
    public void select(int index)
    {
        new Select(getDriver().findElement(By.cssSelector("#documentInformationRequiredRights select")))
            .selectByIndex(index);
    }
}
