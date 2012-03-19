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
package org.xwiki.ircbot.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.FormElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

/**
 * Represents actions that can be done on the IRC.IRCConfiguration page.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class IRCConfigurationPage extends ViewPage
{
    /**
     * Opens the page.
     */
    public static IRCConfigurationPage gotoPage()
    {
        getUtil().gotoPage("IRC", "IRCConfiguration");
        return new IRCConfigurationPage();
    }

    public void setLoggingPage(String name)
    {
        ObjectEditPage oep = editObjects();
        FormElement form = oep.getObjectsOfClass("IRC.LoggingBotListener").get(0);
        form.setFieldValue(By.id("IRC.LoggingBotListener_0_page"), name);
        oep.clickSaveAndContinue();
    }
}
