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
package org.xwiki.blocknote.test.po;

import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the side menu displayed next to the block hovered in the BlockNote rich text area.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class SideMenu extends BaseElement
{
    /**
     * Waits for the side menu of the hovered block to be fully displayed.
     */
    public SideMenu()
    {
        // The editor fades the side menu in, so it is already displayed while still half transparent.
        getDriver().waitUntilJavascriptCondition("const sideMenu = document.querySelector('.bn-side-menu');"
            + "return !!sideMenu && getComputedStyle(sideMenu.parentElement).opacity === '1'");
    }
}
