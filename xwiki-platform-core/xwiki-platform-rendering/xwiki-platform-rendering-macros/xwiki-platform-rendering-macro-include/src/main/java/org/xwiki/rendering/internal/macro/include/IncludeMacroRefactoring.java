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
package org.xwiki.rendering.internal.macro.include;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.MacroRefactoring;
import org.xwiki.rendering.macro.MacroRefactoringException;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Implementation of reference refactoring operation for include macro.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@Component
@Singleton
@Named("include")
public class IncludeMacroRefactoring implements MacroRefactoring
{
    protected static final String DOCUMENT_MACRO_PARAMETER = "document";
    protected static final String REFERENCE_MACRO_PARAMETER = "reference";

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> stringEntityReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference sourceReference,
        DocumentReference targetReference, DocumentReference currentDocumentReference, Syntax syntax, boolean relative)
        throws MacroRefactoringException
    {
        String referenceParameter = macroBlock.getParameter(REFERENCE_MACRO_PARAMETER);
        String documentParameter = macroBlock.getParameter(DOCUMENT_MACRO_PARAMETER);

        String parameterName = null;
        if (!StringUtils.isEmpty(referenceParameter)) {
            DocumentReference reference = this.documentReferenceResolver.resolve(referenceParameter);
            if (sourceReference.equals(reference)) {
                parameterName = REFERENCE_MACRO_PARAMETER;
            }
        } else if (!StringUtils.isEmpty(documentParameter)) {
            DocumentReference reference = this.documentReferenceResolver.resolve(documentParameter);
            if (sourceReference.equals(reference)) {
                parameterName = DOCUMENT_MACRO_PARAMETER;
            }
        }

        if (parameterName != null) {
            MacroBlock result = (MacroBlock) macroBlock.clone();
            String newReference =
                this.stringEntityReferenceSerializer.serialize(targetReference, currentDocumentReference);
            result.setParameter(parameterName, newReference);
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }
}
