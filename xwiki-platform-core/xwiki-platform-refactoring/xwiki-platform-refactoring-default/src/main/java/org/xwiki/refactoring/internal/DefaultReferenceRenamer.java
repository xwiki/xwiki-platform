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
package org.xwiki.refactoring.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.ReferenceRenamer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.block.match.OrBlockMatcher;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.MacroRefactoring;
import org.xwiki.rendering.macro.MacroRefactoringException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;

/**
 * Helper to rename references in various context (link, image, etc. ).
 * 
 * @version $Id$
 * @since 13.4RC1
 */
@Component
@Singleton
public class DefaultReferenceRenamer implements ReferenceRenamer
{
    private static final List<BlockMatcher> DEFAULT_BLOCK_MATCHERS = Arrays.asList(
        new ClassBlockMatcher(LinkBlock.class),
        new ClassBlockMatcher(ImageBlock.class),
        new ClassBlockMatcher(MacroBlock.class)
    );

    private static final Set<ResourceType> SUPPORTED_RESOURCE_TYPES_FOR_DOCUMENTS = Set.of(ResourceType.DOCUMENT,
        ResourceType.SPACE, ResourceType.PAGE, ResourceType.ATTACHMENT, ResourceType.PAGE_ATTACHMENT);

    private static final Set<ResourceType> SUPPORTED_RESOURCE_TYPES_FOR_ATTACHMENTS =
        Set.of(ResourceType.ATTACHMENT, ResourceType.PAGE_ATTACHMENT);

    @Inject
    private Provider<MacroRefactoring> macroRefactoringProvider;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Logger logger;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private ResourceReferenceRenamer resourceReferenceRenamer;

    @Inject
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    @Inject
    private RenderingContext renderingContext;

    @FunctionalInterface
    private interface MacroRefactoringLambda
    {
        Optional<MacroBlock> call(MacroRefactoring macroRefactoring, MacroBlock macroBlock) throws
            MacroRefactoringException;
    }

    @FunctionalInterface
    private interface RenameResourceLambda
    {
        boolean call(ResourceReference reference);
    }

    @Override
    public boolean renameReferences(Block block, DocumentReference currentDocumentReference,
        DocumentReference oldTarget, DocumentReference newTarget, boolean relative)
    {
        return innerRenameReferences(block, currentDocumentReference, oldTarget, newTarget,
            SUPPORTED_RESOURCE_TYPES_FOR_DOCUMENTS,
            (MacroRefactoring macroRefactoring, MacroBlock macroBlock) -> macroRefactoring.replaceReference(macroBlock,
                currentDocumentReference, oldTarget, newTarget, relative),
            reference -> this.resourceReferenceRenamer.updateResourceReference(reference, oldTarget, newTarget,
                currentDocumentReference, relative));
    }

    @Override
    public boolean renameReferences(Block block, DocumentReference currentDocumentReference,
        AttachmentReference oldTarget, AttachmentReference newTarget, boolean relative)
    {
        return innerRenameReferences(block, currentDocumentReference, oldTarget, newTarget,
            SUPPORTED_RESOURCE_TYPES_FOR_ATTACHMENTS,
            (MacroRefactoring macroRefactoring, MacroBlock macroBlock) -> macroRefactoring.replaceReference(macroBlock,
                currentDocumentReference, oldTarget, newTarget, relative),
            reference -> this.resourceReferenceRenamer.updateResourceReference(reference, oldTarget, newTarget,
                currentDocumentReference, relative));
    }

