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
package org.xwiki.invitation.test.po;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BasePage;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.TableElement;

/**
 * Represents the actions possible on the invitation sender page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class InvitationSenderPage extends BasePage
{
    @FindBy(id = "invitation-sender-form")
    private WebElement form;

    private FormContainerElement formElement;

    @FindBy(xpath = "//form[@id='invitation-sender-form']/div/div/span/input[@type='submit'][@name='preview']")
    private WebElement previewButton;

    @FindBy(xpath = "//form[@id='invitation-sender-form']/div/div/span/input[@type='submit'][@name='sendMail']")
    private WebElement sendButton;

    @FindBy(id = "invitation-displaymessage")
    private WebElement preview;

    private InvitationMessageDisplayElement previewElement;

    public static InvitationSenderPage gotoPage()
    {
        getUtil().gotoPage(getURL());
        return new InvitationSenderPage();
    }

    public static String getURL()
    {
        return getUtil().getURL("Invitation", "WebHome");
    }

    public boolean userIsSpammer()
    {
        for (WebElement error : getDriver().findElementsWithoutWaiting(By.id("invitation-permission-error"))) {
            if (error.getText().equals(
                "Error\nA message which you sent was reported as spam and your privilege to send mail has"
                    + " been suspended pending investigation, we apologize for the inconvenience.")) {
                return true;
            }
        }
        return false;
    }

    public void fillInDefaultValues()
    {
        fillForm("user@localhost.localdomain", "This is a subject line.", "This is my message");
    }

    public void fillForm(final String recipients,
        final String subjectLine,
        final String messageBody)
    {
        Map<String, String> map = new HashMap<String, String>();
        if (recipients != null) {
            map.put("recipients", recipients);
        }
        if (subjectLine != null) {
            map.put("subjectLine", subjectLine);
        }
        if (messageBody != null) {
            map.put("messageBody", messageBody);
        }
        getForm().fillFieldsByName(map);
    }

    public InvitationSentPage send()
    {
        this.sendButton.click();
        return this.new InvitationSentPage();
    }

    public InvitationMessageDisplayElement preview()
    {
        this.previewButton.click();

        if (this.previewElement == null) {
            this.previewElement = new InvitationMessageDisplayElement(this.preview);
        }
        return this.previewElement;
    }

    public FormContainerElement getForm()
    {
        if (this.formElement == null) {
            this.formElement = new FormContainerElement(By.id("invitation-sender-form"));
        }
        return this.formElement;
    }

    public InvitationFooterElement getFooter()
    {
        return new InvitationFooterElement();
    }

    /** This page represents the invitation app after the send button has been pressed. */
    public class InvitationSentPage extends BasePage
    {
        @FindBy(id = "invitation-action-message")
        private WebElement messageBox;

        @FindBy(xpath = "//div[@class='message-table']/table")
        private WebElement table;

        public String getMessageBoxContent()
        {
            return this.messageBox.getText();
        }

        public TableElement getTable()
        {
            return new TableElement(this.table);
        }
    }
}
