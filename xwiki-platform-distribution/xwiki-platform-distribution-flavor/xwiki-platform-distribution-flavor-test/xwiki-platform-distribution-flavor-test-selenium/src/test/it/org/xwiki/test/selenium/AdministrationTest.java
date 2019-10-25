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
package org.xwiki.test.selenium;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationMenu;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.selenium.framework.AbstractXWikiTestCase;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import static org.junit.Assert.*;

/**
 * Verify the overall Administration application features.
 * 
 * @version $Id$
 */
public class AdministrationTest extends AbstractXWikiTestCase
{
    /**
     * Test functionality of the ForgotUsername page:
     * <ul>
     * <li>A user can be found using correct email</li>
     * <li>No user is found using wrong email</li>
     * <li>Email text is properly escaped</li>
     * </ul>
     */
    @Test
    public void testForgotUsername()
    {
        String space = "Test";
        String page = "SQLTestPage";
        String mail = "webmaster@xwiki.org"; // default Admin mail
        String user = "Admin";
        String badMail = "bad_mail@evil.com";

        // Ensure there is a page we will try to find using HQL injection
        editInWikiEditor(space, page);
        setFieldValue("title", page);
        setFieldValue("content", page);
        clickEditSaveAndView();

        // test that it finds the correct user
        open("XWiki", "ForgotUsername");
        setFieldValue("e", mail);
        submit("//input[@type='submit']"); // there are no other buttons
        assertTextNotPresent("No account is registered using this email address");
        assertElementPresent("//div[@id='xwikicontent']//strong[text()='" + user + "']");

        // test that bad mail results in no results
        open("XWiki", "ForgotUsername");
        setFieldValue("e", badMail);
        submit("//input[@type='submit']"); // there are no other buttons
        assertTextPresent("No account is registered using this email address");
        assertElementNotPresent("//div[@id='xwikicontent']//strong[@value='" + user + "']");

        // XWIKI-4920 test that the email is properly escaped
        open("XWiki", "ForgotUsername");
        setFieldValue("e", "a' synta\\'x error");
        submit("//input[@type='submit']"); // there are no other buttons
        assertTextPresent("No account is registered using this email address");
        assertTextNotPresent("Error");
    }
}
