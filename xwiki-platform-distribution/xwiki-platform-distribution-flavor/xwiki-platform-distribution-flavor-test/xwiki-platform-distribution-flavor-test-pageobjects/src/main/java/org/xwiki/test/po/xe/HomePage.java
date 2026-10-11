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
package org.xwiki.test.po.xe;

import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the Home Page.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class HomePage extends ViewPage
{
    /** The name of the space holding the home page. */
    private static final String SPACE = "Main";

    /** The name of the home page document. */
    private static final String PAGE = "WebHome";

    /**
     * Opens the home page.
     *
     * @return the home page object
     */
    public static HomePage gotoPage()
    {
        getUtil().gotoPage(SPACE, PAGE);
        return new HomePage();
    }

    /**
     * @return the URL of the home page
     */
    public String getURL()
    {
        return getUtil().getURL(SPACE, PAGE);
    }

    /**
     * @return {@code true} if the browser is currently on the home page
     */
    public boolean isOnHomePage()
    {
        return getDriver().getCurrentUrl().equals(getURL());
    }
}
