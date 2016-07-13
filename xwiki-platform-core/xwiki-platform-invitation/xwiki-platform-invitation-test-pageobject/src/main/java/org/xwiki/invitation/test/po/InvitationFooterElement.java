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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the actions possible on the invitation sender page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class InvitationFooterElement extends BaseElement
{
    @FindBy(id = "invitation-footer")
    private WebElement footer;

    @FindBy(xpath = "//div[@id='invitation-footer']//a[@href='/xwiki/bin/view/Invitation/InvitationMemberActions?inspect=all']")
    private WebElement inspectAll;

    @FindBy(xpath = "//div[@id='invitation-footer']//a[@href='/xwiki/bin/view/Invitation/InvitationMemberActions?inspect=allAsAdmin']")
    private WebElement inspectAllAsAdmin;

    public boolean isAdmin()
    {
        return !getDriver().findElementsWithoutWaiting(this.footer, By.id("HAdministrativeTools")).isEmpty();
    }

    public InspectInvitationsPage inspectMyInvitations()
    {
        this.inspectAll.click();
        return new InspectInvitationsPage.AsUser();
    }

    public InspectInvitationsPage inspectAllInvitations()
    {
        if (!isAdmin()) {
            throw new WebDriverException("Inspection as admin impossible because user is not admin.");
        }
        this.inspectAllAsAdmin.click();
        return new InspectInvitationsPage.AsAdmin();
    }

    public int myPendingInvitations()
    {
        if (getDriver().findElementsWithoutWaiting(this.footer, By.id("my-pending-invitations")).size() == 0)
        {
            return 0;
        }
        String message = getDriver().findElementsWithoutWaiting(this.footer,
            By.id("my-pending-invitations")).get(0).getText().trim();
        return Integer.parseInt(message.substring(0, message.indexOf(" pending")));
    }

    public int spamReports()
    {
        if (getDriver().findElementsWithoutWaiting(this.footer, By.id("spam-reports")).size() == 0) {
            return 0;
        }
        String message = getDriver().findElementsWithoutWaiting(this.footer,
            By.id("spam-reports")).get(0).getText().trim();
        return Integer.parseInt(message.substring(0, message.indexOf(" reported as spam")));
    }
}
