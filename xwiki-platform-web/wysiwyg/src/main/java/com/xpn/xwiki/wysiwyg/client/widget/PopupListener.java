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
package com.xpn.xwiki.wysiwyg.client.widget;

/**
 * Interface that must be implemented in order to be notified of popup events.
 * 
 * @version $Id$
 */
public interface PopupListener
{
    /**
     * Notifies that the given popup was closed.
     * 
     * @param sender The popup that was closed.
     * @param autoClosed Specifies if the popup was closed by clicking outside of it (when the popup auto hides).
     */
    void onPopupClosed(SourcesPopupEvents sender, boolean autoClosed);
}
