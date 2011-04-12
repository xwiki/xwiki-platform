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
package org.xwiki.gwt.wysiwyg.client.plugin.importer;

import com.google.gwt.dom.client.Document;

/**
 * Filters the DOM tree generated from the text pasted into a right text area.
 * <p>
 * Note: The purpose of this filter is not to clean the pasted text but to adjust the DOM tree so that its HTML
 * serialization can be cleaned on the server side.
 * 
 * @version $Id$
 */
public class PasteFilter
{
    /**
     * Filters the given DOM document.
     * 
     * @param document the DOM document to be filtered
     */
    public void filter(Document document)
    {
        // No filtering is done by default. Browser specific implementations may overwrite this method.
    }
}
