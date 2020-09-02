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
import org.openqa.selenium.NoSuchElementException;
import org.xwiki.administration.test.po.RegistrationModal;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.RegisterTest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AbstractRegistrationPage;

/**
 * Test the Administration -> Users -> Create User feature by executing the same tests as in {@link RegisterTest} but
 * from the create user modal.
 * 
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class RegisterFromAdministrationTest extends RegisterTest
{
    @Override
    protected void switchUser(TestUtils testUtils)
    {
        testUtils.getDriver().get(testUtils.getURLToLoginAsAdmin());
        testUtils.recacheSecretToken();
        testUtils.setDefaultCredentials(TestUtils.ADMIN_CREDENTIALS);
    }

    @Override
    protected AbstractRegistrationPage getRegistrationPage()
    {
        return RegistrationModal.gotoPage();
    }

    @Override
    protected boolean tryToRegister(TestUtils testUtils)
    {
        this.registrationPage.clickRegister();

        // Wait until one of the following happens:
        testUtils.getDriver().waitUntilElementsAreVisible(new By[] {
            // A live validation error message appears.
            By.cssSelector("dd > span.LV_validation_message.LV_invalid"),
            // The operation fails on the server.
            By.cssSelector(".xnotification-error"),
            // The operation succeeds.
            By.cssSelector(".xnotification-done")
        },false);

        try {
            // Try to hide the success message by clicking on it.
            testUtils.getDriver().findElementWithoutWaiting(
                By.xpath("//div[contains(@class,'xnotification-done') and contains(., 'User created')]")).click();
            // If we get here it means the registration was successful.
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
