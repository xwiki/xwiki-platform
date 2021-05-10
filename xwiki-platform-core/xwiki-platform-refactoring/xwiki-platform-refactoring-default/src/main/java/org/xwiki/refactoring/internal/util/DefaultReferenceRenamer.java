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
package org.xwiki.refactoring.internal.util;

import java.util.Arrays;
import java.util.HashSet;
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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.util.ReferenceRenamer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.block.match.OrBlockMatcher;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.MacroRefactoring;
import org.xwiki.rendering.macro.MacroRefactoringException;
import org.xwiki.rendering.syntax.Syntax;

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
        new ClassBlockMatcher(ImageBlock.class)
    );

    private static final Set<ResourceType> SUPPORTED_RESOURCE_TYPES = new HashSet<>(Arrays.asList(
        ResourceType.DOCUMENT,
        ResourceType.SPACE,
        ResourceType.PAGE,
        ResourceType.ATTACHMENT
    ));

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

    @Override
    public boolean renameReferences(Block block, DocumentReference currentDocumentReference,
        DocumentReference oldTarget, DocumentReference newTarget, boolean relative)
    {
        List<Block> blocks = block.getBlocks(new OrBlockMatcher(DEFAULT_BLOCK_MATCHERS), Block.Axes.DESCENDANT);

        boolean modified = false;

        for (Block matchingBlock : blocks) {
            if (matchingBlock instanceof MacroBlock) {
                MacroBlock macroBlock = (MacroBlock) matchingBlock;
                Optional<MacroBlock> optionalMacroBlock = this.handleMacroBlock(macroBlock, currentDocumentReference,
                    oldTarget, newTarget, relative);
                if (optionalMacroBlock.isPresent()) {
                    block.replaceChild(optionalMacroBlock.get(), macroBlock);
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
                modified |= this.renameResourceReference(reference, oldTarget, newTarget,
                    currentDocumentReference, relative);
            }
        }

        return modified;
    }

    private Optional<MacroBlock> handleMacroBlock(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        DocumentReference oldTarget, DocumentReference newTarget, boolean relative)
    {
        MacroRefactoring macroRefactoring = this.macroRefactoringProvider.get();
        if (this.componentManager.hasComponent(MacroRefactoring.class, macroBlock.getId())) {
            try {
                macroRefactoring = this.componentManager.getInstance(MacroRefactoring.class,
                    macroBlock.getId());
            } catch (ComponentLookupException e) {
                logger.warn("Error while getting the macro refactoring component for macro id [{}]: [{}].",
                    macroBlock.getId(), ExceptionUtils.getRootCauseMessage(e));
            }
        }

        // the document syntax will be used as a fallback syntax if the XDOM syntax cannot be found
        Syntax syntax;
        try {
            syntax = this.documentAccessBridge.getDocumentInstance(currentDocumentReference).getSyntax();
        } catch (Exception e) {
            logger.warn("Error while trying to get syntax of the current document reference [{}]: "
                + "[{}]", currentDocumentReference, ExceptionUtils.getRootCauseMessage(e));

            // last fallback in the unlikely case of there's no document syntax
            syntax = this.extendedRenderingConfiguration.getDefaultContentSyntax();
        }
        try {
            return macroRefactoring
                .replaceReference(macroBlock, oldTarget, newTarget, currentDocumentReference, syntax,
                    relative);
        } catch (MacroRefactoringException e) {
            logger.warn("Error while trying to refactor reference from [{}] to [{}] in macro [{}] of "
                + "document [{}]", oldTarget, newTarget, macroBlock.getId(), currentDocumentReference);
        }
        return Optional.empty();
    }

    private boolean renameResourceReference(ResourceReference reference, DocumentReference oldReference,
        DocumentReference newReference, DocumentReference currentDocumentReference, boolean relative)
    {
        if (reference == null) {
            throw new IllegalArgumentException("The reference of  the block cannot be null.");
        } else if (!SUPPORTED_RESOURCE_TYPES.contains(reference.getType())) {
            // We are currently only interested in Document or Space references.
            return false;
        } else {
            return this.resourceReferenceRenamer.updateResourceReference(reference, oldReference, newReference,
                currentDocumentReference, relative);
        }
    }
}
