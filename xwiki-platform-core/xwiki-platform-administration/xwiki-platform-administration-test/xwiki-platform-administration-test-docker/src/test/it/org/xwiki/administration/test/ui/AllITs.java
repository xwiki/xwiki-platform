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
@UITest
public class AllITs
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
    @DisplayName("Users and Groups Rights Management")
    class NestedUsersGroupsRightsManagementIT extends UsersGroupsRightsManagementIT
    {
    }

    @Nested
    @DisplayName("Forgot Username")
    class NestedForgotUsernameIT extends ForgotUsernameIT
    {
    }

    @Nested
    @DisplayName("XAR Import")
    class NestedXARImportIT extends XARImportIT
    {
    }

    @Nested
    @DisplayName("Page Templates")
    class NestedPageTemplatesIT extends PageTemplatesIT
    {
    }
}
