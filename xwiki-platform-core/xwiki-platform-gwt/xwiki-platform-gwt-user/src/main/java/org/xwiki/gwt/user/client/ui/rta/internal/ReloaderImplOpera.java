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
package org.xwiki.gwt.user.client.ui.rta.internal;

import org.xwiki.gwt.dom.client.IFrameElement;

/**
 * Extends {@link ReloaderImpl} with implementation for Opera browser.
 * 
 * @version $Id$
 */
public class ReloaderImplOpera extends ReloaderImpl
{
    @Override
    public void unloadIFrameElement(IFrameElement iFrame)
    {
        // Opera doesn't fire the unload event when the in-line frame is detached. Setting the source attribute will do.
        iFrame.setSrc(iFrame.getSrc());
        super.unloadIFrameElement(iFrame);
    }
}
