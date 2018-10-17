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

    private FormElement form;

    public abstract void clickRegister();

    public void fillInJohnSmithValues()
    {
        fillRegisterForm("John", "Smith", "JohnSmith", "WeakPassword", "WeakPassword", "johnsmith@xwiki.org");
    }

    public void fillRegisterForm(final String firstName, final String lastName, final String username,
        final String password, final String confirmPassword, final String email)
    {
        Map<String, String> map = new HashMap<String, String>();
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
        // There is a little piece of js which fills in the name for you.
        // This causes flickering if what's filled in is not cleared.
        if (username != null) {
            while (!username.equals(getForm().getFieldValue(By.name("xwikiname")))) {
                getForm().setFieldValue(By.name("xwikiname"), username);
            }
        }
    }

    private FormElement getForm()
    {
        if (this.form == null) {
            this.form = new FormElement(this.registerFormElement);
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
        return !getDriver().findElementsWithoutWaiting(By.xpath("//div[@id='mainContentArea']/script")).isEmpty();
    }
}
