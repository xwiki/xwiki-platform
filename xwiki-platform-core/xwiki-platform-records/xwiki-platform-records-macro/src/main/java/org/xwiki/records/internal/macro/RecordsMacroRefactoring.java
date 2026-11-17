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

import java.util.HashSet;
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
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.SpaceResourceReference;
import org.xwiki.rendering.macro.MacroRefactoring;

/**
 * Keeps the {@code class} and {@code newEntriesLocation} parameters of the Records macro pointing to their object type
 * and space when pages are moved or renamed, and declares them as references of the page holding the macro.
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

    private static final String LOCATION_PARAMETER = "newEntriesLocation";

    /**
     * The parameters holding a reference, with the type of what they point to.
     */
    private static final Map<String, EntityType> REFERENCE_PARAMETERS =
        Map.of(CLASS_PARAMETER, EntityType.DOCUMENT, LOCATION_PARAMETER, EntityType.SPACE);

    @Inject
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    /**
     * Gives the name of the home page of a space.
     */
    @Inject
    private EntityReferenceProvider entityReferenceProvider;

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
        // The page the values are resolved against once the move is done.
        DocumentReference base = relative ? targetReference : currentDocumentReference;
        MacroBlock newMacroBlock = null;
        for (Map.Entry<String, EntityType> parameter : REFERENCE_PARAMETERS.entrySet()) {
            Optional<String> name = findParameter(macroBlock, parameter.getKey());
            if (name.isEmpty()) {
                continue;
            }
            String value = macroBlock.getParameter(name.get());
            EntityType type = parameter.getValue();
            DocumentReference reference = resolve(value, type, macroBlock, base);
            EntityReference expectedReference;
            if (relative) {
                // The page holding the macro moved from the source to the target. A value that does not name every
                // part of the reference is completed from that page, so it can point elsewhere once the page has
                // moved. What it points to may also be moved by the same job.
                EntityReference previousReference = resolve(value, type, macroBlock, sourceReference);
                expectedReference = updatedEntities.getOrDefault(previousReference, previousReference);
            } else {
                // The source page moved, and the current page holds the macro: only what the value points to moving
                // matters.
                expectedReference = reference.equals(sourceReference) ? targetReference : reference;
            }
            Optional<String> newValue = reference.equals(expectedReference) ? Optional.empty()
                : serialize(new DocumentReference(expectedReference), type, base);
            if (newValue.isPresent()) {
                newMacroBlock = newMacroBlock == null ? (MacroBlock) macroBlock.clone() : newMacroBlock;
                newMacroBlock.setParameter(name.get(), newValue.get());
            }
        }
        return Optional.ofNullable(newMacroBlock);
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
        Set<ResourceReference> references = new HashSet<>();
        REFERENCE_PARAMETERS.forEach((parameterName, type) -> findParameter(macroBlock, parameterName)
            .map(macroBlock::getParameter).ifPresent(value -> references.add(type == EntityType.SPACE
                ? new SpaceResourceReference(value) : new DocumentResourceReference(value))));
        return references;
    }

    /**
     * @param macroBlock the Records macro call
     * @param parameterName the name of a parameter
     * @return the name the call gives to the parameter, which macro parameters accept in any case, if the call sets
     *     it to a non blank value
     */
    private Optional<String> findParameter(MacroBlock macroBlock, String parameterName)
    {
        return macroBlock.getParameters().entrySet().stream()
            .filter(parameter -> parameterName.equalsIgnoreCase(parameter.getKey())
                && StringUtils.isNotBlank(parameter.getValue()))
            .map(Map.Entry::getKey).findFirst();
    }

    /**
     * @return the page the value points to, which for a space is its home page, to compare it with the moved pages
     */
    private DocumentReference resolve(String value, EntityType type, MacroBlock macroBlock, DocumentReference base)
    {
        EntityReference reference = this.macroEntityReferenceResolver.resolve(value, type, macroBlock, base);
        if (type == EntityType.SPACE) {
            return new DocumentReference(getHomeName(), new SpaceReference(reference));
        }
        return new DocumentReference(reference);
    }

    /**
     * @return the value of the reference, or nothing for a space whose home page became a regular page
     */
    private Optional<String> serialize(DocumentReference reference, EntityType type, DocumentReference base)
    {
        EntityReference serialized = reference;
        if (type == EntityType.SPACE) {
            if (!reference.getName().equals(getHomeName())) {
                return Optional.empty();
            }
            serialized = reference.getLastSpaceReference();
        }
        return Optional.of(this.compactWikiEntityReferenceSerializer.serialize(serialized, base.getWikiReference()));
    }

    private String getHomeName()
    {
        return this.entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
    }
}
