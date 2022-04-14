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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.PageResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.macro.MacroRefactoring;
import org.xwiki.rendering.macro.MacroRefactoringException;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;

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
    @Inject
    private Logger logger;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @Inject
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @Inject
    private BeanManager beanManager;

    @Inject
    private PageReferenceResolver<EntityReference> pageReferenceResolver;

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        DocumentReference sourceReference, DocumentReference targetReference, boolean relative)
        throws MacroRefactoringException
    {
        return getMacroBlock(macroBlock, currentDocumentReference, sourceReference, targetReference, relative);
    }

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        AttachmentReference sourceReference, AttachmentReference targetReference, boolean relative)
        throws MacroRefactoringException
    {
        return getMacroBlock(macroBlock, currentDocumentReference, sourceReference, targetReference, relative);
    }

    private <T extends EntityReference> Optional<MacroBlock> getMacroBlock(MacroBlock macroBlock,
        DocumentReference currentDocumentReference, T sourceReference, T targetReference, boolean relative)
        throws MacroRefactoringException
    {
        Optional<MacroBlock> result;

        IncludeMacroParameters parameters = getMacroParameters(macroBlock);
        if (!StringUtils.isEmpty(parameters.getReference())) {
            // Check if the macro block's reference parameter contains the same reference as the one being refactored.
            // If true then perform the refactoring. If not, do nothing.
            EntityReference reference =
                this.macroEntityReferenceResolver.resolve(parameters.getReference(), parameters.getType(), macroBlock);
            if (sourceReference.equals(reference, parameters.getType())) {
                MacroBlock newMacroBlock = (MacroBlock) macroBlock.clone();
                boolean serialized = serializeTargetReference(reference, newMacroBlock, targetReference,
                    currentDocumentReference, relative);
                result = serialized ? Optional.of(newMacroBlock) : Optional.empty();
            } else {
                result = Optional.empty();
            }
        } else {
            result = Optional.empty();
        }
        return result;
    }

    @Override
    public Set<ResourceReference> extractReferences(MacroBlock macroBlock) throws MacroRefactoringException
    {
        IncludeMacroParameters parameters = getMacroParameters(macroBlock);
        ResourceReference resourceReference;
        if (EntityType.PAGE.equals(parameters.getType())) {
            resourceReference = new PageResourceReference(parameters.getReference());
        } else if (EntityType.DOCUMENT.equals(parameters.getType())) {
            resourceReference = new DocumentResourceReference(parameters.getReference());
        } else if (EntityType.ATTACHMENT.equals(parameters.getType())) {
            resourceReference = new AttachmentResourceReference(parameters.getReference());
        } else {
            // Entity type not supported and it shouldn't happen. Do nothing to not impact the refactoring.
            this.logger.warn("The reference type [{}] is not currently supported. Not extracting the reference [{}] "
                + "for it.", parameters.getType(), parameters.getReference());
            resourceReference = null;
        }
        return resourceReference != null ? Collections.singleton(resourceReference) : Collections.emptySet();
    }

    private <T extends EntityReference> boolean serializeTargetReference(EntityReference reference,
        MacroBlock newMacroBlock, T targetReference, DocumentReference currentDocumentReference, boolean relative)
    {
        boolean result;

        // If the user was using the "page" parameter then continue to use it. Otherwise, use "reference".
        // Note that this means that if the user was using the old legacy "document" parameter, it'll get
        // canonized into a "reference" parameter.
        if (EntityType.PAGE.equals(reference.getType())) {
            // Note: we make sure we serialize a PageReference to keep the page syntax, by converting the
            // targetReference (DocumentReference) to a PageReference.
            PageReference pageReference = this.pageReferenceResolver.resolve(targetReference);
            newMacroBlock.setParameter("page",
                serializeTargetReference(pageReference, currentDocumentReference, relative));
            result = true;
        } else {
            newMacroBlock.setParameter("reference",
                serializeTargetReference(targetReference, currentDocumentReference, relative));
            result = true;
        }
        return result;
    }

    private String serializeTargetReference(EntityReference newTargetReference,
        DocumentReference currentDocumentReference, boolean relative)
    {
        // Note: if the wiki was specified by the user, it'll get removed if it's not needed.
        return relative
            ? this.compactEntityReferenceSerializer.serialize(newTargetReference, currentDocumentReference)
            : this.compactEntityReferenceSerializer.serialize(newTargetReference);
    }

    private IncludeMacroParameters getMacroParameters(MacroBlock macroBlock) throws MacroRefactoringException
    {
        // Populate and validate macro parameters.
        IncludeMacroParameters macroParameters = new IncludeMacroParameters();
        try {
            this.beanManager.populate(macroParameters, macroBlock.getParameters());
        } catch (Throwable e) {
            // One macro parameter was invalid, don't perform the refactoring but raise an exception to be on the safe
            // side.
            throw new MacroRefactoringException(String.format("There's one or several invalid "
                + "parameters for an [%s] macro with parameters [%s]", macroBlock.getId(), macroBlock.getParameters()),
                e);
        }
        return macroParameters;
    }
}
