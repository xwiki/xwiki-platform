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
package org.xwiki.user.directory.test.po;

import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the User Directory page ({@code Main.UserDirectory}).
 *
 * @version $Id$
 * @since 4.2M3
 */
public class UserDirectoryPage extends ViewPage
{
    /**
     * Go to the User Directory page.
     *
     * @return the page object for the User Directory page
     */
    public static UserDirectoryPage gotoPage()
    {
        getUtil().gotoPage("Main", "UserDirectory");
        return new UserDirectoryPage();
    }

    /**
     * @return the live table listing the users, once it's ready
     */
    public LiveTableElement getUserDirectoryLiveTable()
    {
        LiveTableElement lt = new LiveTableElement("userdirectory");
        lt.waitUntilReady();
        return lt;
    }
}
