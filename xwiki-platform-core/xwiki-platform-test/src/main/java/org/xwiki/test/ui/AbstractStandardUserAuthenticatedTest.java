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
package org.xwiki.test.ui;

import org.junit.Before;

/**
 * Helper class to be extended by tests requiring a standard user logged in.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public abstract class AbstractStandardUserAuthenticatedTest extends AbstractTest
{
    @Before
    public void setUp() throws Exception
    {
        loginStandardUser();
    }

    public static void loginStandardUser()
    {
        if (!"TestUser".equals(getUtil().getLoggedInUserName())) {
            // Log in and direct to a non existent page so that it loads very fast and we don't incur the time cost of
            // going to the home page for example.
            getUtil().registerLoginAndGotoPage("TestUser", "TestPassword", getUtil().getURLToNonExistentPage());
            getUtil().recacheSecretToken();
        }
    }
}
