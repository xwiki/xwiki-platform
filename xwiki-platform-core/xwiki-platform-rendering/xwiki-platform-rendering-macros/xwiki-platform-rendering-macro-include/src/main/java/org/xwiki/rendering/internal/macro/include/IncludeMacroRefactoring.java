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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
import org.xwiki.text.StringUtils;

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
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @Inject
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @Inject
    private EntityReferenceResolver<EntityReference> defaultEntityReferenceResolver;

    @Inject
    private BeanManager beanManager;

    @Inject
    private PageReferenceResolver<EntityReference> pageReferenceResolver;

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        DocumentReference sourceReference, DocumentReference targetReference, boolean relative,
        Map<EntityReference, EntityReference> updatedEntities)
        throws MacroRefactoringException
    {
        return getMacroBlock(macroBlock, currentDocumentReference, sourceReference, targetReference, relative,
            updatedEntities);
    }

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        AttachmentReference sourceReference, AttachmentReference targetReference, boolean relative,
        Map<EntityReference, EntityReference> updatedEntities)
        throws MacroRefactoringException
    {
        return getMacroBlock(macroBlock, currentDocumentReference, sourceReference, targetReference, relative,
            updatedEntities);
    }

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        DocumentReference sourceReference, DocumentReference targetReference, boolean relative)
        throws MacroRefactoringException
    {
        return getMacroBlock(macroBlock, currentDocumentReference, sourceReference, targetReference, relative,
            Map.of(sourceReference, targetReference));
    }

    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        AttachmentReference sourceReference, AttachmentReference targetReference, boolean relative)
        throws MacroRefactoringException
    {
        return getMacroBlock(macroBlock, currentDocumentReference, sourceReference, targetReference, relative,
            Map.of(sourceReference.getDocumentReference(), targetReference.getDocumentReference()));
    }

    // FIXME: double check we don't need to use updated documents parameters here.
    private <T extends EntityReference> Optional<MacroBlock> getMacroBlock(MacroBlock macroBlock,
        DocumentReference currentDocumentReference, T sourceReference, T targetReference, boolean relative,
        Map<EntityReference, EntityReference> updatedEntities)
        throws MacroRefactoringException
    {
        Optional<MacroBlock> result;

        IncludeMacroParameters parameters = getMacroParameters(macroBlock);
        // Note: an empty string means a reference to the current page and thus a recursive include. Renaming the page
        // doesn't require changing the value (since it's still an empty string), thus, we skip it!
        if (!StringUtils.isEmpty(parameters.getReference())) {
            // There are 3 use cases here to handle:
            // - UC1: the document/attachment pointed to by the include parameter has been moved. In this case the
            //        passed sourceReference should be equal to that parameter in the MacroBlock.
            // - UC2: the document containing the include macro has been moved. In this case the passed
            //         sourceReference/targetReference point to the original/new document reference.

            MacroBlock newMacroBlock = (MacroBlock) macroBlock.clone();

            // Check if the macro block's reference parameter contains the same reference as the one being refactored.
            // Note: Make sure to pass the sourceReference in the resolver since the context document may not be set
            // in the refactoring job.
            EntityReference reference = this.macroEntityReferenceResolver.resolve(parameters.getReference(),
                parameters.getType(), macroBlock, sourceReference);

            // TODO: Don't honor the relative parameter for now since its usage seems to be not correct or hazy at
            // best. Instead refactoring using a relative reference if the user was using a relative reference and
            // an absolute reference if the user was using an absolute reference.
            boolean resolvedRelative = !isReferenceAbsolute(parameters.getReference(), reference);

            if (areReferencesEqual(reference, sourceReference)) {
                // We are in UC1
                boolean serialized = serializeTargetReference(reference, newMacroBlock, targetReference,
                    currentDocumentReference, resolvedRelative);
                result = serialized ? Optional.of(newMacroBlock) : Optional.empty();
            } else {
                // We assume we are in UC2 since there should only be two uses cases (UC1 and UC2).
                // Resolve the macro parameter relative to the new targetReference and check if it's different from
                // the original resolved macro parameter.
                // Note that the use case when sourceReference and targetReference are Attachment References but the
                // sourceReference is not pointing to the macro reference/page parameter is not a valid use case and
                // thus we don't handle it.
                reference = this.macroEntityReferenceResolver.resolve(parameters.getReference(), parameters.getType(),
                    macroBlock, sourceReference);
                EntityReference targetResolvedReference = this.macroEntityReferenceResolver.resolve(
                    parameters.getReference(), parameters.getType(), macroBlock, targetReference);
                if (!reference.equals(targetResolvedReference)) {
                    boolean serialized = serializeTargetReference(reference, newMacroBlock, reference,
                        targetResolvedReference, resolvedRelative);
                    result = serialized ? Optional.of(newMacroBlock) : Optional.empty();
                } else {
                    result = Optional.empty();
                }
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

    private boolean areReferencesEqual(EntityReference reference1, EntityReference reference2)
    {
        boolean result;

        if (reference1.equals(reference2)) {
            result = true;
        } else if (isPointingToDocuments(reference1, reference2) || isPointingToAttachments(reference1, reference2)) {
            // Convert the passed reference2 into a reference of the same type as reference1 so that they can be
            // compared. However, we also need to check that they are of the same type/level since otherwise the
            // resolver will extract internal reference parts, and we can find having, say, an Attachment Reference
            // equal to a Document Reference.
            EntityReference convertedReference2 =
                this.defaultEntityReferenceResolver.resolve(reference2, reference1.getType());
            result = reference1.equals(convertedReference2);
        } else {
            result = false;
        }

        return result;
    }

    private boolean isPointingToDocuments(EntityReference reference1, EntityReference reference2)
    {
        return (EntityType.DOCUMENT.equals(reference1.getType()) || EntityType.PAGE.equals(reference1.getType()))
            && (EntityType.DOCUMENT.equals(reference2.getType()) || EntityType.PAGE.equals(reference2.getType()));
    }

    private boolean isPointingToAttachments(EntityReference reference1, EntityReference reference2)
    {
        return (EntityType.ATTACHMENT.equals(reference1.getType())
                || EntityType.PAGE_ATTACHMENT.equals(reference1.getType()))
            && (EntityType.ATTACHMENT.equals(reference2.getType())
                || EntityType.PAGE_ATTACHMENT.equals(reference2.getType()));
    }

    private <T extends EntityReference> boolean serializeTargetReference(EntityReference originalReference,
        MacroBlock newMacroBlock, T targetReference, EntityReference currentReference, boolean relative)
    {
        boolean result;

        // TODO: the relative parameter has no impact ATM since the passed reference to serialize
        // (i.e. targetReference) is an absolute reference (either a DocumentReference or an AttachmentReference)

        // If the user was using the "page" parameter then continue to use it. Otherwise, use "reference".
        // Note that this means that if the user was using the old legacy "document" parameter, it'll get
        // canonized into a "reference" parameter.
        if (EntityType.PAGE.equals(originalReference.getType())) {
            // Note: we make sure we serialize a PageReference to keep the page syntax, by converting the
            // targetReference (DocumentReference) to a PageReference.
            PageReference pageReference = this.pageReferenceResolver.resolve(targetReference);
            newMacroBlock.setParameter("page",
                serializeTargetReference(pageReference, currentReference, relative));
            result = true;
        } else {
            newMacroBlock.setParameter("reference",
                serializeTargetReference(targetReference, currentReference, relative));
            result = true;
        }
        return result;
    }

    private String serializeTargetReference(EntityReference newTargetReference, EntityReference currentReference,
        boolean relative)
    {
        // Notes:
        // - If the wiki was specified by the user, it'll get removed if it's not needed.
        // - When relative is false, we still don't want to display the wiki if it's the same as the wiki of the
        //   current reference.
        return relative
            ? this.compactEntityReferenceSerializer.serialize(newTargetReference, currentReference)
            : this.compactWikiEntityReferenceSerializer.serialize(newTargetReference,
                currentReference.extractReference(EntityType.WIKI));
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

    private boolean isReferenceAbsolute(String referenceRepresentation, EntityReference absoluteEntityReference)
    {
        // Serialize the entityReference and verify if it matches the string representation
        // Remove the wiki part since we want to consider that a reference without a wiki part can still be an absolute
        // one.
        return this.compactWikiEntityReferenceSerializer.serialize(absoluteEntityReference,
            absoluteEntityReference.extractReference(EntityType.WIKI)).equals(referenceRepresentation);
    }
}
