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

import java.util.List;
import java.util.ArrayList;

import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriverException;
import org.xwiki.test.ui.po.BasePage;
import org.xwiki.test.ui.po.TableElement;

/**
 * Represents the actions possible when inspecting invitations.
 *
 * @version $Id$
 * @since 4.2M1
 */
public abstract class InspectInvitationsPage extends BasePage
{
    private InvitationFooterElement footer = new InvitationFooterElement();

    @FindBy(tagName = "table")
    private WebElement tableWebEl;

    public TableElement getTable()
    {
        return new TableElement(tableWebEl);
    }

    public InvitationFooterElement getFooter() {
        return footer;
    }

    /** If there is a message box telling the status and memo the content is returned. */
    public String getStatusAndMemo()
    {
        List<WebElement> elements = getDriver().findElementsWithoutWaiting(By.id("message-status-and-memo"));
        if (elements.size() > 0) {
            return elements.get(0).getText();
        }
        return null;
    }

    public OneMessage getMessageWhere(String columnName, String value)
    {
        List<String> columnEntries = new ArrayList<String>();
        List<WebElement> column = getTable().getColumn(columnName);
        for (WebElement cell : column) {
            if (cell.getText().equals(value)) {
                // Get the Subject element in the same row and look inside for a link.
                WebElement link = getDriver().findElementsWithoutWaiting(
                    getTable().getColumn("Subject").get(column.indexOf(cell)), By.tagName("a")).get(0);
                link.click();
                return null;
            }
            columnEntries.add(cell.getText());
        }
        throw new WebDriverException("Could not find message with " + column + " equal to "
                                     + value + "\nIn columbn with entries: " + columnEntries.toString());
    }

    /** Should only be made available to OneMessage implementations. */
    protected TableElement clickMessageHistory()
    {
        getTable().getColumn("Message History").get(1).findElements(By.tagName("a")).get(0).click();
        return new TableElement(getDriver().findElement(By.id("message-history-table")).findElement(By.tagName("table")));
    }

    public static interface OneMessage
    {
        public InvitationMessageDisplayElement getMessage();

        /** Returns the message telling the user that they successfully set the status to not spam */
        public String notSpam(String message);

        public InvitationActionConfirmationElement cancel();

        public String getStatusAndMemo();

        public TableElement clickMessageHistory();
    }

    public static class AsAdmin extends InspectInvitationsPage
    {
        @Override
        public OneMessage getMessageWhere(String column, String value)
        {
            super.getMessageWhere(column, value);
            return this.new OneMessage();
        }

        public class OneMessage extends AsAdmin implements InspectInvitationsPage.OneMessage
        {
            @FindBy(id = "invitation-displaymessage")
            private WebElement preview;

            @FindBy(name = "doAction_notSpam")
            private WebElement notSpamButton;

            @Override
            public InvitationMessageDisplayElement getMessage()
            {
                return new InvitationMessageDisplayElement(preview);
            }

            @Override
            public TableElement clickMessageHistory()
            {
                return super.clickMessageHistory();
            }

            @Override
            public InvitationActionConfirmationElement cancel()
            {
                throw new WebDriverException("Invitation cannot be canceled as an admin");
            }

            @Override
            public String notSpam(String message)
            {
                notSpamButton.click();
                InvitationActionConfirmationElement confirm = new InvitationActionConfirmationElement();
                // We can't go forward unless we are on the right form.
                if (!confirm.getLabel().equalsIgnoreCase("Synopsis of findings and/or action taken")) {
                    throw new WebDriverException("Not on 'not spam' confirm page, message says: " + confirm.getLabel());
                }
                confirm.setMemo(message);
                return confirm.confirm();
            }
        }
    }

    public static class AsUser extends InspectInvitationsPage
    {
        @FindBy(name = "doAction_cancel")
        protected WebElement cancelButton;

        @Override
        public OneMessage getMessageWhere(String column, String value)
        {
            super.getMessageWhere(column, value);
            return this.new OneMessage();
        }

        public class OneMessage extends AsUser implements InspectInvitationsPage.OneMessage
        {
            @FindBy(id = "invitation-displaymessage")
            private WebElement preview;

            @Override
            public InvitationMessageDisplayElement getMessage()
            {
                return new InvitationMessageDisplayElement(preview);
            }

            @Override
            public TableElement clickMessageHistory()
            {
                return super.clickMessageHistory();
            }

            @Override
            public InvitationActionConfirmationElement cancel()
            {
                cancelButton.click();
                return new InvitationActionConfirmationElement();
            }

            @Override
            public String notSpam(String message)
            {
                throw new WebDriverException("Function only possible for admin.");
            }
        }
    }
}
