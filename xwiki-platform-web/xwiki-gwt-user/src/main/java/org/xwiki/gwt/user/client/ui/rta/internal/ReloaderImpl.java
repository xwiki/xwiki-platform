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
 * Browser specific code used to reload a rich text area.
 * 
 * @version $Id$
 */
public class ReloaderImpl
{
    /**
     * Detaches an in-line frame ensuring that the unload event is fired.
     * 
     * @param iFrame the in-line frame to unload
     */
    public void unloadIFrameElement(IFrameElement iFrame)
    {
        iFrame.getParentNode().removeChild(iFrame);
    }
}
