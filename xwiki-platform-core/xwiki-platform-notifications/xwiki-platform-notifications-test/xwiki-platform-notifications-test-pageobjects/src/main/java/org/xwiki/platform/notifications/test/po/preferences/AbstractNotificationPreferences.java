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
package org.xwiki.platform.notifications.test.po.preferences;

import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.BootstrapSwitch;

/**
 * Abstract class for notification preferences.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public abstract class AbstractNotificationPreferences extends BaseElement
{
    protected WebElement webElement;

    protected XWikiWebDriver driver;

    protected BootstrapSwitch alertSwitch;

    protected BootstrapSwitch emailSwitch;

    /**
     * Construct an AbstractNotificationPreferences.
     *
     * @param webElement table body of the application
     * @param driver the web driver
     */
    public AbstractNotificationPreferences(WebElement webElement, XWikiWebDriver driver)
    {
        this.webElement = webElement;
        this.driver = driver;
        this.alertSwitch = getSwitch("alert");
        this.emailSwitch = getSwitch("email");
    }

    /**
     * @return the state of the alert switch
     */
    public BootstrapSwitch.State getAlertState()
    {
        return alertSwitch.getState();
    }

    /**
     * @return the state of the email switch
     */
    public BootstrapSwitch.State getEmailState()
    {
        return emailSwitch.getState();
    }

    /**
     * Set the state of the alert switch.
     * @param state expected state
     * @throws Exception if the state is not valid
     */
    public void setAlertState(BootstrapSwitch.State state) throws Exception
    {
        alertSwitch.setState(state);
    }

    /**
     * Set the state of the email switch.
     * @param state expected state
     * @throws Exception if the state is not valid
     */
    public void setEmailState(BootstrapSwitch.State state) throws Exception
    {
        emailSwitch.setState(state);
    }

    /**
     * @param format the format of the notification
     * @return the switch associated to the given format
     */
    protected abstract BootstrapSwitch getSwitch(String format);
}
