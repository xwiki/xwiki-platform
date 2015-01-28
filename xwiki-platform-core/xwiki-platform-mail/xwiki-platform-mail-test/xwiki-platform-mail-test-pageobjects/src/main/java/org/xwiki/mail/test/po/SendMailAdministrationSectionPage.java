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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.AdministrationSectionPage;

/**
 * Represents the actions possible on the General Mail Administration Page.
 * 
 * @version $Id$
 * @since 6.4RC1
 */
public class SendMailAdministrationSectionPage extends AdministrationSectionPage
{
    @FindBy(id = "Mail.SendMailConfigClass_0_host")
    private WebElement host;

    @FindBy(id = "Mail.SendMailConfigClass_0_port")
    private WebElement port;

    @FindBy(id = "Mail.SendMailConfigClass_0_sendWaitTime")
    private WebElement sendWaitTime;

    @FindBy(id = "Mail.SendMailConfigClass_0_discardSuccessStatuses")
    private WebElement discardSuccessStatuses;

    public SendMailAdministrationSectionPage()
    {
        super("Mail Sending");
    }

    public String getHost()
    {
        return this.host.getAttribute("value");
    }

    public void setHost(String host)
    {
        this.host.clear();
        this.host.sendKeys(host);
    }

    public String getPort()
    {
        return this.port.getAttribute("value");
    }

    public void setPort(String port)
    {
        this.port.clear();
        this.port.sendKeys(port);
    }

    public String getSendWaitTime()
    {
        return this.sendWaitTime.getAttribute("value");
    }

    public void setSendWaitTime(String sendWaitTime)
    {
        this.sendWaitTime.clear();
        this.sendWaitTime.sendKeys(sendWaitTime);
    }

    /**
     * @since 6.4.1
     */
    public void setDiscardSuccessStatuses(boolean discardSuccessStatuses)
    {
        if (getDiscardSuccessStatuses() != discardSuccessStatuses) {
            this.discardSuccessStatuses.click();
        }
    }

    /**
     * @since 6.4.1
     */
    public boolean getDiscardSuccessStatuses()
    {
        return this.discardSuccessStatuses.isSelected();
    }
}
