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
package org.xwiki.gwt.wysiwyg.client.plugin.macro.input;

/**
 * Interface to an object that can have focus.
 * <p>
 * NOTE: We use our own interface and not the one provided by GWT {@link com.google.gwt.user.client.ui.HasFocus} because
 * we need only a way to focus the object without implementing {@link com.google.gwt.user.client.ui.SourcesFocusEvents}
 * and {@link com.google.gwt.user.client.ui.SourcesKeyboardEvents}.
 * 
 * @version $Id$
 */
public interface HasFocus
{
    /**
     * Gives or takes the focus to/from this object. Only one object can have focus at a time.
     * 
     * @param focused {@code true} to give the focus to this object, {@code false} to take it
     */
    void setFocus(boolean focused);
}
