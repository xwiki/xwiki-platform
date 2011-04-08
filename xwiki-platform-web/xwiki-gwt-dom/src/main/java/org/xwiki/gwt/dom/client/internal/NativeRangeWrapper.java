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
package org.xwiki.gwt.dom.client.internal;

import org.xwiki.gwt.dom.client.JavaScriptObject;

/**
 * Interface to access the native range object wrapped by a specific range implementation.
 * 
 * @version $Id$
 */
public interface NativeRangeWrapper
{
    /**
     * @return the native range wrapped by this object.
     */
    JavaScriptObject getNativeRange();

    /**
     * Sets the native range to be wrapped by this object.
     * 
     * @param nativeRange the native range to be wrapped by this object.
     */
    void setNativeRange(JavaScriptObject nativeRange);
}
