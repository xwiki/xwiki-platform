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
package org.xwiki.gwt.dom.client;

import org.xwiki.gwt.dom.client.internal.DefaultSelectionManager;

import com.google.gwt.core.client.GWT;

/**
 * Interface for retrieving the current selection.
 * 
 * @version $Id$
 */
public interface SelectionManager
{
    /**
     * We create the singleton instance using deferred binding in order to use different implementations for different
     * browsers.
     */
    SelectionManager INSTANCE = GWT.create(DefaultSelectionManager.class);

    /**
     * @param doc The document for which to retrieve the selection.
     * @return The selection associated with the specified document.
     */
    Selection getSelection(Document doc);
}
