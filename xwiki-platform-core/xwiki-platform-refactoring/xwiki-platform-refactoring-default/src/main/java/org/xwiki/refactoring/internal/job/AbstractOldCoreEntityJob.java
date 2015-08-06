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
package org.xwiki.refactoring.internal.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceTree;
import org.xwiki.model.reference.EntityReferenceTreeNode;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.EntityRequest;
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
 * Abstract job that targets multiple entities and which relies on the old-core implementation.
 * 
 * @param <R> the request type
 * @param <S> the job status type
 * @version $Id$
 * @since 7.2M1
 */
public abstract class AbstractOldCoreEntityJob<R extends EntityRequest, S extends EntityJobStatus<? super R>> extends
    AbstractEntityJob<R, S>
{
    /**
     * Regular expression used to match the special characters supported by the like HQL operator (plus the escaping
     * character).
     */
    private static final Pattern LIKE_SPECIAL_CHARS = Pattern.compile("([%_/])");

    /**
     * Used to perform the low level operations on entities.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to serialize a space reference in order to query the child documents.
     */
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    /**
     * Used to resolve the references of child documents.
     */
    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    /**
     * Used to query the child documents.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * Used to get a {@link BlockRenderer} dynamically.
     * 
     * @see #updateRelativeLinks(XWikiDocument, DocumentReference)
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    /**
     * Used to serialize link references.
     * 
     * @see #updateRelativeLinks(XWikiDocument, DocumentReference)
     */
    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    protected boolean copy(DocumentReference source, DocumentReference destination)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        DocumentReference userReference = xcontext.getUserReference();
        try {
            xcontext.setUserReference(this.request.getUserReference());
            String language = source.getLocale() != null ? source.getLocale().toString() : null;
            boolean result =
                xcontext.getWiki().copyDocument(source, destination, language, false, true, false, xcontext);
            if (result) {
                this.logger.info("Document [{}] has been copied to [{}].", source, destination);
            } else {
                this.logger.warn("Cannot fully copy [{}] to [{}] because an orphan translation"
                    + " exists at the destination.", source, destination);
            }
            return result;
        } catch (Exception e) {
            this.logger.error("Failed to copy [{}] to [{}].", source, destination, e);
            return false;
        } finally {
            xcontext.setUserReference(userReference);
        }
    }

    protected boolean delete(DocumentReference reference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        DocumentReference userReference = xcontext.getUserReference();
        try {
            xcontext.setUserReference(this.request.getUserReference());
            XWikiDocument document = xcontext.getWiki().getDocument(reference, xcontext);
            if (document.getTranslation() == 1) {
                xcontext.getWiki().deleteDocument(document, xcontext);
                this.logger.info("Document [{}] has been deleted.", reference);
            } else {
                xcontext.getWiki().deleteAllDocuments(document, xcontext);
                this.logger.info("Document [{}] has been deleted with all its translations.", reference);
            }
            return true;
        } catch (Exception e) {
            this.logger.error("Failed to delete document [{}].", reference, e);
            return false;
        } finally {
            xcontext.setUserReference(userReference);
        }
    }

    protected boolean exists(DocumentReference reference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        return xcontext.getWiki().exists(reference, xcontext);
    }

    private List<DocumentReference> getDocumentReferences(SpaceReference spaceReference)
    {
        try {
            // At the moment we don't have a way to retrieve only the direct children so we select all the descendants.
            // This means we select all the documents from the specified space and from all the nested spaces.
            String statement =
                "select distinct(doc.fullName) from XWikiDocument as doc "
                    + "where doc.space = :space or doc.space like :spacePrefix escape '/'";
            Query query = this.queryManager.createQuery(statement, Query.HQL);
            query.setWiki(spaceReference.getWikiReference().getName());
            String localSpaceReference = this.localEntityReferenceSerializer.serialize(spaceReference);
            query.bindValue("space", localSpaceReference);
            String spacePrefix = LIKE_SPECIAL_CHARS.matcher(localSpaceReference).replaceAll("/$1");
            query.bindValue("spacePrefix", spacePrefix + ".%");

            List<DocumentReference> descendants = new ArrayList<>();
            for (Object fullName : query.execute()) {
                descendants.add(this.explicitDocumentReferenceResolver.resolve((String) fullName, spaceReference));
            }
            return descendants;
        } catch (Exception e) {
            this.logger.error("Failed to retrieve the documents from [{}].", spaceReference, e);
            return Collections.emptyList();
        }
    }

    private EntityReferenceTreeNode getDocumentReferenceTree(SpaceReference spaceReference)
    {
        return new EntityReferenceTree(getDocumentReferences(spaceReference)).get(spaceReference);
    }

    protected void visitDocuments(SpaceReference spaceReference, Visitor<DocumentReference> visitor)
    {
        visitDocumentNodes(getDocumentReferenceTree(spaceReference), visitor);
    }

    protected List<DocumentReference> getBackLinkedReferences(DocumentReference documentReference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            return xcontext.getWiki().getDocument(documentReference, xcontext).getBackLinkedReferences(xcontext);
        } catch (XWikiException e) {
            this.logger.error("Failed to retrieve the back-links for document [{}].", documentReference, e);
            return Collections.emptyList();
        }
    }

    protected void renameLinks(DocumentReference documentReference, DocumentReference oldLinkTarget,
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

    protected void updateRelativeLinks(DocumentReference oldReference, DocumentReference newReference)
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
