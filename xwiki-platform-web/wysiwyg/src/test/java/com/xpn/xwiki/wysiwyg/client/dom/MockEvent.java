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
 * Instances of this class can be used for testing the response of the rich text area to different DOM events. Such
 * functional tests are useful especially because they can be run on different browsers (including IE) which currently
 * doesn't happen with the Selenium functional tests.
 * 
 * @version $Id$
 */
public final class MockEvent extends Event
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected MockEvent()
    {
    }

    /**
     * Creates a new mock event.
     * 
     * @return the newly created mock event
     */
    public static native MockEvent newInstance()
    /*-{
        return {
            preventDefault: function() {}
        };
    }-*/;

    /**
     * Sets the type of this event.
     * 
     * @see Event#getType()
     * @param type the name of the event
     */
    public native void setType(String type)
    /*-{
        this.type = type;
    }-*/;

    /**
     * @param keyCode the Unicode value of the key that has been pressed
     */
    public native void setKeyCode(int keyCode)
    /*-{
        this.keyCode = keyCode;
    }-*/;
}
