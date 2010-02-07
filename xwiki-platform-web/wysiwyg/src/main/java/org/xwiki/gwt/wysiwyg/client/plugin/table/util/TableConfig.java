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
package org.xwiki.gwt.wysiwyg.client.plugin.table.util;

import org.xwiki.gwt.dom.client.JavaScriptObject;

/**
 * @version $Id$
 */
public class TableConfig extends JavaScriptObject
{
    /**
     * Default constructor. Overlay types always have protected, zero-arguments constructors.
     */
    protected TableConfig()
    {
    }

    /**
     * Number of rows in the table.
     * 
     * @return number of rows.
     */
    public final native int getRowNumber()
    /*-{ 
        return this.rows; 
    }-*/;

    /**
     * Number of columns in the table.
     * 
     * @return number of columns.
     */
    public final native int getColNumber()
    /*-{ 
        return this.cols; 
    }-*/;
    
    /**
     * Size of table border.
     * 
     * @return size of border in pixels.
     */
    public final native int getBorderSize()
    /*-{ 
        return this.borderSize; 
    }-*/;
    
    /**
     * Does the table contain a header row.
     * 
     * @return true if the table has a header row.
     */
    public final native boolean hasHeader()
    /*-{ 
        return this.header; 
    }-*/;
}
