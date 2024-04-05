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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Component dedicated to perform refactoring of existing macros.
 *
 * @version $Id$
 * @since 13.4RC1
 */
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
     * @param currentDocumentReference the reference of the document in which the block is located
     * @param sourceReference the reference to replace.
     * @param targetReference the reference to use as replacement.
     * @param relative if {@code true} indicate that the reference should be resolved relatively to the current document
     * @return an optional containing the new macro block with proper information if it needs to be updated, else
     *         an empty optional.
     * @throws MacroRefactoringException in case of problem to parse or render the macro content.
     */
    Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        DocumentReference sourceReference, DocumentReference targetReference, boolean relative)
        throws MacroRefactoringException;

    /**
     * Replace the given source reference by the given entity reference in the macro block. The method returns an
     * optional containing a modified macro block if it needs to be updated, else it returns an empty optional.
     * Depending on the macro implementation, this method might lead to parsing the macro
     * content for finding the reference, or might just modify the macro parameters.
     *
     * @param macroBlock the macro block in which to replace the reference.
     * @param currentDocumentReference the reference of the document in which the block is located
     * @param sourceReference the reference to replace.
     * @param targetReference the reference to use as replacement
     * @param relative if {@code true} indicate that the reference should be resolved relatively to the current document
     * @return an optional containing the new macro block with proper information if it needs to be updated, else
     *         an empty optional.
     * @throws MacroRefactoringException in case of problem to parse or render the macro content.
     * @since 14.2RC1
     */
    Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        AttachmentReference sourceReference, AttachmentReference targetReference, boolean relative)
        throws MacroRefactoringException;

    /**
     * Extract references used in the macro so that they can be used for example for creating backlinks.
     *
     * @param macroBlock the macro block in which to look for references.
     * @return a set of references contained in the macro, in its content or parameters.
     * @throws MacroRefactoringException in case of problem to parse the macro content.
     * @since 13.7RC1
     */
    default Set<ResourceReference> extractReferences(MacroBlock macroBlock) throws MacroRefactoringException
    {
        return Collections.emptySet();
    }
}
