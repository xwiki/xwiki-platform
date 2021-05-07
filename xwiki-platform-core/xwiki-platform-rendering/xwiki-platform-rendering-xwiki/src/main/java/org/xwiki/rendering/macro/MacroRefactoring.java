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
package org.xwiki.rendering.macro;

import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

/**
 * Component dedicated to perform refactoring of existing macros.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@Unstable
@Role
public interface MacroRefactoring
{
    /**
     * Replace the given source reference by the given entity reference in the macro block. The method returns an
     * optional containing a modified macro block if it needs to be updated, else it returns an empty optional.
     * Depending on the macro implementation, this method might lead to parsing the macro
     * content for finding the reference, or might just modify the macro parameters.
     *
     * @param macroBlock the macro block in which to replace the reference.
     * @param sourceReference the reference to replace.
     * @param targetReference the reference to use as replacement.
     * @param currentDocumentReference the reference of the document in which the block is located
     * @param syntax the syntax of the document where the macro block is.
     * @param relative if {@code true} indicate that the reference should be resolved relatively to the current document
     * @return an optional containing the new macro block with proper information if it needs to be updated, else
     *         an empty optional.
     * @throws MacroRefactoringException in case of problem to parse or render the macro content.
     */
    Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference sourceReference,
        DocumentReference targetReference, DocumentReference currentDocumentReference, Syntax syntax, boolean relative)
        throws MacroRefactoringException;
}
