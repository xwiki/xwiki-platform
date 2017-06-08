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
package org.xwiki.test.ui.administration;

import org.openqa.selenium.By;
import org.xwiki.administration.test.po.LightBoxRegistrationPage;
import org.xwiki.test.ui.po.AbstractRegistrationPage;
import org.xwiki.test.ui.RegisterTest;
import org.xwiki.test.ui.TestUtils;

/**
 * Test the Admin->Users->AddNewUser feature by executing the same tests as in RegisterTest but from
 * the lightbox from Admin->Users->AddNewUser.
 * 
 * @version $Id$
 * @since 2.4M2
 */
public class RegisterFromAdministrationTest extends RegisterTest
{
    @Override
    protected void switchUser()
    {
        getDriver().get(getUtil().getURLToLoginAsAdmin());
        getUtil().recacheSecretToken();
        getUtil().setDefaultCredentials(TestUtils.ADMIN_CREDENTIALS);
    }

    @Override
    protected AbstractRegistrationPage getRegistrationPage()
    {
        return LightBoxRegistrationPage.gotoPage();
    }

    @Override
    protected boolean tryToRegister()
    {
        registrationPage.clickRegister();

        getDriver().waitUntilElementsAreVisible(
            new By[] {By.xpath("//td[@class='username']/a[@href='/xwiki/bin/view/XWiki/JohnSmith']"),
                      By.xpath("//dd/span[@class='LV_validation_message LV_invalid']")
            },
            false
        );

        return !getDriver()
                .findElements(
                  By.xpath("//td[@class='username']/a[@href='/xwiki/bin/view/XWiki/JohnSmith']"))
                    .isEmpty();
    }
}
