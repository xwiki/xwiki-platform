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
import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * Interface used to retrieve the list of sheets available for a given document.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Role
public interface SheetManager
{
    /**
     * Returns the list of sheets available for a given XWiki document in the current execution context.
     * <p>
     * Note: We can't use a document reference because the document might have unsaved objects. The create forms usually
     * display an unsaved document that has some objects attached.
     * <p>
     * If this method fails for some reason to retrieve the sheets it shound't throw an exception but return an empty
     * list and log an appropriate warning message instead.
     * 
     * @param document a XWiki document
     * @param action the action for which to retrieve the list of sheets ('view', 'edit' etc.); you can pass
     *            {@code null} or an empty string as a substitute for any action
     * @return the list of sheets available for the given document and the specified action; these are sheets designed
     *         to be displayed when the specified action is performed on the given document (e.g. view sheets, edit
     *         sheets etc.)
     */
    List<DocumentReference> getSheets(DocumentModelBridge document, String action);

    /**
     * Indicate if the sheet system should be used (instead of old edit modes and #include based dispay). Generally
     * controlled using the URL {@code sheet} parameter;
     * 
     * @return true if the sheet system should be used
     * @since 10.9RC1
     */
    default boolean isSheetForced()
    {
        return false;
    }
}
