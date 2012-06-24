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
package org.xwiki.gwt.dom.client.internal.mozilla;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.internal.DefaultSelection;

/**
 * Fixes selection problems found in Mozilla browsers.
 * 
 * @version $Id$
 */
public class MozillaSelection extends DefaultSelection
{
    /**
     * Creates a new selection object.
     * 
     * @param nativeSelection the underlying native selection to be used
     */
    public MozillaSelection(NativeSelection nativeSelection)
    {
        super(nativeSelection);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes the ghost resize handlers (visible around images and tables) as well as other trailing graphics.
     * 
     * @see DefaultSelection#removeAllRanges()
     */
    @Override
    public void removeAllRanges()
    {
        if (getRangeCount() > 0) {
            // Select all. Apparently this has the effect of removing ghost resize handlers and other trailing graphics.
            ((Document) getNativeSelection().getRangeAt(0).getStartContainer().getOwnerDocument()).execCommand(
                "selectall", null);
        }
        super.removeAllRanges();
    }
}
