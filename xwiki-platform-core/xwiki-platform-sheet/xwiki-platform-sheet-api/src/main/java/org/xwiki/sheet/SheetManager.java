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

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

/**
 * The interface used to manage document and class sheets.
 * 
 * @version $Id$
 */
@ComponentRole
public interface SheetManager
{
    /**
     * The possible values for the {@code display} sheet property.
     */
    static enum SheetDisplay
    {
        /**
         * The sheet outputs most of the generated HTML page.
         */
        PAGE,

        /**
         * The sheet output is aggregated inside the content area of the generated HTML page.
         */
        INLINE
    }

    /**
     * @param documentReference a reference to the document that is being rendered
     * @param action the action taken on the rendered document ('view', 'edit' etc.)
     * @param display where the sheet output will be placed
     * @return the list of sheets that match the specified document, action and display
     */
    List<DocumentReference> getSheets(DocumentReference documentReference, String action, SheetDisplay display);

    /**
     * Use this method if you want to apply a custom sheet to a document. The sheet can be automatically determined
     * based on the type of objects attached to the document (see
     * {@link #bindClassSheet(DocumentReference, DocumentReference)}) but this method allows you to overwrite this sheet
     * with a custom one.
     * 
     * @param documentReference a reference to the document where to include the sheet
     * @param sheetReference specifies the sheet to be included
     */
    void bindDocumentSheet(DocumentReference documentReference, DocumentReference sheetReference);

    /**
     * Use this method if you want to prevent a custom sheet from applying to a document. This is the opposite of
     * {@link #bindDocumentSheet(DocumentReference, DocumentReference)}
     * 
     * @param documentReference a reference to the document from which to exclude the sheet
     * @param sheetReference specifies the sheet to be excluded
     */
    void unbindDocumentSheet(DocumentReference documentReference, DocumentReference sheetReference);

    /**
     * @param classReference a reference to a XWiki class
     * @param action the action for which to retrieve the sheet
     * @return a sheet that can be used to render objects of the specified class for the specified action, {@code null}
     *         is no proper sheet is found
     */
    DocumentReference getClassSheet(DocumentReference classReference, String action);

    /**
     * Binds a XWiki class to a sheet so that whenever a document with an object of the specified type is rendered the
     * specified sheet is used.
     * 
     * @param classReference a XWiki class
     * @param sheetReference a class sheet that renders objects of the specified class
     */
    void bindClassSheet(DocumentReference classReference, DocumentReference sheetReference);

    /**
     * Removes the binding between the specified XWiki class and the specified sheet. This is the opposite of
     * {@link #bindClassSheet(DocumentReference, DocumentReference)}
     * 
     * @param classReference a XWiki class
     * @param sheetReference the class sheet that shouldn't be used anymore for rendering the objects of the specified
     *            class
     */
    void unbindClassSheet(DocumentReference classReference, DocumentReference sheetReference);
}
