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
package org.xwiki.gwt.dom.client;

import com.google.gwt.dom.client.Node;

/**
 * Exposes a JavaScript DOM attribute node in Java code.
 * 
 * @version $Id$
 */
public class Attribute extends Node
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected Attribute()
    {
        super();
    }

    /**
     * @return the name of the attribute
     */
    public final native String getName()
    /*-{
        return this.name;
    }-*/;

    /**
     * @return {@code true} if this attribute was explicitly given a value in the original document, {@code false}
     *         otherwise
     */
    public final native boolean isSpecified()
    /*-{
        return this.specified;
    }-*/;

    /**
     * @return the value of this attribute; character and general entity references are replaced with their values
     * @see Element#getAttribute(String)
     */
    public final native String getValue()
    /*-{
        return this.value;
    }-*/;

    /**
     * Sets the value of this attribute.
     * 
     * @param value the new value
     * @see Element#setAttribute(String, String)
     */
    public final native void setValue(String value)
    /*-{
        this.value = value;
    }-*/;

    /**
     * @return the element node this attribute is attached to or {@code null} if this attribute is not in use
     */
    public final native Element getOwnerElement()
    /*-{
        return this.ownerElement;
    }-*/;
}
