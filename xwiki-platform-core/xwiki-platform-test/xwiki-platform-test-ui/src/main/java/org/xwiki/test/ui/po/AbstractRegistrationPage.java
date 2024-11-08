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
    /**
     * Username used for registering a user with {@link #fillInJohnSmithValues()}.
     * @since 15.10RC1
     */
    public static final String JOHN_SMITH_USERNAME = "JohnSmith";

    /**
     * Password used for registering a user with {@link #fillInJohnSmithValues()}.
     * @since 15.10RC1
     */
    public static final String JOHN_SMITH_PASSWORD = "WeakPassword";

    @FindBy(id = "register")
    private WebElement registerFormElement;

    private FormContainerElement form;

    public abstract void clickRegister();

    public void fillInJohnSmithValues()
    {
        fillRegisterForm("John", "Smith", JOHN_SMITH_USERNAME, JOHN_SMITH_PASSWORD, JOHN_SMITH_PASSWORD,
            "johnsmith@xwiki.org");
    }

    public void fillRegisterForm(final String firstName, final String lastName, final String username,
        final String password, final String confirmPassword, final String email)
    {
        Map<String, String> map = new HashMap<String, String>();
        // remove the onfocus on login, to avoid any problem to put the value.
        getDriver().executeJavascript("try{ document.getElementById('xwikiname').onfocus = null; " +
            "}catch(err){}");
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
        return getDriver().findElementsWithoutWaiting(
            By.xpath("//dd/span[contains(@class,'LV_validation_message LV_invalid')]"));
    }

    /**
      * @return a list of WebElements representing error messages. Use after calling register()
      */
    public WebElement getErrorMessage(String message)
    {
        return getDriver().findElementWithoutWaiting(By.xpath("//div[@class = 'box errormessage']"));
    }

    /** @return Is the specified message included in the list of validation failure messages. */
    public boolean validationFailureMessagesInclude(String message)
    {
        return !getDriver().findElementsWithoutWaiting(
            By.xpath("//dd/span[contains(@class, 'LV_validation_message') and " + 
                "contains(@class, 'LV_invalid') and " +
                "contains(., '" + message + "')]"))
            .isEmpty();
    }

    /** @return Is the specified message included in the list of error messages. */
    public boolean errorMessageInclude(String message)
    {
        return !getDriver().findElementsWithoutWaiting(
                By.xpath("//div[@class = 'box errormessage' and . = '" + message + "']"))
            .isEmpty();
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
