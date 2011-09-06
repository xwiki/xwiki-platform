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

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.syntax.Syntax;

/**
 * The interface used to render sheets.
 * 
 * @version $Id$
 */
@ComponentRole
public interface SheetRenderer
{
    /**
     * Applies a sheet to a document by rendering the sheet in the context of the document. This method ensures the
     * programming rights of the sheet are preserved: if the sheet doesn't have programming rights then it is evaluated
     * without them, otherwise, if the sheet has programming rights, it is evaluated with programming rights even if the
     * target document doesn't have them.
     * 
     * @param document the target document, i.e. the document the sheet is applied to
     * @param sheet the sheet to render
     * @param outputSyntax the output syntax
     * @return the result of rendering the specified sheet in the context of the target document
     */
    String render(DocumentModelBridge document, DocumentModelBridge sheet, Syntax outputSyntax);
}
