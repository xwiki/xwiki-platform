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
import org.xwiki.gwt.dom.client.Element;

/**
 * A control range is a list of DOM elements. When an element is added to a control range and that range is selected the
 * element will be decorated with special markers for editing that element. For instance if an image is selected this
 * way the user will be able to resize the image.
 * 
 * @version $Id$
 */
public final class ControlRange extends NativeRange
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected ControlRange()
    {
    }

    /**
     * Creates a new control range from the given document.
     * 
     * @param doc The owner document of the created control range.
     * @return A new control range within the specified document.
     */
    public static native ControlRange newInstance(Document doc)
    /*-{
        var controlRange = doc.body.createControlRange();
        controlRange.ownerDocument = doc;
        return controlRange;
    }-*/;

    /**
     * @return The number of objects included the range.
     */
    public native int getLength()
    /*-{
        return this.length;
    }-*/;

    /**
     * Adds an element to the control range.
     * <p>
     * Not all elements support control selection. If you try to add an element that doesn't support control selection
     * to a control range you will get sooner or later an exception (e.g. "Invalid argument"):
     * <ul>
     * <li>in IE8 standards mode the exception is thrown when you try to select the range (so you can add any element to
     * a control range but then you cannot select it)</li>
     * <li>otherwise the exception is thrown when you add an invalid element to a control range (so by this method)</li>
     * </ul>
     * 
     * @param element The element to include in this range.
     */
    public native void add(Element element)
    /*-{
        this.addElement(element);
    }-*/;

    /**
     * @param index Zero-based index of the element to get.
     * @return The element at the specified index in this control range.
     */
    public native Element get(int index)
    /*-{
        return this.item(index);
    }-*/;

    /**
     * Removes an element from this control range.
     * 
     * @param index Zero-based index of the element to remove from the control range.
     */
    public native void remove(int index)
    /*-{
        this.remove(index);
    }-*/;

    /**
     * @return a duplicate of this control range
     */
    public ControlRange duplicate()
    {
        ControlRange clone = newInstance(getOwnerDocument());
        for (int i = 0; i < getLength(); i++) {
            clone.add(get(i));
        }
        return clone;
    }

    /**
     * Tests if this control range equals the given control range.
     * 
     * @param other the control range to compare with this control range
     * @return {@code true} if the given control range is equal to this control rage, {@code false} otherwise
     */
    public boolean isEqual(ControlRange other)
    {
        if (this == other) {
            return true;
        }
        if (other != null && getLength() == other.getLength()) {
            for (int i = 0; i < getLength(); i++) {
                if (get(i) != other.get(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
