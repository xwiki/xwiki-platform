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
package org.xwiki.scheduler.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

public class SchedulerPage extends ViewPage
{
    @FindBy(linkText = "Back to the job list")
    private WebElement backToHomeLink;

    @FindBy(xpath = "//div[@id= 'xwikicontent']//div[@class= 'code']")
    private WebElement script;

    public SchedulerHomePage backToHome()
    {
        this.backToHomeLink.click();
        return new SchedulerHomePage();
    }

    /**
     * @since 15.0RC1
     * @since 14.10.3
     */
    public String getScript()
    {
        return this.script.getText();
    }
}
