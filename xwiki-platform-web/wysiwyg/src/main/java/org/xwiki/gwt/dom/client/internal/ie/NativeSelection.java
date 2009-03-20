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
package org.xwiki.gwt.dom.client.internal.ie;

import org.xwiki.gwt.dom.client.Document;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Wraps the selection JavaScript object provided by Internet Explorer.
 * 
 * @version $Id$
 */
public final class NativeSelection extends JavaScriptObject
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected NativeSelection()
    {
    }

    /**
     * @param doc The DOM document for which to retrieve the selection object.
     * @return The selection object associated with the given in-line frame.
     */
    public static synchronized native NativeSelection getInstance(Document doc)
    /*-{
        var selection = doc.selection;
        selection.ownerDocument = doc;
        return selection;
    }-*/;

    /**
     * Retrieves the type of selection.
     * 
     * @return One of the following values:
     *         <ul>
     *         <li>none: No selection/insertion point.</li>
     *         <li>text: Specifies a text selection.</li>
     *         <li>control: Specifies a control selection, which enables dimension controls allowing the selected object
     *         to be resized.</li>
     *         </ul>
     */
    public native String getType()
    /*-{
        return this.type;
    }-*/;

    /**
     * Clears the contents of the selection.
     */
    public native void clear()
    /*-{
        this.clear();
    }-*/;

    /**
     * Creates a TextRange object from the current text selection, or a ControlRange object from a control selection.
     * 
     * @return The created range object.
     */
    public native NativeRange createRange()
    /*-{
        var range = this.createRange();
        range.ownerDocument = this.ownerDocument;
        return range;
    }-*/;

    /**
     * Cancels the current selection and sets the selection type to none.
     */
    public native void empty()
    /*-{
        this.empty();
    }-*/;

    /**
     * @return The document associated with this selection object.
     */
    public native Document getOwnerDocument()
    /*-{
        return this.ownerDocument;
    }-*/;
}
