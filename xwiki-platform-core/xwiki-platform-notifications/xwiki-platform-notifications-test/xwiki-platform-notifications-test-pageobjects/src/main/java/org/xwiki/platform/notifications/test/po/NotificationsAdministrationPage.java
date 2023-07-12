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
package org.xwiki.platform.notifications.test.po;

/**
 * Represents the Notifications preferences in administration.
 *
 * @version $Id$
 * @since 13.2R1
 */
public class NotificationsAdministrationPage extends AbstractNotificationsSettingsPage
{
    /**
     * Default constructor.
     */
    public NotificationsAdministrationPage()
    {
        this.initializeApplications();
    }

    /**
     * Go to the administration page related to notifications and create the appropriate instance.
     * @return the appropriate instance of {@link NotificationsAdministrationPage}.
     */
    public static NotificationsAdministrationPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "section=Notifications");
        return new NotificationsAdministrationPage();
    }
}
