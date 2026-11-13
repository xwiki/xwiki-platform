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
package org.xwiki.menu.test.po;

import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.livedata.test.po.LiveDataElement;

/**
 * Represents the Menu.WebHome page.
 *
 * @version $Id$
 * @since 10.6RC1
 */
public class MenuHomePage extends ApplicationHomePage
{
    private static final String SPACE = "Menu";

    /**
     * Go to the home page of the Menu application.
     *
     * @return the page object for the Menu application home page
     */
    public static MenuHomePage gotoPage()
    {
        ApplicationHomePage.gotoPage(SPACE);
        return new MenuHomePage();
    }

    /**
     * @return the name of the space holding the Menu application home page
     */
    public static String getSpace()
    {
        return SPACE;
    }

    /**
     * @return the name of the Menu application home page
     */
    public static String getPage()
    {
        return "WebHome";
    }

    /**
     * @return the Live Data page object of the Menu home page
     * @since 13.5RC1
     */
    public LiveDataElement getLiveData()
    {
        return new LiveDataElement("menu");
    }
}
