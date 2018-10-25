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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.api.AbstractCheckRightsRequest;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DeletedDocument;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.parentchild.ParentChildConfiguration;

/**
 * Default implementation of {@link ModelBridge} based on the old XWiki model.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Singleton
public class DefaultModelBridge implements ModelBridge
{
    /**
     * Regular expression used to match the special characters supported by the like HQL operator (plus the escaping
     * character).
     */
    private static final Pattern LIKE_SPECIAL_CHARS = Pattern.compile("([%_/])");

    /**
     * The reference to the type of object used to create an automatic redirect when renaming or moving a document.
     */
    private static final LocalDocumentReference REDIRECT_CLASS_REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "RedirectClass");

    @Inject
    private Logger logger;

    /**
     * Used to perform the low level operations on entities.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to query the child documents.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * Used to serialize a space reference in order to query the child documents.
     */
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    /**
     * Used to serialize the redirect location.
     *
     * @see #createRedirect(DocumentReference, DocumentReference)
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * Used to resolve the references of child documents.
     */
    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    /**
     * Used to check the old and new parents reference.
     */
    @Inject
    private DocumentReferenceResolver<EntityReference> entityReferenceDocumentReferenceResolver;

    /**
     * Used to create the minimum need parent reference.
     */
    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    /**
     * Use to get back a relative reference based on a compact string reference.
     */
    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeStringEntityReferenceResolver;

    @Inject
    private JobProgressManager progressManager;

    @Inject
    private ParentChildConfiguration parentChildConfiguration;

    @Inject
    private EntityReferenceProvider entityReferenceProvider;

    @Override
    public boolean create(DocumentReference documentReference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            XWikiDocument newDocument = xcontext.getWiki().getDocument(documentReference, xcontext);
            xcontext.getWiki().saveDocument(newDocument, xcontext);
            this.logger.info("Document [{}] has been created.", documentReference);
            return true;
        } catch (Exception e) {
            this.logger.error("Failed to create document [{}].", documentReference, e);
            return false;
        }
    }

    @Override
    public boolean copy(DocumentReference source, DocumentReference destination)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            String language = source.getLocale() != null ? source.getLocale().toString() : null;
            boolean result =
                xcontext.getWiki().copyDocument(source, destination, language, false, true, true, xcontext);
            if (result) {
                this.logger.info("Document [{}] has been copied to [{}].", source, destination);
            } else {
                this.logger.warn(
                    "Cannot fully copy [{}] to [{}] because an orphan translation" + " exists at the destination.",
                    source, destination);
            }
            return result;
        } catch (Exception e) {
            this.logger.error("Failed to copy [{}] to [{}].", source, destination, e);
            return false;
        }
    }

    @Override
    public boolean delete(DocumentReference reference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
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
        }
    }

    @Override
    public boolean removeLock(DocumentReference reference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            XWikiDocument document = xcontext.getWiki().getDocument(reference, xcontext);

            if (document.getLock(xcontext) != null) {
                document.removeLock(xcontext);
                this.logger.info("Document [{}] has been unlocked.", reference);
            }

            return true;
        } catch (Exception e) {
            // Just warn, since it's a recoverable situation.
            this.logger.warn("Failed to unlock document [{}].", reference, e);
            return false;
        }
    }

    @Override
    public void createRedirect(DocumentReference oldReference, DocumentReference newReference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        DocumentReference redirectClassReference =
            new DocumentReference(REDIRECT_CLASS_REFERENCE, oldReference.getWikiReference());
        if (xcontext.getWiki().exists(redirectClassReference, xcontext)) {
            try {
                XWikiDocument oldDocument = xcontext.getWiki().getDocument(oldReference, xcontext);
                int number = oldDocument.createXObject(redirectClassReference, xcontext);
                String location = this.defaultEntityReferenceSerializer.serialize(newReference);
                oldDocument.getXObject(redirectClassReference, number).setStringValue("location", location);
                oldDocument.setHidden(true);
                xcontext.getWiki().saveDocument(oldDocument, "Create automatic redirect.", xcontext);
                this.logger.info("Created automatic redirect from [{}] to [{}].", oldReference, newReference);
            } catch (XWikiException e) {
                this.logger.error("Failed to create automatic redirect from [{}] to [{}].", oldReference, newReference,
                    e);
            }
        } else {
            this.logger.warn("We can't create an automatic redirect from [{}] to [{}] because [{}] is missing.",
                oldReference, newReference, redirectClassReference);
        }
    }

    @Override
    public boolean canOverwriteSilently(DocumentReference documentReference)
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
            DocumentReference redirectClassReference =
                new DocumentReference(REDIRECT_CLASS_REFERENCE, documentReference.getWikiReference());
            // Overwrite silently the redirect pages.
            return document.getXObject(redirectClassReference) != null;
        } catch (XWikiException e) {
            this.logger.warn("Failed to get document [{}]. Root cause: [{}].", documentReference,
                ExceptionUtils.getRootCauseMessage(e));
            return false;
        }
    }

    @Override
    public boolean exists(DocumentReference reference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        return xcontext.getWiki().exists(reference, xcontext);
    }

    @Override
    public List<DocumentReference> getBackLinkedReferences(DocumentReference documentReference, String wikiId)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        String previousWikiId = xcontext.getWikiId();
        try {
            xcontext.setWikiId(wikiId);
            return xcontext.getWiki().getDocument(documentReference, xcontext).getBackLinkedReferences(xcontext);
        } catch (XWikiException e) {
            this.logger.error("Failed to retrieve the back-links for document [{}] on wiki [{}].", documentReference,
                wikiId, e);
            return Collections.emptyList();
        } finally {
            xcontext.setWikiId(previousWikiId);
        }
    }

    @Override
    public List<DocumentReference> getDocumentReferences(SpaceReference spaceReference)
    {
        try {
            // At the moment we don't have a way to retrieve only the direct children so we select all the descendants.
            // This means we select all the documents from the specified space and from all the nested spaces.
            String statement = "select distinct(doc.fullName) from XWikiDocument as doc "
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

    @Override
    public boolean updateParentField(final DocumentReference oldParentReference,
        final DocumentReference newParentReference)
    {
        XWikiContext context = xcontextProvider.get();
        XWiki wiki = context.getWiki();

        boolean popLevelProgress = false;
        try {
            // Note: This operation could have been done in Hibernate (using the Store API) in one single update query.
            // However, due to XWiki's document cache, it`s better in the end to use the Document API and update each
            // child document individually.
            XWikiDocument oldParentDocument = wiki.getDocument(oldParentReference, context);

            List<DocumentReference> childReferences = oldParentDocument.getChildrenReferences(context);

            if (childReferences.size() > 0) {
                this.progressManager.pushLevelProgress(childReferences.size(), this);
                popLevelProgress = true;
            }

            for (DocumentReference childReference : childReferences) {
                this.progressManager.startStep(this);

                XWikiDocument childDocument = wiki.getDocument(childReference, context);
                childDocument.setParentReference(newParentReference);

                wiki.saveDocument(childDocument, "Updated parent field.", true, context);

                this.progressManager.endStep(this);
            }

            if (childReferences.size() > 0) {
                this.logger.info("Document parent fields updated from [{}] to [{}] for [{}] documents.",
                    oldParentReference, newParentReference, childReferences.size());
            }
        } catch (Exception e) {
            this.logger.error("Failed to update the document parent fields from [{}] to [{}].", oldParentReference,
                newParentReference, e);
            return false;
        } finally {
            if (popLevelProgress) {
                this.progressManager.popLevelProgress(this);
            }
        }

        return true;
    }

    @Override
    public DocumentReference setContextUserReference(DocumentReference userReference)
    {
        XWikiContext context = xcontextProvider.get();
        DocumentReference previousUserReference = context.getUserReference();
        context.setUserReference(userReference);
        return previousUserReference;
    }

    @Override
    public void update(DocumentReference documentReference, Map<String, String> parameters)
    {
        try {
            XWikiContext context = xcontextProvider.get();
            XWiki wiki = context.getWiki();
            XWikiDocument document = wiki.getDocument(documentReference, context);
            boolean save = false;

            String title = parameters.get("title");
            if (title != null && !title.equals(document.getTitle())) {
                document.setTitle(title);
                save = true;
            }

            // Some old applications still rely on the parent/child links between documents.
            // For the retro-compatibility, we synchronize the "parent" field of the document with the (real)
            // hierarchical parent.
            //
            // But if the user has voluntary enabled the legacy "parent/child" mechanism for the breadcrumbs, we keep
            // the old behaviour when location and parent/child mechanism were not linked.
            //
            // More information: https://jira.xwiki.org/browse/XWIKI-13493
            if (!parentChildConfiguration.isParentChildMechanismEnabled()) {
                DocumentReference hierarchicalParent = getHierarchicalParent(documentReference);
                DocumentReference oldParent = document.getParentReference();

                DocumentReference hierarchicalParentDoc = this.entityReferenceDocumentReferenceResolver
                    .resolve(hierarchicalParent);
                DocumentReference oldParentDoc = this.entityReferenceDocumentReferenceResolver.resolve(oldParent);

                if (!hierarchicalParentDoc.equals(oldParentDoc)) {
                    String parentSerializedReference = this.compactEntityReferenceSerializer
                        .serialize(hierarchicalParent, documentReference);

                    document.setParentReference(this.relativeStringEntityReferenceResolver
                        .resolve(parentSerializedReference, EntityType.DOCUMENT));
                    save = true;
                }
            }

            if (save) {
                wiki.saveDocument(document, "Update document after refactoring.", true, context);
                this.logger.info("Document [{}] has been updated.", documentReference);
            }
        } catch (Exception e) {
            this.logger.error("Failed to update the document [{}] after refactoring.", documentReference, e);
        }
    }

    private DocumentReference getHierarchicalParent(DocumentReference documentReference)
    {
        final String spaceHomePage = entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();

        EntityReference parentOfTheSpace = documentReference.getLastSpaceReference().getParent();

        boolean pageIsNotTerminal = documentReference.getName().equals(spaceHomePage);

        // Case 1: The document has the location A.B.C.WebHome
        // The parent should be A.B.WebHome
        if (pageIsNotTerminal && parentOfTheSpace.getType() == EntityType.SPACE) {
            return new DocumentReference(spaceHomePage, new SpaceReference(parentOfTheSpace));
        }

        // Case 2: The document has the location A.WebHome
        // The parent should be Main.WebHome
        if (pageIsNotTerminal && parentOfTheSpace.getType() == EntityType.WIKI) {
            return new DocumentReference(spaceHomePage,
                new SpaceReference(entityReferenceProvider.getDefaultReference(EntityType.SPACE).getName(),
                    documentReference.getWikiReference()));
        }

        // Case 3: The document has the location A.B
        // The parent should be A.WebHome
        return new DocumentReference(spaceHomePage, documentReference.getLastSpaceReference());
    }

    @Override
    public boolean restoreDeletedDocument(long deletedDocumentId, AbstractCheckRightsRequest request)
    {
        XWikiContext context = this.xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        DocumentReference deletedDocumentReference = null;
        try {
            // Retrieve the deleted document by ID.
            XWikiDeletedDocument deletedDocument = xwiki.getDeletedDocument(deletedDocumentId, context);
            if (deletedDocument == null) {
                logger.error("Deleted document with ID [{}] does not exist.", deletedDocumentId);
                return false;
            }

            deletedDocumentReference = deletedDocument.getDocumentReference();

            // If the document (or the translation) that we want to restore does not exist, restore it.
            if (xwiki.exists(deletedDocumentReference, context)) {
                // TODO: Add overwrite support maybe also with interactive (question/answer) mode.
                // Default for now is to skip and log as error to restore over existing documents.
                logger.error("Document [{}] with ID [{}] can not be restored. Document already exists",
                    deletedDocument.getFullName(), deletedDocumentId);
            } else if (request.isCheckAuthorRights()
                && !canRestoreDeletedDocument(deletedDocument, context.getAuthorReference())) {
                logger.error("The author [{}] of this script is not allowed to restore document [{}] with ID [{}]",
                    context.getAuthorReference(), deletedDocumentReference, deletedDocumentId);
            } else if (request.isCheckRights()
                && !canRestoreDeletedDocument(deletedDocument, context.getUserReference())) {
                logger.error("You are not allowed to restore document [{}] with ID [{}]", deletedDocumentReference,
                    deletedDocumentId);
            } else {
                // Restore the document.
                xwiki.restoreFromRecycleBin(deletedDocument.getId(), "Restored from recycle bin", context);

                logger.info("Document [{}] has been restored", deletedDocumentReference);

                return true;
            }
        } catch (Exception e) {
            // Try to log the document reference since it`s more useful than the ID.
            if (deletedDocumentReference != null) {
                logger.error("Failed to restore document [{}] with ID [{}]", deletedDocumentReference,
                    deletedDocumentId, e);
            } else {
                logger.error("Failed to restore deleted document with ID [{}]", deletedDocumentId, e);
            }
        }

        return false;
    }

    @Override
    public List<Long> getDeletedDocumentIds(String batchId)
    {
        XWikiContext context = this.xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        List<Long> result = new ArrayList<>();
        try {
            XWikiDeletedDocument[] deletedDocuments =
                xwiki.getRecycleBinStore().getAllDeletedDocuments(batchId, false, context, true);
            for (XWikiDeletedDocument deletedDocument : deletedDocuments) {
                result.add(deletedDocument.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to get deleted document IDs for batch [{}]", batchId);
        }

        return result;
    }

    protected boolean canRestoreDeletedDocument(XWikiDeletedDocument deletedDocument, DocumentReference userReference)
    {
        boolean result = false;

        XWikiContext context = this.xcontextProvider.get();

        // Remember the context user.
        DocumentReference currentUserReference = context.getUserReference();
        try {
            // Reuse the DeletedDocument API to check rights.
            DeletedDocument deletedDocumentApi = new DeletedDocument(deletedDocument, context);

            // Note: DeletedDocument API works with the current context user.
            context.setUserReference(userReference);

            result = deletedDocumentApi.canUndelete();
        } catch (Exception e) {
            logger.error("Failed to check restore rights on deleted document [{}] for user [{}]",
                deletedDocument.getId(), userReference, e);
        } finally {
            // Restore the context user;
            context.setUserReference(currentUserReference);
        }

        return result;
    }
}
