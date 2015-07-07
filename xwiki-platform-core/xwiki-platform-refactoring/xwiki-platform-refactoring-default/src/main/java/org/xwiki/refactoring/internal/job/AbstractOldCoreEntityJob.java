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
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.EntityRequest;

import com.xpn.xwiki.XWikiContext;
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
     * 
     * @see #getChildren(DocumentReference)
     */
    private static final Pattern LIKE_SPECIAL_CHARS = Pattern.compile("([%_/])");

    /**
     * Used to perform the low level operations on entities.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to serialize a space reference in order to query the child document.
     * 
     * @see #getChildren(DocumentReference)
     */
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    /**
     * Used to resolve the references of child documents.
     * 
     * @see #getChildren(DocumentReference)
     */
    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    /**
     * Used to query the child documents.
     */
    @Inject
    private QueryManager queryManager;

    protected boolean copy(DocumentReference source, DocumentReference destination)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        DocumentReference userReference = xcontext.getUserReference();
        try {
            xcontext.setUserReference(this.request.getUserReference());
            boolean result = xcontext.getWiki().copyDocument(source, destination, null, false, true, false, xcontext);
            if (result) {
                this.logger.info("Document [{}] has been copied to [{}].", source, destination);
            } else {
                this.logger.warn(
                    "Cannot fully copy [{}] to [{}] because an orphan translation exists at the destination.", source,
                    destination);
            }
            return result;
        } catch (Exception e) {
            this.logger.warn("Failed to copy [{}] to [{}].", source, destination, e);
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
            xcontext.getWiki().deleteAllDocuments(document, xcontext);
            this.logger.info("Document [{}] has been deleted with all its translations.", reference);
            return true;
        } catch (Exception e) {
            this.logger.warn("Failed to delete document [{}].", reference, e);
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

    /**
     * @param documentReference the reference to a non-terminal (WebHome) document
     * @return the list of references to the child documents of the specified parent document
     */
    protected List<DocumentReference> getChildren(DocumentReference documentReference)
    {
        try {
            // We don't have a way to retrieve only the direct children so we select all the descendants. This means we
            // select all the documents from the same space (excluding the given document) and from all the nested
            // spaces.
            String statement =
                "select distinct(doc.fullName) from XWikiDocument as doc "
                    + "where (doc.space = :space and doc.name <> :name) or doc.space like :spacePrefix escape '/'";
            Query query = this.queryManager.createQuery(statement, "hql");
            query.setWiki(documentReference.getWikiReference().getName());
            String localSpaceReference = this.localEntityReferenceSerializer.serialize(documentReference.getParent());
            query.bindValue("space", localSpaceReference).bindValue("name", documentReference.getName());
            String spacePrefix = LIKE_SPECIAL_CHARS.matcher(localSpaceReference).replaceAll("/$1");
            query.bindValue("spacePrefix", spacePrefix + ".%");

            List<DocumentReference> children = new ArrayList<>();
            for (Object fullName : query.execute()) {
                children.add(this.explicitDocumentReferenceResolver.resolve((String) fullName, documentReference));
            }
            return children;
        } catch (Exception e) {
            this.logger.warn("Failed to retrieve the child documents of [{}].", documentReference, e);
            return Collections.emptyList();
        }
    }
}