    private boolean innerRenameReferences(Block block, DocumentReference currentDocumentReference,
        EntityReference oldTarget,
        EntityReference newTarget, Set<ResourceType> allowedResourceTypes,
        MacroRefactoringLambda macroRefactoringLambda, RenameResourceLambda renameResourceLambda)
    {
        List<Block> blocks = block.getBlocks(new OrBlockMatcher(DEFAULT_BLOCK_MATCHERS), Block.Axes.DESCENDANT);

        boolean modified = false;

        for (Block matchingBlock : blocks) {
            if (matchingBlock instanceof MacroBlock) {
                MacroBlock macroBlock = (MacroBlock) matchingBlock;
                Optional<MacroBlock> optionalMacroBlock = handleMacroBlock(macroBlock, currentDocumentReference,
                    oldTarget, newTarget, macroRefactoringLambda);
                if (optionalMacroBlock.isPresent()) {
                    macroBlock.getParent().replaceChild(optionalMacroBlock.get(), macroBlock);
                    modified = true;
                }
            } else {
                ResourceReference reference;
                if (matchingBlock instanceof LinkBlock) {
                    LinkBlock linkBlock = (LinkBlock) matchingBlock;
                    reference = linkBlock.getReference();
                } else if (matchingBlock instanceof ImageBlock) {
                    ImageBlock imageBlock = (ImageBlock) matchingBlock;
                    reference = imageBlock.getReference();
                } else {
                    throw new IllegalArgumentException(String.format(
                        "Only LinkBlock and ImageBlock can be processed and given class was: [%s]",
                        matchingBlock.getClass().getName()));
                }
                modified |= this.renameResourceReference(reference, allowedResourceTypes, renameResourceLambda);
            }
        }

        return modified;
    }

    private Optional<MacroBlock> handleMacroBlock(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        EntityReference oldTarget, EntityReference newTarget, MacroRefactoringLambda macroRefactoringLambda)
    {
        MacroRefactoring macroRefactoring = this.macroRefactoringProvider.get();
        if (this.componentManager.hasComponent(MacroRefactoring.class, macroBlock.getId())) {
            try {
                macroRefactoring = this.componentManager.getInstance(MacroRefactoring.class,
                    macroBlock.getId());
            } catch (ComponentLookupException e) {
                this.logger.warn("Error while getting the macro refactoring component for macro id [{}]: [{}].",
                    macroBlock.getId(), ExceptionUtils.getRootCauseMessage(e));
            }
        }

        // the document syntax will be used as a fallback syntax if the XDOM syntax cannot be found
        Syntax syntax;
        try {
            syntax = this.documentAccessBridge.getDocumentInstance(currentDocumentReference).getSyntax();
        } catch (Exception e) {
            this.logger.warn("Error while trying to get syntax of the current document reference [{}]: "
                + "[{}]", currentDocumentReference, ExceptionUtils.getRootCauseMessage(e));

            // last fallback in the unlikely case of there's no document syntax
            syntax = this.extendedRenderingConfiguration.getDefaultContentSyntax();
        }
        if (this.renderingContext instanceof MutableRenderingContext) {
            // Set the default syntax
            ((MutableRenderingContext) this.renderingContext).push(this.renderingContext.getTransformation(),
                this.renderingContext.getXDOM(), syntax, this.renderingContext.getTransformationId(),
                this.renderingContext.isRestricted(), this.renderingContext.getTargetSyntax());
        }
        try {
            return macroRefactoringLambda.call(macroRefactoring, macroBlock);
        } catch (MacroRefactoringException e) {
            this.logger.warn("Error while trying to refactor reference from [{}] to [{}] in macro [{}] of "
                + "document [{}]", oldTarget, newTarget, macroBlock.getId(), currentDocumentReference);
        } finally {
            // don't forget to pop the rendering context.
            if (this.renderingContext instanceof MutableRenderingContext) {
                ((MutableRenderingContext) this.renderingContext).pop();
            }
        }
        return Optional.empty();
    }

    private boolean renameResourceReference(ResourceReference reference, Set<ResourceType> allowedResourceTypes,
        RenameResourceLambda renameResourceLambda)
    {
        if (reference == null) {
            throw new IllegalArgumentException("The reference of  the block cannot be null.");
        } else if (!allowedResourceTypes.contains(reference.getType())) {
            // Skip if the resource type is not allowed.
            return false;
        } else {
            return renameResourceLambda.call(reference);
        }
    }
}
