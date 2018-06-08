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
package org.xwiki.panels.test.po;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.WebElement;
import org.xwiki.index.tree.test.po.DocumentTreeElement;

/**
 * Represents the page tree from the navigation panel.
 * 
 * @version $Id$
 * @since 10.5RC1
 */
public class NavigationTreeElement extends DocumentTreeElement
{
    public NavigationTreeElement(WebElement element)
    {
        super(element);
    }

    public List<String> getTopLevelPages()
    {
        return getTopLevelNodes().stream().map(node -> node.getLabel()).collect(Collectors.toList());
    }
}
