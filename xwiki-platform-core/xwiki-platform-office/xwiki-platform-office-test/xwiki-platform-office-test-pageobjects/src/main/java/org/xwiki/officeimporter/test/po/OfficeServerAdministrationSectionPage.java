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
package org.xwiki.officeimporter.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.AdministrationSectionPage;

/**
 * @since 7.3M1
 * @version $Id$
 */
public class OfficeServerAdministrationSectionPage extends AdministrationSectionPage
{
    private static final String SERVERSTATE_XPATH =
        "//label[contains(text(), 'Server state')]/../following-sibling::dd";

    @FindBy(xpath = SERVERSTATE_XPATH)
    private WebElement serverState;

    @FindBy(css = "input[name='action'][value='start']")
    private WebElement startServerOption;

    @FindBy(css = "input[name='action'][value='stop']")
    private WebElement stopServerOption;

    @FindBy(css = "input[name='action'][value='restart']")
    private WebElement restartServerOption;

    @FindBy(css = "input[type='submit'][value='Update']")
    private WebElement updateButton;
    
    public OfficeServerAdministrationSectionPage()
    {
        super("XWiki.OfficeImporterAdmin");
    }
    
    public String getServerState()
    {
        return serverState.getText();
    }
    
    public void startServer()
    {
        startServerOption.click();
        updateButton.click();

        // Wait for the state to be connected
        getDriver().waitUntilElementHasTextContent(By.xpath(SERVERSTATE_XPATH), "Connected");
    }

    public void stopServer()
    {
        stopServerOption.click();
        updateButton.click();
    }
    
    public void restartServer()
    {
        restartServerOption.click();
        updateButton.click();
    }
}
