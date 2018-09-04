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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * Wrap a bootstrap switch widget (http://bootstrapswitch.com/).
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Unstable
public class BootstrapSwitch
{
    /**
     * The different states the switch can have.
     */
    public enum State
    {
        /**
         * State is 'ON'.
         */
        ON,

        /**
         * State is 'OFF'.
         */
        OFF,

        /**
         * State is 'UNDETERMINED', which is a special state a bootstrap switch can have (neither ON or OFF).
         */
        UNDETERMINED
    }

    private WebElement webElement;

    private XWikiWebDriver driver;

    /**
     * Construct a bootstrap switch.
     * @param webElement the element the the wrapper will handle
     * @param driver the xwiki web driver
     */
    public BootstrapSwitch(WebElement webElement, XWikiWebDriver driver)
    {
        this.webElement = webElement;
        this.driver = driver;
    }

    /**
     * @return the state of the switch
     */
    public State getState()
    {
        if (isSwitchIndeterminate()) {
            return State.UNDETERMINED;
        }
        return isSwitchOn() ? State.ON : State.OFF;
    }

    private boolean isSwitchOn()
    {
        return getHTMLClass().contains("bootstrap-switch-on");
    }

    private boolean isSwitchIndeterminate()
    {
        return getHTMLClass().contains("bootstrap-switch-indeterminate");
    }

    private String getHTMLClass()
    {
        return webElement.getAttribute("class");
    }

    /**
     * Click on the switch.
     */
    public void click()
    {
        State originalState = getState();
        this.webElement.findElement(By.className("bootstrap-switch-label")).click();
        driver.waitUntilCondition(webDriver -> getState() != originalState);
    }

    /**
     * Set the state of the switch with the given value.
     * @param wantedState the expected state
     * @throws Exception if the expected state cannot be set
     */
    public void setState(State wantedState) throws Exception
    {
        if (wantedState == State.UNDETERMINED) {
            throw new Exception("It is not possible to set a switch to undetermined state manually.");
        }
        int tries = 0;
        while (wantedState != getState()) {
            if (tries++ > 2) {
                throw new Exception("Failed to give the desired state.");
            }
            click();
        }
    }

    /**
     * @return either or not the input is enabled (meaning we can click on it)
     *
     * @since 10.8RC1
     * @since 9.11.8
     */
    public boolean isEnabled()
    {
        return webElement.findElement(By.tagName("input")).isEnabled();
    }
}
