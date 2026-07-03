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
package org.xwiki.repository.test.po;

import java.util.List;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.repository.test.po.edit.ExtensionSupporterInlinePage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * @version $Id$
 * @since 16.8.0RC1
 */
public class ExtensionSupportPage extends ViewPage
{
    public static final LocalDocumentReference LOCAL_REFERENCE =
        new LocalDocumentReference(List.of("Extension", "Support"), "WebHome");

    @FindBy(name = "ExtensionCode.ExtensionSupporterClass_0_name")
    private WebElement supporterNameInput;

    @FindBy(name = "register_supporter")
    private WebElement registerButton;

    public static ExtensionSupportPage gotoPage()
    {
        getUtil().gotoPage(LOCAL_REFERENCE);

        return new ExtensionSupportPage();
    }

    public void setSupporterName(String supporterName)
    {
        this.supporterNameInput.clear();
        this.supporterNameInput.sendKeys(supporterName);
    }

    public ExtensionSupporterInlinePage clickRegister()
    {
        this.registerButton.click();

        return new ExtensionSupporterInlinePage();
    }
}
