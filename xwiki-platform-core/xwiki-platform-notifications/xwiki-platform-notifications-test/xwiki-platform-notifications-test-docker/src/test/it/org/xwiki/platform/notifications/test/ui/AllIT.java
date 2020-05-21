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
package org.xwiki.platform.notifications.test.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI tests for the Notifications feature.
 *
 * @since 12.3RC1
 * @version $Id$
 */
@UITest(sshPorts = {
    // Open the GreenMail port so that the XWiki instance inside a Docker container can use the SMTP server provided
    // by GreenMail running on the host.
    3025
},
    properties = {
        // Mail used for NotificationsEmailsIT and filters preferences for almost all tests
        "xwikiDbHbmCommonExtraMappings=mailsender.hbm.xml,notification-filter-preferences.hbm.xml",

        // Scheduler used in NotificationsEmailsIT
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.scheduler.SchedulerPlugin",
        // Switch to domain-based URL for NotificationsEmailsIT
        "xwikiCfgVirtualUsepath=0"
    },
    extraJARs = {
        "org.xwiki.platform:xwiki-platform-mail-send-storage",
        "org.xwiki.platform:xwiki-platform-notifications-filters-default"
    }
)
public class AllIT
{
    @Nested
    @DisplayName("Overall Notifications Settings UI tests")
    class NestedNotificationsSettingsIT extends NotificationsSettingsIT
    {
    }

    @Nested
    @DisplayName("Overall Notifications Display UI tests")
    class NestedNotificationsIT extends NotificationsIT
    {
    }

    @Nested
    @DisplayName("Overall Notifications Emails tests")
    class NestedNotificationsEmailsIT extends NotificationsEmailsIT
    {
    }
}