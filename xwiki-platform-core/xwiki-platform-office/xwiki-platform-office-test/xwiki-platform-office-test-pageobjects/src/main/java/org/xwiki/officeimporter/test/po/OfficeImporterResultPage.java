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
import org.xwiki.test.ui.po.ViewPage;

/**
 * @since 7.3M1
 * @version $Id$
 */
public class OfficeImporterResultPage extends ViewPage
{
    @FindBy(css = "#xwikicontent div.box.infomessage")
    private WebElement message;
    
    public String getMessage()
    {
        return message.getText();
    }
    
    public ViewPage viewResult()
    {
        for (WebElement link : message.findElements(By.tagName("a"))) {
            if (link.getText().equals("result")) {
                link.click();
                return new ViewPage();
            }
        }
        throw new RuntimeException("Failed to click on the result link.");
    }
}
