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
 * The interface used to bind sheets to documents and classes.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Role
public interface SheetBinder
{
    /**
     * Retrieves the list of sheets explicitly bound to a XWiki document. This method doesn't perform any checks on the
     * returned list of sheets. The returned list can contain duplicates, sheets that doesn't exist or sheets that only
     * some users have the right to view.
     * 
     * @param document a XWiki document
     * @return the list of sheets explicitly bound to the given document
     */
    List<DocumentReference> getSheets(DocumentModelBridge document);

    /**
     * Retrieves the list of XWiki documents explicitly bound to a given sheet. This method doesn't perform any checks
     * on the specified sheet: it may not even exist or it may not be viewable by the current user. This method simply
     * returns the list of documents that explicitly declare they're using the specified sheet.
     * 
     * @param sheetReference a reference to a sheet
     * @return the list of XWiki documents explicitly bound to the specified sheet.
     */
    List<DocumentReference> getDocuments(DocumentReference sheetReference);

    /**
     * Binds a sheet to a XWiki document. This method doesn't perform any checks on the specified sheet. The sheet may
     * not even exist or may not be viewable by all users. The changes are not persisted until the document is saved.
     * 
     * @param document a XWiki document
     * @param sheetReference a reference to a sheet
     * @return {@code true} if the sheet was successfully bound to the given document, {@code false} otherwise (e.g. if
     *         the specified sheet was already bound to the given document)
     */
    boolean bind(DocumentModelBridge document, DocumentReference sheetReference);

    /**
     * Removes the binding between a sheet and a XWiki document. The changes are not persisted until the document is
     * saved.
     * 
     * @param document a XWiki document
     * @param sheetReference a reference to a sheet
     * @return {@code true} if the sheet was successfully unbound from the given document, {@code false} otherwise (e.g.
     *         if the specified sheet wasn't bound to the given document)
     */
    boolean unbind(DocumentModelBridge document, DocumentReference sheetReference);
}
