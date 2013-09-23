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

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.IFrameElement;

/**
 * Extends {@link ReloaderImpl} for older versions of the Internet Explorer browser (6, 7 and 8).
 * 
 * @version $Id$
 */
public class ReloaderImplIEOld extends ReloaderImpl
{
    @Override
    public void unloadIFrameElement(IFrameElement iFrame)
    {
        // IE doesn't unload the in-line frame when it is removed from the document.
        ((Document) iFrame.getContentDocument()).open();
        super.unloadIFrameElement(iFrame);
    }
}
