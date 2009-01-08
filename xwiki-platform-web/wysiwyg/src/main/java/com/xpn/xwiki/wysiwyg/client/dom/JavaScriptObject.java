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
package com.xpn.xwiki.wysiwyg.client.dom;

/**
 * Extends GWT JavaScriptObject to add a fromJson method. Usage : <code>
 * public class MyCar extends JavaScriptObject {   
 *     protected MyCar() {}
 *     public final native int getWheelNumber() / *-{ return this.wheelnb; }-* /;
 *     public final native String getColor() / *-{ return this.color; }-* /;
 * }
 * 
 * MyCar redcar = (MyCar) MyCar.fromJson("{ wheelnb: 4, color: 'red' }");
 * redcar.getWheelNumber();
 * redcar.getColor();
 * </code>
 * 
 * @see com.google.gwt.core.client.JavaScriptObject
 * @version $Id$
 */
public class JavaScriptObject extends com.google.gwt.core.client.JavaScriptObject
{
    /**
     * Default constructor. Overlay types always have protected, zero-arguments constructors.
     */
    protected JavaScriptObject()
    {
    }

    /**
     * Create a JavaScriptObject from a JSON string.
     * 
     * @param input a valid JSON string.
     * @return resulting JavaScriptObject
     */
    public static final native JavaScriptObject fromJson(String input)
    /*-{ 
         return eval('(' + input + ')') 
    }-*/;

    /**
     * Returns the reference stored in this JavaScript object for the given key.
     * 
     * @param key the key whose value to return
     * @return the value of the specified key
     */
    public final native Object get(String key)
    /*-{
        return this[key];
    }-*/;

    /**
     * Saves the given reference in this JavaScript object using the specified key.
     * 
     * @param key the string used for storing and retrieving the reference
     * @param ref the object whose reference will be stored
     * @return the previous reference associated with the given key
     */
    public final native Object set(String key, Object ref)
    /*-{
        var oldRef = this[key];
        this[key] = ref;
        return oldRef;
    }-*/;
}
