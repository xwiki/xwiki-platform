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
package org.xwiki.test.ui.po;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the actions possible for the different registration pages (standard registration page and the registration
 * modal).
 *
 * @version $Id$
 * @since 3.2M3
 */
public abstract class AbstractRegistrationPage extends BasePage
{
    @FindBy(id = "register")
    private WebElement registerFormElement;

    private FormContainerElement form;

    public abstract void clickRegister();

    public void fillInJohnSmithValues()
    {
        fillRegisterForm("John", "Smith", "JohnSmith", "WeakPassword", "WeakPassword", "johnsmith@xwiki.org");
    }

    /**
     * Fill the registration form for the creation of a new user with the provided parameters. When a parameter is
     * {@code null}, the corresponding field stays unchanged.
     *
     * @param firstName the first name of the new user
     * @param lastName the last name of the new user
     * @param username the username of the new user
     * @param password the password of the new user
     * @param confirmPassword the confirmation password of the new user
     * @param email the email of the new user
     */
    public void fillRegisterForm(final String firstName, final String lastName, final String username,
        final String password, final String confirmPassword, final String email)
    {
        Map<String, String> map = new HashMap<>();
        // remove the onfocus on login, to avoid any problem to put the value.
        getDriver().executeJavascript("try{ document.getElementById('xwikiname').onfocus = null; " 
            + "}catch(err){}");
        if (firstName != null) {
            map.put("register_first_name", firstName);
        }
        if (lastName != null) {
            map.put("register_last_name", lastName);
        }
        if (username != null) {
            map.put("xwikiname", username);
        }
        if (password != null) {
            map.put("register_password", password);
        }
        if (confirmPassword != null) {
            map.put("register2_password", confirmPassword);
        }
        if (email != null) {
            map.put("register_email", email);
        }
        getForm().fillFieldsByName(map);
    }

    private FormContainerElement getForm()
    {
        if (this.form == null) {
            this.form = new FormContainerElement(By.id("register"));
        }
        return this.form;
    }

    /** @return a list of WebElements representing validation failure messages. Use after calling register() */
    public List<WebElement> getValidationFailureMessages()
    {
        return getDriver().findElementsWithoutWaiting(By.xpath("//dd/span[@class='LV_validation_message LV_invalid']"));
    }

    /** @return Is the specified message included in the list of validation failure messages. */
    public boolean validationFailureMessagesInclude(String message)
    {
        return getDriver().findElementsWithoutWaiting(
            By.xpath("//dd/span[@class='LV_validation_message LV_invalid' and . = '" + message + "']")).size() > 0;
    }

    /** Try to make LiveValidation validate the forms. */
    public void triggerLiveValidation()
    {
        // By manually invoking onsubmit with null as it's parameter,
        // liveValidation will check fields but when it attempts to call submit with null as the
        // input, it encounters an error which keeps the next page from loading.
        getDriver().executeJavascript("try{ document.getElementById('register_first_name').focus(); " +
            "document.getElementById('register').onsubmit(null); }catch(err){}");
    }

    public boolean isLiveValidationEnabled()
    {
        return !getDriver().findElementsWithoutWaiting(this.registerFormElement, By.xpath("./script")).isEmpty();
    }
}
