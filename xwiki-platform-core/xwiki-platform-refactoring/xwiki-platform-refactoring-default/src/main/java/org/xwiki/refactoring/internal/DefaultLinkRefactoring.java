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

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.BlockRenderer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.render.LinkedResourceHelper;

/**
 * Default implementation of {@link LinkRefactoring}.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Singleton
public class DefaultLinkRefactoring implements LinkRefactoring
{
    @Inject
    private Logger logger;

    /**
     * Used to perform the low level operations on entities.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * The component used to record the progress.
     */
    @Inject
    private JobProgressManager progressManager;

    /**
     * Used to serialize link references.
     *
     * @see #updateRelativeLinks(XWikiDocument, DocumentReference)
     */
    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    @Inject
    private EntityReferenceResolver<ResourceReference> resourceReferenceResolver;

    /**
     * Used to get a {@link BlockRenderer} dynamically.
     *
     * @see #updateRelativeLinks(XWikiDocument, DocumentReference)
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    private LinkedResourceHelper linkedResourceHelper;

    @Override
    public void renameLinks(DocumentReference documentReference, DocumentReference oldLinkTarget,
        DocumentReference newLinkTarget)
    {
        boolean popLevelProgress = false;
        XWikiContext xcontext = this.xcontextProvider.get();
        String previousWikiId = xcontext.getWikiId();
        try {
            xcontext.setWikiId(documentReference.getWikiReference().getName());
            XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
            List<Locale> locales = document.getTranslationLocales(xcontext);

            this.progressManager.pushLevelProgress(1 + locales.size(), this);
            popLevelProgress = true;

            // Update the default locale instance.
            this.progressManager.startStep(this);
            renameLinks(document, oldLinkTarget, newLinkTarget);
            this.progressManager.endStep(this);

            // Update the translations.
            for (Locale locale : locales) {
                this.progressManager.startStep(this);
                renameLinks(document.getTranslatedDocument(locale, xcontext), oldLinkTarget, newLinkTarget);
                this.progressManager.endStep(this);
            }
        } catch (XWikiException e) {
            this.logger.error("Failed to rename the links that target [{}] from [{}].", oldLinkTarget,
                documentReference, e);
        } finally {
            if (popLevelProgress) {
                this.progressManager.popLevelProgress(this);
            }
            xcontext.setWikiId(previousWikiId);
        }
    }

    private void renameLinks(XWikiDocument document, DocumentReference oldTarget, DocumentReference newTarget)
        throws XWikiException
    {
        DocumentReference currentDocumentReference = document.getDocumentReference();

        // We support only the syntaxes for which there is an available renderer.
        if (!this.contextComponentManagerProvider.get().hasComponent(BlockRenderer.class,
            document.getSyntax().toIdString())) {
            this.logger.warn(
                "We can't rename the links from [{}] " + "because there is no renderer available for its syntax [{}].",
                currentDocumentReference, document.getSyntax());
            return;
        }

        XDOM xdom = document.getXDOM();
        List<Block> blocks = linkedResourceHelper.getBlocks(xdom);

        boolean modified = false;
        for (Block block : blocks) {
            try {
                modified |= renameLink(block, currentDocumentReference, oldTarget, newTarget);
            } catch (IllegalArgumentException e) {
                continue;
            }
        }

        if (modified) {
            document.setContent(xdom);
            saveDocumentPreservingContentAuthor(document, "Renamed back-links.", false);
            this.logger.info("The links from [{}] that were targeting [{}] have been updated to target [{}].",
                document.getDocumentReferenceWithLocale(), oldTarget, newTarget);
        } else {
            this.logger.info("No back-links to update in [{}].", currentDocumentReference);
        }
    }

    private boolean renameLink(Block block, DocumentReference currentDocumentReference, DocumentReference oldTarget,
        DocumentReference newTarget) throws IllegalArgumentException
    {
        boolean modified = false;

        ResourceReference resourceReference = linkedResourceHelper.getResourceReference(block);
        if (resourceReference == null) {
            // Skip invalid blocks.
            throw new IllegalArgumentException();
        }

        ResourceType resourceType = resourceReference.getType();

        // TODO: support ATTACHMENT as well.
        if (!ResourceType.DOCUMENT.equals(resourceType) && !ResourceType.SPACE.equals(resourceType)) {
            // We are currently only interested in Document or Space references.
            throw new IllegalArgumentException();
        }

        // Resolve the resource reference.
        EntityReference linkEntityReference =
            resourceReferenceResolver.resolve(resourceReference, null, currentDocumentReference);
        // Resolve the document of the reference.
        DocumentReference linkTargetDocumentReference =
            defaultReferenceDocumentReferenceResolver.resolve(linkEntityReference);
        EntityReference newTargetReference = newTarget;
        ResourceType newResourceType = resourceType;

        // If the link was resolved to a space...
        if (EntityType.SPACE.equals(linkEntityReference.getType())) {
            if (XWiki.DEFAULT_SPACE_HOMEPAGE.equals(newTarget.getName())) {
                // If the new document reference is also a space (non-terminal doc), be careful to keep it
                // serialized as a space still (i.e. without ".WebHome") and not serialize it as a doc by mistake
                // (i.e. with ".WebHome").
                newTargetReference = newTarget.getLastSpaceReference();
            } else {
                // If the new target is a non-terminal document, we can not use a "space:" resource type to access
                // it anymore. To fix it, we need to change the resource type of the link reference "doc:".
                newResourceType = ResourceType.DOCUMENT;
            }
        }

        // If the link targets the old (renamed) document reference, we must update it.
        if (linkTargetDocumentReference.equals(oldTarget)) {
            modified = true;
            String newReferenceString =
                this.compactEntityReferenceSerializer.serialize(newTargetReference, currentDocumentReference);

            // Update the reference in the XDOM.
            linkedResourceHelper.setResourceReferenceString(block, newReferenceString);
            linkedResourceHelper.setResourceType(block, newResourceType);
        }

        return modified;
    }

    @Override
    public void updateRelativeLinks(DocumentReference oldReference, DocumentReference newReference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            updateRelativeLinks(xcontext.getWiki().getDocument(newReference, xcontext), oldReference);
        } catch (XWikiException e) {
            this.logger.error("Failed to update the relative links from [{}].", newReference, e);
        }
    }

    private void updateRelativeLinks(XWikiDocument document, DocumentReference oldDocumentReference)
        throws XWikiException
    {
        // We support only the syntaxes for which there is an available renderer.
        if (!this.contextComponentManagerProvider.get().hasComponent(BlockRenderer.class,
            document.getSyntax().toIdString())) {
            this.logger.warn(
                "We can't update the relative links from [{}]"
                    + " because there is no renderer available for its syntax [{}].",
                document.getDocumentReference(), document.getSyntax());
            return;
        }

        DocumentReference newDocumentReference = document.getDocumentReference();

        XDOM xdom = document.getXDOM();
        List<Block> blocks = linkedResourceHelper.getBlocks(xdom);

        boolean modified = false;
        for (Block block : blocks) {
            ResourceReference resourceReference = linkedResourceHelper.getResourceReference(block);
            if (resourceReference == null || StringUtils.isEmpty(resourceReference.getReference())) {
                // Skip invalid blocks.
                continue;
            }

            ResourceType resourceType = resourceReference.getType();

            // TODO: support ATTACHMENT as well.
            if (!ResourceType.DOCUMENT.equals(resourceType) && !ResourceType.SPACE.equals(resourceType)) {
                // We are currently only interested in Document or Space references.
                continue;
            }

            // current link, use the old document's reference to fill in blanks.
            EntityReference oldLinkReference =
                this.resourceReferenceResolver.resolve(resourceReference, null, oldDocumentReference);
            // new link, use the new document's reference to fill in blanks.
            EntityReference newLinkReference =
                this.resourceReferenceResolver.resolve(resourceReference, null, newDocumentReference);

            // If the new and old link references don`t match, then we must update the relative link.
            if (!newLinkReference.equals(oldLinkReference)) {
                modified = true;

                // Serialize the old (original) link relative to the new document's location, in compact form.
                String serializedLinkReference =
                    this.compactEntityReferenceSerializer.serialize(oldLinkReference, newDocumentReference);

                // Update the reference in the XDOM.
                linkedResourceHelper.setResourceReferenceString(block, serializedLinkReference);
            }
        }

        if (modified) {
            document.setContent(xdom);
            saveDocumentPreservingContentAuthor(document, "Updated the relative links.", true);
            this.logger.info("Updated the relative links from [{}].", document.getDocumentReference());
        } else {
            this.logger.info("No relative links to update in [{}].", document.getDocumentReference());
        }
    }

    /**
     * HACK: Save the given document without changing the content author because the document may loose or win
     * programming and script rights as a consequence and this is not the intent of this operation. Even though the
     * document content field was modified, the change is purely syntactic; the semantic is not affected so it's not
     * clear whether the content author deserves to be updated or not (even without the side effects).
     * 
     * @param document the document to be saved
     * @param comment the revision comment
     * @param minorEdit whether it's a minor edit or not
     * @throws XWikiException if saving the document fails
     */
    private void saveDocumentPreservingContentAuthor(XWikiDocument document, String comment, boolean minorEdit)
        throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        // Preserve the content author.
        document.setContentDirty(false);
        // Make sure the version is incremented.
        document.setMetaDataDirty(true);
        document.setAuthorReference(xcontext.getUserReference());
        xcontext.getWiki().saveDocument(document, comment, minorEdit, xcontext);
    }
}
