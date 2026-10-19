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
package org.xwiki.records.internal.macro;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.macro.MacroRefactoring;

/**
 * Keeps the {@code class} parameter of the Records macro pointing to its object type when pages are moved or renamed,
 * and declares that object type as a reference of the page holding the macro.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Named(RecordsMacro.ID)
@Singleton
public class RecordsMacroRefactoring implements MacroRefactoring
{
    private static final String CLASS_PARAMETER = "class";

    @Inject
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        DocumentReference sourceReference, DocumentReference targetReference, boolean relative)
    {
        return replaceReference(macroBlock, currentDocumentReference, sourceReference, targetReference, relative,
            Map.of(sourceReference, targetReference));
    }

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        DocumentReference sourceReference, DocumentReference targetReference, boolean relative,
        Map<EntityReference, EntityReference> updatedEntities)
    {
        Optional<String> parameterName = findClassParameter(macroBlock);
        if (parameterName.isEmpty()) {
            return Optional.empty();
        }
        String value = macroBlock.getParameter(parameterName.get());

        DocumentReference base;
        EntityReference classReference;
        EntityReference expectedClassReference;
        if (relative) {
            // The page holding the macro moved from the source to the target. A value that does not name every part
            // of the object type is completed from that page, so it can point elsewhere once the page has moved. The
            // object type may also be moved by the same job.
            base = targetReference;
            classReference = resolve(value, macroBlock, targetReference);
            EntityReference previousClassReference = resolve(value, macroBlock, sourceReference);
            expectedClassReference = updatedEntities.getOrDefault(previousClassReference, previousClassReference);
        } else {
            // The source page moved, and the current page holds the macro: only the object type itself moving matters.
            base = currentDocumentReference;
            classReference = resolve(value, macroBlock, currentDocumentReference);
            expectedClassReference = classReference.equals(sourceReference) ? targetReference : classReference;
        }

        if (classReference.equals(expectedClassReference)) {
            return Optional.empty();
        }
        MacroBlock newMacroBlock = (MacroBlock) macroBlock.clone();
        newMacroBlock.setParameter(parameterName.get(), this.compactWikiEntityReferenceSerializer
            .serialize(expectedClassReference, base.getWikiReference()));
        return Optional.of(newMacroBlock);
    }

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        AttachmentReference sourceReference, AttachmentReference targetReference, boolean relative)
    {
        // The macro names no attachment.
        return Optional.empty();
    }

    @Override
    public Set<ResourceReference> extractReferences(MacroBlock macroBlock)
    {
        return findClassParameter(macroBlock)
            .<Set<ResourceReference>>map(name -> Set.of(new DocumentResourceReference(macroBlock.getParameter(name))))
            .orElse(Set.of());
    }

    /**
     * @param macroBlock the Records macro call
     * @return the name the call gives to the {@code class} parameter, which macro parameters accept in any case, if
     *     the call sets it to a non blank value
     */
    private Optional<String> findClassParameter(MacroBlock macroBlock)
    {
        return macroBlock.getParameters().entrySet().stream()
            .filter(parameter -> CLASS_PARAMETER.equalsIgnoreCase(parameter.getKey())
                && StringUtils.isNotBlank(parameter.getValue()))
            .map(Map.Entry::getKey).findFirst();
    }

    private EntityReference resolve(String value, MacroBlock macroBlock, DocumentReference base)
    {
        return new DocumentReference(
            this.macroEntityReferenceResolver.resolve(value, EntityType.DOCUMENT, macroBlock, base));
    }
}
