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

import java.util.ArrayList;
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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.refactoring.util.ReferenceRenamer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.block.match.OrBlockMatcher;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.MacroRefactoring;
import org.xwiki.rendering.macro.MacroRefactoringException;
import org.xwiki.rendering.syntax.Syntax;

import static com.xpn.xwiki.XWiki.DEFAULT_SPACE_HOMEPAGE;

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
    private EntityReferenceResolver<ResourceReference> entityReferenceResolver;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    @Inject
    private PageReferenceResolver<EntityReference> defaultReferencePageReferenceResolver;

    @Inject
    private Provider<MacroRefactoring> macroRefactoringProvider;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Logger logger;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public boolean renameReferences(XDOM xdom, DocumentReference currentDocumentReference, DocumentReference oldTarget,
        DocumentReference newTarget, boolean relative)
    {
        List<Block> blocks = xdom.getBlocks(new OrBlockMatcher(DEFAULT_BLOCK_MATCHERS), Block.Axes.DESCENDANT);

        boolean modified = false;

        for (Block block : blocks) {
            if (block instanceof MacroBlock) {
                MacroBlock macroBlock = (MacroBlock) block;
                Optional<MacroBlock> optionalMacroBlock = this.handleMacroBlock(macroBlock, currentDocumentReference,
                    oldTarget, newTarget, relative);
                if (optionalMacroBlock.isPresent()) {
                    xdom.replaceChild(optionalMacroBlock.get(), macroBlock);
                    modified = true;
                }
            } else {
                ResourceReference reference;
                if (block instanceof LinkBlock) {
                    LinkBlock linkBlock = (LinkBlock) block;
                    reference = linkBlock.getReference();

                } else if (block instanceof ImageBlock) {
                    ImageBlock imageBlock = (ImageBlock) block;
                    reference = imageBlock.getReference();
                } else {
                    throw new IllegalArgumentException(String.format(
                        "Only LinkBlock and ImageBlock can be processed and given class was: [%s]",
                        block.getClass().getName()));
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
        Syntax syntax;
        try {
            syntax = this.documentAccessBridge.getDocumentInstance(currentDocumentReference).getSyntax();
        } catch (Exception e) {
            logger.warn("Error while trying to get syntax of the current document reference [{}]: "
                + "[{}]", currentDocumentReference, ExceptionUtils.getRootCauseMessage(e));

            // FIXME: probably not a good way to fallback
            syntax = Syntax.XWIKI_2_1;
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
            return this.updateResourceReference(reference, oldReference, newReference, currentDocumentReference,
                relative);
        }
    }

    private boolean updateResourceReference(ResourceReference resourceReference,
        DocumentReference oldReference, DocumentReference newReference, DocumentReference currentDocumentReference,
        boolean relative)
    {
        boolean result = false;

        EntityReference linkEntityReference;

        if (relative) {
            linkEntityReference = this.entityReferenceResolver.resolve(resourceReference, null, newReference);
        } else {
            linkEntityReference =
                this.entityReferenceResolver.resolve(resourceReference, null, currentDocumentReference);
        }

        if (relative && newReference.equals(linkEntityReference.extractReference(EntityType.DOCUMENT))) {
            // If the link is relative to the containing document we don't modify it
            return false;
        }

        // current link, use the old document's reference to fill in blanks.
        EntityReference oldLinkReference =
            this.entityReferenceResolver.resolve(resourceReference, null, oldReference);

        DocumentReference linkTargetDocumentReference =
            this.defaultReferenceDocumentReferenceResolver.resolve(linkEntityReference);
        EntityReference newTargetReference = newReference;
        ResourceType newResourceType = resourceReference.getType();

        // If the new and old link references don`t match, then we must update the relative link.
        if (relative && !linkEntityReference.equals(oldLinkReference)) {
            // Serialize the old (original) link relative to the new document's location, in compact form.
            String serializedLinkReference =
                this.compactEntityReferenceSerializer.serialize(oldLinkReference, newReference);
            resourceReference.setReference(serializedLinkReference);
            result = true;
        }
        // If the link targets the old (renamed) document reference, we must update it.
        else if (linkTargetDocumentReference.equals(oldReference)) {
            // If the link was resolved to a space...
            if (EntityType.SPACE.equals(linkEntityReference.getType())) {
                if (DEFAULT_SPACE_HOMEPAGE.equals(newReference.getName())) {
                    // If the new document reference is also a space (non-terminal doc), be careful to keep it
                    // serialized as a space still (i.e. without ".WebHome") and not serialize it as a doc by mistake
                    // (i.e. with ".WebHome").
                    newTargetReference = oldReference.getLastSpaceReference();
                } else {
                    // If the new target is a non-terminal document, we can not use a "space:" resource type to access
                    // it anymore. To fix it, we need to change the resource type of the link reference "doc:".
                    newResourceType = ResourceType.DOCUMENT;
                }
            }

            // If the link was resolved to a page...
            if (EntityType.PAGE.equals(linkEntityReference.getType())) {
                // Be careful to keep it serialized as a page still and not serialize it as a doc by mistake
                newTargetReference = this.defaultReferencePageReferenceResolver.resolve(newReference);
            }

            // If the link was resolved as an attachment
            if (EntityType.ATTACHMENT.equals(linkEntityReference.getType())) {
                // Make sure to serialize an attachment reference and not just the document
                newTargetReference = new AttachmentReference(linkEntityReference.getName(), newReference);
            }

            String newReferenceString =
                this.compactEntityReferenceSerializer.serialize(newTargetReference, currentDocumentReference);

            resourceReference.setReference(newReferenceString);
            resourceReference.setType(newResourceType);
            result = true;
        }
        return result;
    }
}
