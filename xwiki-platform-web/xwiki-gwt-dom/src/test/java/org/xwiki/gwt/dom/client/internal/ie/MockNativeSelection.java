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

import org.xwiki.gwt.dom.client.JavaScriptObject;

/**
 * A mock native selection to be used in unit tests. It holds a reference to a given native range which is always
 * returned.
 * 
 * @version $Id$
 */
public final class MockNativeSelection extends JavaScriptObject
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected MockNativeSelection()
    {
    }

    /**
     * @param nativeRange a reference to the native range which will always be returned by the new mock native selection
     * @return a new mock native selection object casted to {@link NativeSelection}
     */
    public static native NativeSelection newInstance(NativeRange nativeRange)
    /*-{
        return {
            ownerDocument : nativeRange.ownerDocument,
            createRange : function() {
                return nativeRange;
            }
        };
    }-*/;
}
