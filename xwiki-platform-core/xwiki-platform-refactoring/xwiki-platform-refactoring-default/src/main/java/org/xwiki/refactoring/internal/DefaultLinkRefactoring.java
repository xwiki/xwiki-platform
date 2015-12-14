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

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.BlockRenderer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

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
     * Used to resolve link references.
     */
    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    /**
     * Used to serialize link references.
     *
     * @see #updateRelativeLinks(XWikiDocument, DocumentReference)
     */
    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    /**
     * Used to get a {@link BlockRenderer} dynamically.
     *
     * @see #updateRelativeLinks(XWikiDocument, DocumentReference)
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Override
    public void renameLinks(DocumentReference documentReference, DocumentReference oldLinkTarget,
        DocumentReference newLinkTarget)
    {
        boolean popLevelProgress = false;
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
            List<Locale> locales = document.getTranslationLocales(xcontext);

            this.progressManager.pushLevelProgress(1 + locales.size(), this);
            popLevelProgress = true;

            // Update the default locale instance.
            this.progressManager.startStep(this);
            renameLinks(document, oldLinkTarget, newLinkTarget);

            // Update the translations.
            for (Locale locale : locales) {
                this.progressManager.startStep(this);
                renameLinks(document.getTranslatedDocument(locale, xcontext), oldLinkTarget, newLinkTarget);
            }
        } catch (XWikiException e) {
            this.logger.error("Failed to rename the links that target [{}] from [{}].", oldLinkTarget,
                documentReference, e);
        } finally {
            if (popLevelProgress) {
                this.progressManager.popLevelProgress(this);
            }
        }
    }

    private void renameLinks(XWikiDocument document, DocumentReference oldTarget, DocumentReference newTarget)
        throws XWikiException
    {
        // We support only the syntaxes for which there is an available renderer.
        if (!this.contextComponentManagerProvider.get().hasComponent(BlockRenderer.class,
            document.getSyntax().toIdString())) {
            this.logger.warn("We can't rename the links from [{}] "
                + "because there is no renderer available for its syntax [{}].", document.getDocumentReference(),
                document.getSyntax());
            return;
        }

        XDOM xdom = document.getXDOM();
        List<LinkBlock> linkBlockList = xdom.getBlocks(new ClassBlockMatcher(LinkBlock.class), Block.Axes.DESCENDANT);

        boolean modified = false;
        for (LinkBlock linkBlock : linkBlockList) {
            ResourceReference linkReference = linkBlock.getReference();
            if (linkReference.getType().equals(ResourceType.DOCUMENT)) {
                DocumentReference target =
                    this.explicitDocumentReferenceResolver.resolve(linkReference.getReference(),
                        document.getDocumentReference());

                if (target.equals(oldTarget)) {
                    modified = true;
                    linkReference.setReference(this.compactEntityReferenceSerializer.serialize(newTarget,
                        document.getDocumentReference()));
                }
            }
        }

        if (modified) {
            XWikiContext xcontext = this.xcontextProvider.get();
            document.setContent(xdom);
            document.setAuthorReference(xcontext.getUserReference());
            xcontext.getWiki().saveDocument(document, "Renamed back-links.", xcontext);
            this.logger.info("The links from [{}] that were targeting [{}] have been updated to target [{}].",
                document.getDocumentReferenceWithLocale(), oldTarget, newTarget);
        } else {
            this.logger.info("No back-links to update in [{}].", document.getDocumentReference());
        }
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
            this.logger.warn("We can't update the relative links from [{}]"
                + " because there is no renderer available for its syntax [{}].", document.getDocumentReference(),
                document.getSyntax());
            return;
        }

        XDOM xdom = document.getXDOM();
        List<LinkBlock> linkBlockList = xdom.getBlocks(new ClassBlockMatcher(LinkBlock.class), Block.Axes.DESCENDANT);

        boolean modified = false;
        for (LinkBlock linkBlock : linkBlockList) {
            ResourceReference linkReference = linkBlock.getReference();
            if (linkReference.getType().equals(ResourceType.DOCUMENT)) {
                DocumentReference oldLinkReference =
                    this.explicitDocumentReferenceResolver.resolve(linkReference.getReference(), oldDocumentReference);

                DocumentReference newLinkReference =
                    this.explicitDocumentReferenceResolver.resolve(linkReference.getReference(),
                        document.getDocumentReference());

                if (!newLinkReference.equals(oldLinkReference)) {
                    modified = true;
                    linkReference.setReference(this.compactEntityReferenceSerializer.serialize(oldLinkReference,
                        document.getDocumentReference()));
                }
            }
        }

        if (modified) {
            XWikiContext xcontext = this.xcontextProvider.get();
            document.setContent(xdom);
            document.setAuthorReference(xcontext.getUserReference());
            xcontext.getWiki().saveDocument(document, "Updated the relative links.", true, xcontext);
            this.logger.info("Updated the relative links from [{}].", document.getDocumentReference());
        } else {
            this.logger.info("No relative links to update in [{}].", document.getDocumentReference());
        }
    }
}
