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
package org.xwiki.extension.test.po.distribution;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The Orphaned Dependencies step UI.
 *
 * @version $Id$
 * @since 11.10
 */
public class CleanApplyFinalizeDistributionStep extends AbstractDistributionPage
{
    @FindBy(xpath = "//button[@name='cleanapplyreport']")
    private WebElement continueButton;

    public void waitForUninstallComplete()
    {
        // Skip this step when the progress bar is not needed (i.e., no dependencies are checked in the orphaned 
        // dependencies list).
        if (!isCompleteStepDisabled()) {
            getDriver().waitUntilElementIsVisible(By.xpath("//button[@name='cleanapplyreport' and not(@disabled)]"));
        }
    }

    public CleanApplyReportDistributionStep clickContinue()
    {
        // Skip this step when the progress bar is not needed (i.e., no dependencies are checked in the orphaned 
        // dependencies list).
        if (!isCompleteStepDisabled()) {
            // FIXME: workaround for https://github.com/mozilla/geckodriver/issues/1026
            getDriver().addPageNotYetReloadedMarker();
            this.continueButton.click();
            getDriver().waitUntilPageIsReloaded();
        }
        return new CleanApplyReportDistributionStep();
    }
}
