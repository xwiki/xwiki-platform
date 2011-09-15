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
package org.xwiki.sheet;

import java.util.List;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

/**
 * The interface used to manage document and class sheets.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@ComponentRole
public interface SheetManager
{
    /**
     * Returns the list of sheets associated with a XWiki document.
     * <p>
     * Note: We can't use a document reference because the document might have unsaved objects. The create forms usually
     * render an unsaved document that has some objects attached.
     * 
     * @param document a XWiki document
     * @param action the action for which to retrieve the list of sheets ('view', 'edit' etc.)
     * @return the list of sheets associated with the given document and the specified action
     */
    List<DocumentReference> getSheets(DocumentModelBridge document, String action);

    /**
     * Returns the sheet associated with a XWiki class.
     * 
     * @param classReference a reference to a XWiki class
     * @param action the action for which to retrieve the sheet
     * @return a sheet that can be used to display objects of the specified class for the specified action, {@code null}
     *         is no corresponding sheet is found
     */
    DocumentReference getClassSheet(DocumentReference classReference, String action);
}
