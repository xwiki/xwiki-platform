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
package org.xwiki.livedata.test.po;

import org.xwiki.test.ui.po.BaseElement;

/**
 * Live Data page object. Provides the operations to obtain the page objects for the different live data layouts, and to
 * switch between them.
 *
 * @version $Id$
 * @since 13.4RC1
 * @since 12.10.9
 */
public class LiveDataElement extends BaseElement
{
    private final String id;

    /**
     * Default constructor. Initializes a Live Data page object by its id.
     *
     * @param id the live data id
     */
    public LiveDataElement(String id)
    {
        this.id = id;
    }

    /**
     * @return a table layout page object for the live data
     */
    public TableLayoutElement getTableLayout()
    {
        TableLayoutElement tableLayoutElement = new TableLayoutElement(this.id);
        tableLayoutElement.waitUntilReady();
        return tableLayoutElement;
    }
    
}
