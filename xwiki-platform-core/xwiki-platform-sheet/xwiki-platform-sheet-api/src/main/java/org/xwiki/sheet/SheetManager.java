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
import org.xwiki.rendering.syntax.Syntax;

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
     * @param classReference a reference to a XWiki class
     * @param action the action for which to retrieve the sheet
     * @return a sheet that can be used to render objects of the specified class for the specified action, {@code null}
     *         is no proper sheet is found
     */
    DocumentReference getClassSheet(DocumentReference classReference, String action);

    /**
     * Applies a sheet to a document by rendering the sheet in the context of the document. This method ensures the
     * programming rights of the sheet are preserved: if the sheet doesn't have programming rights then it is evaluated
     * without them, otherwise, if the sheet has programming rights, it is evaluated with programming rights even if the
     * target document doesn't have them.
     * 
     * @param documentReference the target document, i.e. the document the sheet is applied to
     * @param sheetReference the sheet to apply
     * @param outputSyntax the output syntax
     * @return the result of rendering the specified sheet in the context of the target document
     */
    String apply(DocumentReference documentReference, DocumentReference sheetReference, Syntax outputSyntax);
}
