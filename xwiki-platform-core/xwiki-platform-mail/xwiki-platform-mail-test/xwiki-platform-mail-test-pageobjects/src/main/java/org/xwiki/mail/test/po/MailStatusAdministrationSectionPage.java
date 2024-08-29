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
package org.xwiki.mail.test.po;

import org.openqa.selenium.By;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.livedata.test.po.LiveDataElement;

/**
 * Represents the actions possible on the Mail Status Administration Page.
 *
 * @version $Id$
 * @since 6.4RC1
 */
public class MailStatusAdministrationSectionPage extends AdministrationSectionPage
{
    public MailStatusAdministrationSectionPage()
    {
        super("Mail Sending Status");
    }

    public static MailStatusAdministrationSectionPage gotoPage()
    {
        gotoPage("emailStatus");
        return new MailStatusAdministrationSectionPage();
    }

    /**
     * @return the status live data
     */
    public LiveDataElement getLiveData()
    {
        return new LiveDataElement("sendmailstatus");
    }

    /**
     * Click on a mail action.
     *
     * @param rowNumber the row number in which to call the actions (start at 1 for the first row)
     * @param actionName the name of the action (e.g., {@code mailsendingaction_resend})
     */
    public void clickAction(int rowNumber, String actionName)
    {
        getLiveData().getTableLayout().clickAction(rowNumber, By.cssSelector(String.format("[name='%s']", actionName)));
    }
}
