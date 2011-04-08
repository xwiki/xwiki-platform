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

import com.google.gwt.core.client.JsArrayString;

/**
 * Unit tests for {@link JavaScriptObject}.
 * 
 * @version $Id$
 */
public class JavaScriptObjectTest extends DOMTestCase
{
    /**
     * Unit test for {@link JavaScriptObject#set(String, Object)} and {@link JavaScriptObject#get(String)}.
     */
    public void testSetGet()
    {
        JavaScriptObject object = JavaScriptObject.createObject().cast();

        String green = "green";
        String color = "color";
        assertNull(object.get(color));

        object.set(color, green);
        assertEquals(green, object.get(color));

        object.remove(color);
        assertNull(object.get(color));
    }

    /**
     * Unit test for {@link JavaScriptObject#getKeys()}.
     */
    public void testGetKeys()
    {
        JavaScriptObject object = JavaScriptObject.createObject().cast();

        assertEquals(0, object.getKeys().length());

        String name = "name";
        object.set(name, "alice");
        JsArrayString keys = object.getKeys();
        assertEquals(1, keys.length());
        assertEquals(name, keys.get(0));

        object.remove(name);
        assertEquals(0, object.getKeys().length());
    }

    /**
     * Unit test for {@link JavaScriptObject#remove(String)}.
     */
    public void testRemove()
    {
        JavaScriptObject object = JavaScriptObject.createObject().cast();

        String pet = "pet";
        String dog = "dog";
        object.set(pet, dog);
        assertEquals(1, object.getKeys().length());

        String owner = "owner";
        String bob = "bob";
        object.set(owner, bob);
        assertEquals(2, object.getKeys().length());

        assertEquals(dog, object.remove(pet));
        assertEquals(1, object.getKeys().length());
        assertEquals(bob, object.remove(owner));
        assertEquals(0, object.getKeys().length());
    }

    /**
     * Unit test for {@link JavaScriptObject#remove(String)} when the target JavaScript object is the window.
     */
    public void testRemoveOnWindow()
    {
        Window window = Window.get();
        String city = "city";
        String paris = "Paris";
        window.set(city, paris);
        assertEquals(paris, window.get(city));
        assertEquals(paris, window.remove(city));
        assertNull(window.get(city));
    }
}
