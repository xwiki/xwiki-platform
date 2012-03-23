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

import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the IRC.WebHome page.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class WebHomePage extends ViewPage
{
    /**
     * Opens the IRC Bot page.
     */
    public static WebHomePage gotoPage()
    {
        getUtil().gotoPage("IRC", "WebHome");
        return new WebHomePage();
    }

    public LiveTableElement getArchiveLiveTable()
    {
        LiveTableElement lt = new LiveTableElement("ircarchives");
        lt.waitUntilReady();
        return lt;
    }
}
