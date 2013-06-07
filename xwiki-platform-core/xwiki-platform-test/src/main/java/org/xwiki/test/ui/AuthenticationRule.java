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

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.openqa.selenium.WebDriver;

/**
 * Authenticates a user in the wiki before the test starts.
 *
 * @version $Id$
 * @since 5.1M1
 * @todo decide if we want this way of authenticating to replace {@link AbstractStandardUserAuthenticatedTest}
 */
public class AuthenticationRule implements MethodRule
{
    private String userName;

    private String userPassword;

    private TestUtils testUtils;

    private WebDriver driver;

    public AuthenticationRule(String userName, String userPassword, TestUtils testUtils, WebDriver driver)
    {
        this.userName = userName;
        this.userPassword = userPassword;
        this.testUtils = testUtils;
        this.driver = driver;
    }

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, final Object target)
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                authenticate();
                base.evaluate();
            }
        };
    }

    public void authenticate()
    {
        if (!this.userName.equals(this.testUtils.getLoggedInUserName())) {
            // Log in and direct to a non existent page so that it loads very fast and we don't incur the time cost of
            // going to the home page for example.
            this.driver.get(this.testUtils.getURLToLoginAndGotoPage(this.userName, this.userPassword,
                this.testUtils.getURLToNonExistentPage()));
            this.testUtils.recacheSecretToken();
        }
    }
}
