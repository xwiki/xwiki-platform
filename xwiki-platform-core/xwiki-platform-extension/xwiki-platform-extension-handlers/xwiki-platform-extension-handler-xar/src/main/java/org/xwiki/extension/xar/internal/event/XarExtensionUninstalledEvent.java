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
package org.xwiki.extension.xar.internal.event;

import org.xwiki.extension.event.ExtensionUninstalledEvent;

/**
 * An event triggered when a XAR extension has been uninstalled. See the {@link ExtensionUninstalledEvent} for more
 * information.
 * 
 * @version $Id$
 * @since 11.10
 */
public class XarExtensionUninstalledEvent extends WrappingExtensionEvent
{
    /**
     * Matches all XAR extensions.
     */
    public XarExtensionUninstalledEvent()
    {
        this(new ExtensionUninstalledEvent());
    }

    /**
     * Wraps the given {@link ExtensionUninstalledEvent}.
     *
     * @param originalEvent the original {@link ExtensionUninstalledEvent}
     */
    public XarExtensionUninstalledEvent(ExtensionUninstalledEvent originalEvent)
    {
        super(originalEvent);
    }
}
