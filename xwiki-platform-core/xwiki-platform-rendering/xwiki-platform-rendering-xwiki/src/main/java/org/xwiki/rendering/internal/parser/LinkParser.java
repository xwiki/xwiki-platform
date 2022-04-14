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
package org.xwiki.rendering.internal.parser;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.MacroRefactoring;
import org.xwiki.rendering.macro.MacroRefactoringException;

/**
 * Utility component whose job is to retrieve and extract all links from an XDOM.
 *
 * @version $Id$
 * @since 13.7RC1
 */
@Component(roles = LinkParser.class)
@Singleton
public class LinkParser
{
    @Inject
    private EntityReferenceResolver<ResourceReference> entityReferenceResolver;

    @Inject
    private Provider<MacroRefactoring> defaultMacroRefactoringProvider;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /**
     * Extract all the references of an XDOM and return them as {@link ResourceReference}.
     * This method specifically checks for references in {@link LinkBlock}, {@link ImageBlock} and {@link MacroBlock}.
     * For {@link MacroBlock} it checks if there's a dedicated {@link MacroRefactoring} component associated with the
     * block id to perform the extraction, else it's using the default component implementation.
     * Note that this method logs any exception that it might occur as a warning, so it doesn't throw any of them.
     *
     * @param dom an XDOM that might contain references.
     * @return a set of {@link ResourceReference} contained in that XDOM.
     */
    public Set<ResourceReference> extractReferences(XDOM dom)
    {
        Set<ResourceReference> result = new HashSet<>();

        List<LinkBlock> linkBlocks = dom.getBlocks(new ClassBlockMatcher(LinkBlock.class), Block.Axes.DESCENDANT);
        List<ImageBlock> imageBlocks = dom.getBlocks(new ClassBlockMatcher(ImageBlock.class), Block.Axes.DESCENDANT);
        List<MacroBlock> macroBlocks = dom.getBlocks(new ClassBlockMatcher(MacroBlock.class), Block.Axes.DESCENDANT);

        for (LinkBlock linkBlock : linkBlocks) {
            result.add(linkBlock.getReference());
        }

        for (ImageBlock imageBlock : imageBlocks) {
            result.add(imageBlock.getReference());
        }

        for (MacroBlock macroBlock : macroBlocks) {
            MacroRefactoring macroRefactoring = this.defaultMacroRefactoringProvider.get();
            if (this.componentManager.hasComponent(MacroRefactoring.class, macroBlock.getId())) {
                try {
                    macroRefactoring =
                        this.componentManager.getInstance(MacroRefactoring.class, macroBlock.getId());
                } catch (ComponentLookupException e) {
                    this.logger.warn("Error while loading macro refactoring component for macro [{}]: [{}]",
                        macroBlock.getId(), ExceptionUtils.getRootCauseMessage(e));
                }
            }
            try {
                result.addAll(macroRefactoring.extractReferences(macroBlock));
            } catch (MacroRefactoringException e) {
                this.logger.warn("Error while extracting references from macro [{}]: [{}]",
                    macroBlock.getId(),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return result;
    }

    /**
     * Retrieve all entity references contained in the given XDOM. This method checks for references contained in
     * links, images, or macros.
     *
     * @param dom the XDOM for which to retrieve links
     * @param entityTypes mapping of the types of references to return (and their corresponding resource types)
     * @param currentReference the current document reference for making a relative resolution
     * @return a set of references contained in the XDOM
     * @since 14.2RC1
     */
    public Set<EntityReference> getUniqueLinkedEntityReferences(XDOM dom,
        Map<EntityType, Set<ResourceType>> entityTypes, DocumentReference currentReference)
    {
        Set<EntityReference> result = new LinkedHashSet<>();

        Set<ResourceReference> resourceReferences = this.extractReferences(dom);
        for (ResourceReference resourceReference : resourceReferences) {
            addReference(resourceReference, entityTypes, currentReference, result);
        }

        return result;
    }

    private void addReference(ResourceReference reference, Map<EntityType, Set<ResourceType>> entityTypes,
        DocumentReference currentReference, Set<EntityReference> references)
    {
        String referenceString = reference.getReference();
        ResourceType resourceType = reference.getType();

        Optional<EntityType> entityType =
            entityTypes.entrySet().stream().filter(e -> e.getValue().contains(resourceType)).map(Map.Entry::getKey)
                .findFirst();

        if (entityType.isEmpty()) {
            // We are only interested in resources leading to an entity type mapped to the resource type.
            return;
        }

        // Optimization: If the reference is empty, the link is an autolink and we don`t include it.
        if (StringUtils.isEmpty(referenceString)) {
            return;
        }

        EntityReference linkEntityReference = this.entityReferenceResolver.resolve(reference, entityType.get());

        // Verify after resolving it that the link is not an autolink (i.e., a link to the current document)
        if (!Objects.equals(linkEntityReference.extractReference(currentReference.getType()), currentReference)) {
            // Since this method is used for saving backlinks and since backlinks must be
            // saved with the space and page name but without the wiki part, we remove the wiki
            // part before serializing.
            // This is a bit of a hack since the default serializer should theoretically fail
            // if it's passed an invalid reference.
            references.add(linkEntityReference);
        }
    }
}
