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
package org.xwiki.activeinstalls.test.po;

import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the ActiveInstalls.WebHome page.
 *
 * @version $Id$
 * @since 6.1M1
 */
public class ActiveInstallsHomePage extends ViewPage
{
    /**
     * Opens the home page.
     */
    public static ActiveInstallsHomePage gotoPage()
    {
        getUtil().gotoPage(getSpace(), getPage());
        return new ActiveInstallsHomePage();
    }

    public static String getSpace()
    {
        return "ActiveInstalls";
    }

    public static String getPage()
    {
        return "WebHome";
    }
}
