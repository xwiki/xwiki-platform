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
package org.xwiki.gwt.user.client.ui.rta;

import java.util.Map;

import org.xwiki.gwt.user.client.ui.rta.internal.ReloaderImpl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.LoadHandler;

/**
 * Reloads a rich text area.
 * 
 * @version $Id$
 */
public class Reloader
{
    /**
     * The underlying implementation used by this reloader.
     */
    private final ReloaderImpl impl = GWT.create(ReloaderImpl.class);

    /**
     * Creates a new reloader for the specified rich text area.
     * 
     * @param rta the rich text that needs to be reloaded
     */
    public Reloader(RichTextArea rta)
    {
        impl.setTextArea(rta);
    }

    /**
     * Reloads the underlying rich text area.
     * 
     * @param params optional reload parameters
     * @param handler the object notified when the rich text area is reloaded
     */
    public void reload(Map<String, String> params, final LoadHandler handler)
    {
        impl.reload(params, handler);
    }
}
