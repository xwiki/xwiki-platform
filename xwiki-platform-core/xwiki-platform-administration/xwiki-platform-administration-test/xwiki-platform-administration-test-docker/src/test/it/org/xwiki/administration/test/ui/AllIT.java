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
package org.xwiki.administration.test.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI tests for the Administration feature.
 *
 * @version $Id$
 * @since 11.2RC1
 */
@UITest(sshPorts = {
    // Open the GreenMail port so that the XWiki instance inside a Docker container can use the SMTP server provided
    // by GreenMail running on the host.
    3025
},
    properties = {
        // The Mail module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml
        "xwikiDbHbmCommonExtraMappings=mailsender.hbm.xml",
        // Pages created in the tests need to have PR since we ask for PR to send mails so we need to exclude them from
        // the PR checker.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.ResetPassword|.*:XWiki\\.ResetPasswordComplete",
        // Add the RightsManagerPlugin needed by the UsersGroupsRightsManagementIT
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib -->
        "org.xwiki.platform:xwiki-platform-mail-send-storage"
    }
)
public class AllIT
{
    @Nested
    @DisplayName("Overall Administration UI")
    class NestedAdministrationIT extends AdministrationIT
    {
    }

    @Nested
    @DisplayName("Reset Password")
    class NestedResetPasswordIT extends ResetPasswordIT
    {
    }

    @Nested
    @DisplayName("ConfigurableClass")
    class NestedConfigurableClassIT extends ConfigurableClassIT
    {
    }

    @Nested
    @DisplayName("UsersGroupsRightsManagement")
    class NestedUsersGroupsRightsManagementsIT extends UsersGroupsRightsManagementIT
    {
    }
}
