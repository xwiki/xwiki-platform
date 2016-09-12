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

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

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
    private static final LocalDocumentReference REDIRECT_CLASS_REFERENCE = new LocalDocumentReference(
        XWiki.SYSTEM_SPACE, "RedirectClass");

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

    @Inject
    private JobProgressManager progressManager;

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
                this.logger.warn("Cannot fully copy [{}] to [{}] because an orphan translation"
                    + " exists at the destination.", source, destination);
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
    public boolean exists(DocumentReference reference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        return xcontext.getWiki().exists(reference, xcontext);
    }

    @Override
    public List<DocumentReference> getBackLinkedReferences(DocumentReference documentReference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            return xcontext.getWiki().getDocument(documentReference, xcontext).getBackLinkedReferences(xcontext);
        } catch (XWikiException e) {
            this.logger.error("Failed to retrieve the back-links for document [{}].", documentReference, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<DocumentReference> getDocumentReferences(SpaceReference spaceReference)
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

            if (save) {
                wiki.saveDocument(document, "Update document after refactoring.", true, context);
                this.logger.info("Document [{}] has been updated.", documentReference);
            }
        } catch (Exception e) {
            this.logger.error("Failed to update the document [{}] after refactoring.", documentReference, e);
        }
    }
}
