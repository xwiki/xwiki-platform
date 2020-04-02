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
package org.xwiki.user.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Represents the User Profile Preferences Tab.
 *
 * @version $Id$
 * @since 12.3RC1
 * @since 12.2.1
 */
public class GroupsUserProfilePage extends AbstractUserProfilePage
{
    @FindBy(id = "user.profile.groups.title")
    private WebElement groupsTitle;
    
    private LiveTableElement liveTable = new LiveTableElement("user.profile.group.table");

    public static GroupsUserProfilePage gotoPage(String username)
    {
        getUtil().gotoPage("XWiki", username, "view", "category=groups");
        GroupsUserProfilePage page = new GroupsUserProfilePage(username);
        return page;
    }
    
    public GroupsUserProfilePage(String username)
    {
        super(username);
    }

    public String getPreferencesTitle()
    {
        return this.groupsTitle.getText();
    }

    public LiveTableElement getGroupsPaneLiveTable()
    {
        this.liveTable.waitUntilReady();
        return this.liveTable;
    }
}
