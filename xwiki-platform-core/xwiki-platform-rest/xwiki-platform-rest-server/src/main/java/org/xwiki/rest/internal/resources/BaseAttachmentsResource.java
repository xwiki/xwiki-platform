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
package org.xwiki.rest.internal.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang.StringUtils;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidator;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.internal.attachment.XWikiAttachmentAccessWrapper;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.internal.RangeIterable;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public class BaseAttachmentsResource extends XWikiResource
{
    /**
     * Helper class that contains newly created attachment information to be returned to the client. It contains the
     * JAXB attachment object and a boolean variable that states if the attachment existed before. This class is used by
     * the storeAttachment utility method.
     */
    protected static class AttachmentInfo
    {
        protected Attachment attachment;

        protected boolean alreadyExisting;

        public AttachmentInfo(Attachment attachment, boolean alreadyExisting)
        {
            this.attachment = attachment;
            this.alreadyExisting = alreadyExisting;
        }

        public Attachment getAttachment()
        {
            return attachment;
        }

        public boolean isAlreadyExisting()
        {
            return alreadyExisting;
        }
    }

    private static final String FILTER_FILE_TYPES = "fileTypes";

    private static final Pattern COMMA = Pattern.compile("\\s*,\\s*");

    private static final String DOT = ".";

    private static final Map<String, String> FILTER_TO_QUERY = new HashMap<>();

    static {
        FILTER_TO_QUERY.put("space", "doc.space");
        FILTER_TO_QUERY.put("page", "doc.fullName");
        FILTER_TO_QUERY.put("name", "attachment.filename");
        FILTER_TO_QUERY.put("author", "attachment.author");
    }

    @Inject
    protected ContextualAuthorizationManager authorization;

    @Inject
    private ModelFactory modelFactory;

    @Inject
    @Named("hidden/document")
    private QueryFilter hiddenDocumentFilter;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    /**
     * @param scope where to retrieve the attachments from; it should be a reference to a wiki, space or document
     * @param filters the filters used to restrict the set of attachments (you can filter by space name, document
     *     name, attachment name, author and type)
     * @param offset defines the start of the range
     * @param limit the maximum number of attachments to include in the range
     * @param withPrettyNames whether to include pretty names (like author full name and document title) in the
     *     returned attachment metadata
     * @return the list of attachments from the specified scope that match the given filters and that are within the
     *     specified range
     * @throws XWikiRestException if we fail to retrieve the attachments
     */
    protected Attachments getAttachments(EntityReference scope, Map<String, String> filters, Integer offset,
        Integer limit, Boolean withPrettyNames) throws XWikiRestException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        String database = xcontext.getWikiId();

        Attachments attachments = objectFactory.createAttachments();

        try {
            xcontext.setWikiId(scope.extractReference(EntityType.WIKI).getName());

            List<Object> queryResults = getAttachmentsQuery(scope, filters).setLimit(limit).setOffset(offset).execute();
            attachments.withAttachments(queryResults.stream().map(this::processAttachmentsQueryResult)
                // Apply passed filters
                .filter(getFileTypeFilter(filters.getOrDefault(FILTER_FILE_TYPES, "")))
                // Filter out attachments the current user is not allowed to see
                .filter(a -> authorization.hasAccess(Right.VIEW, a.getReference()))
                // Convert XWikiAttachment to REST Attachment
                .map(xwikiAttachment -> toRestAttachment(xwikiAttachment, withPrettyNames))
                .collect(Collectors.toList()));
        } catch (QueryException e) {
            throw new XWikiRestException(e);
        } finally {
            xcontext.setWikiId(database);
        }
        
        return attachments;
    }

    private Query getAttachmentsQuery(EntityReference scope, Map<String, String> filters) throws QueryException
    {
        StringBuilder statement = new StringBuilder().append("select doc.space, doc.name, doc.version, attachment")
            .append(" from XWikiDocument as doc, XWikiAttachment as attachment");

        Map<String, String> exactParams = new HashMap<>();
        Map<String, String> prefixParams = new HashMap<>();
        Map<String, String> suffixParams = new HashMap<>();
        Map<String, String> containsParams = new HashMap<>();

        List<String> whereClause = new ArrayList<>();
        whereClause.add("attachment.docId = doc.id");

        // Apply the specified scope.
        if (scope.getType() == EntityType.DOCUMENT) {
            whereClause.add("doc.fullName = :localDocumentReference");
            exactParams.put("localDocumentReference", this.localEntityReferenceSerializer.serialize(scope));
        } else if (scope.getType() == EntityType.SPACE) {
            whereClause.add("(doc.space = :localSpaceReference or doc.space like :localSpaceReferencePrefix)");
            String localSpaceReference = this.localEntityReferenceSerializer.serialize(scope);
            exactParams.put("localSpaceReference", localSpaceReference);
            prefixParams.put("localSpaceReferencePrefix", localSpaceReference + '.');
        }

        // Apply the specified filters.
        applyFilters(filters, whereClause, containsParams);

        // We need to handle the file type filter separately.
        applyFileTypeFilter(filters, whereClause, suffixParams, containsParams);

        statement.append(" where ").append(StringUtils.join(whereClause, " and "));

        Query query = queryManager.createQuery(statement.toString(), Query.HQL);

        // Bind the query parameter values.
        for (Map.Entry<String, String> entry : exactParams.entrySet()) {
            query.bindValue(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : prefixParams.entrySet()) {
            query.bindValue(entry.getKey()).literal(entry.getValue()).anyChars();
        }
        for (Map.Entry<String, String> entry : suffixParams.entrySet()) {
            query.bindValue(entry.getKey()).anyChars().literal(entry.getValue());
        }
        for (Map.Entry<String, String> entry : containsParams.entrySet()) {
            query.bindValue(entry.getKey()).anyChars().literal(entry.getValue()).anyChars();
        }

        query.addFilter(this.hiddenDocumentFilter);

        return query;
    }

    private void applyFilters(Map<String, String> filters, List<String> constraints, Map<String, String> parameters)
    {
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String column = FILTER_TO_QUERY.get(entry.getKey());
            if (!StringUtils.isEmpty(entry.getValue()) && column != null) {
                constraints.add(String.format("upper(%s) like :%s", column, entry.getKey()));
                parameters.put(entry.getKey(), entry.getValue().toUpperCase());
            }
        }
    }

    private void applyFileTypeFilter(Map<String, String> filters, List<String> constraints,
        Map<String, String> suffixParams, Map<String, String> containsParams)
    {
        List<String> fileTypeConstraints = new ArrayList<>();
        applyMediaTypeFilter(filters, fileTypeConstraints, containsParams);
        applyFileNameExtensionFilter(filters, fileTypeConstraints, suffixParams);
        if (!fileTypeConstraints.isEmpty()) {
            constraints.add("(" + StringUtils.join(fileTypeConstraints, " or ") + ")");
        }
    }

    private void applyMediaTypeFilter(Map<String, String> filters, List<String> constraints,
        Map<String, String> parameters)
    {
        Set<String> acceptedMediaTypes = getAcceptedMediaTypes(filters.getOrDefault(FILTER_FILE_TYPES, ""));
        if (!acceptedMediaTypes.isEmpty()) {
            // Not all the attachments have their media type saved in the database. We will filter out these attachments
            // afterwards.
            constraints.add("attachment.mimeType is null");
            constraints.add("attachment.mimeType = ''");
            int index = 0;
            for (String mediaType : acceptedMediaTypes) {
                String parameterName = "mediaType" + index++;
                constraints.add("upper(attachment.mimeType) like :" + parameterName);
                parameters.put(parameterName, mediaType);
            }
        }
    }

    private Set<String> getAcceptedMediaTypes(String fileTypesFilter)
    {
        // Filter out empty values and file name extensions (starting with dot) because we handle them separately.
        return Arrays.asList(COMMA.split(fileTypesFilter)).stream().filter(s -> !s.isEmpty() && !s.startsWith(DOT))
            .map(String::toUpperCase).collect(Collectors.toSet());
    }

    private void applyFileNameExtensionFilter(Map<String, String> filters, List<String> constraints,
        Map<String, String> parameters)
    {
        Set<String> acceptedFileNameExtensions =
            getAcceptedFileNameExtensions(filters.getOrDefault(FILTER_FILE_TYPES, ""));
        if (!acceptedFileNameExtensions.isEmpty()) {
            int index = 0;
            for (String extension : acceptedFileNameExtensions) {
                String parameterName = "extension" + index++;
                constraints.add("upper(attachment.filename) like :" + parameterName);
                parameters.put(parameterName, extension);
            }
        }
    }

    private Set<String> getAcceptedFileNameExtensions(String fileTypesFilter)
    {
        // File types that start with dot are considered file name extension filters.
        return Arrays.asList(COMMA.split(fileTypesFilter)).stream().filter(s -> s.startsWith(DOT))
            .map(String::toUpperCase).collect(Collectors.toSet());
    }

    private XWikiAttachment processAttachmentsQueryResult(Object queryResult)
    {
        Object[] fields = (Object[]) queryResult;
        List<String> pageSpaces = Utils.getSpacesFromSpaceId((String) fields[0]);
        String pageName = (String) fields[1];
        String pageVersion = (String) fields[2];
        XWikiAttachment attachment = (XWikiAttachment) fields[3];

        XWikiContext xcontext = this.xcontextProvider.get();
        DocumentReference documentReference = new DocumentReference(xcontext.getWikiId(), pageSpaces, pageName);
        XWikiDocument document = new XWikiDocument(documentReference);
        document.setVersion(pageVersion);
        attachment.setDoc(document, false);

        return attachment;
    }

    private Predicate<XWikiAttachment> getFileTypeFilter(String acceptedFileTypes)
    {
        // Not all the attachments have their media type stored in the database so we can't rely only on the
        // query-level filtering. We need to also detect the media type after the query is executed and filter
        // out the attachments that don't match the accepted media types.
        Set<String> acceptedMediaTypes = getAcceptedMediaTypes(acceptedFileTypes);
        Set<String> acceptedFileNameExtensions = getAcceptedFileNameExtensions(acceptedFileTypes);

        // We accept the attachment if:
        // * there's no media type filtering or
        // * the media type is stored (which means it was already filtered at the query level) or
        // * the file name matches the filter or
        // * the computed media type matches the filter
        return (attachment) -> acceptedMediaTypes.isEmpty() || !StringUtils.isEmpty(attachment.getMimeType())
            || hasAcceptedFileNameExtension(attachment, acceptedFileNameExtensions)
            || hasAcceptedMediaType(attachment, acceptedMediaTypes);
    }

    private boolean hasAcceptedFileNameExtension(XWikiAttachment attachment, Set<String> acceptedFileNameExtensions)
    {
        String fileName = attachment.getFilename().toUpperCase();
        return acceptedFileNameExtensions.stream()
            .anyMatch(acceptedFileNamedExtension -> fileName.endsWith(acceptedFileNamedExtension));
    }

    private boolean hasAcceptedMediaType(XWikiAttachment attachment, Set<String> acceptedMediaTypes)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        String detectedMediaType = attachment.getMimeType(xcontext).toUpperCase();
        return acceptedMediaTypes.stream().anyMatch(acceptedMediaType -> detectedMediaType.contains(acceptedMediaType));
    }

    private Attachment toRestAttachment(XWikiAttachment xwikiAttachment, Boolean withPrettyNames)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        com.xpn.xwiki.api.Attachment apiAttachment = new com.xpn.xwiki.api.Attachment(
            new Document(xwikiAttachment.getDoc(), xcontext), xwikiAttachment, xcontext);
        return this.modelFactory.toRestAttachment(this.uriInfo.getBaseUri(), apiAttachment, withPrettyNames, false);
    }

    protected Attachments getAttachmentsForDocument(Document doc, int start, int number, Boolean withPrettyNames)
    {
        Attachments attachments = this.objectFactory.createAttachments();

        RangeIterable<com.xpn.xwiki.api.Attachment> attachmentsRange =
            new RangeIterable<com.xpn.xwiki.api.Attachment>(doc.getAttachmentList(), start, number);
        for (com.xpn.xwiki.api.Attachment xwikiAttachment : attachmentsRange) {
            attachments.getAttachments().add(
                this.modelFactory.toRestAttachment(this.uriInfo.getBaseUri(), xwikiAttachment, withPrettyNames, false));
        }

        return attachments;
    }

    protected AttachmentInfo storeAndRetrieveAttachment(Document document, String attachmentName, InputStream content,
        Boolean withPrettyNames) throws XWikiException, AttachmentValidationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument previousDoc = xcontext.getDoc();
        try {
            // The context needs to be updated with the document where the attachment will be attached to be able to 
            // resolve the configuration when validating the attachment mimetype.
            xcontext.setDoc(document.getDocument());
            boolean alreadyExisting = document.getAttachment(attachmentName) != null;

            XWikiAttachment xwikiAttachment =
                createOrUpdateAttachment(new AttachmentReference(attachmentName, document.getDocumentReference()),
                    content);

            // The doc has been updated during the creation of the attachment, so we need to ensure we answer with the
            // updated version.
            Document updatedDoc = xwikiAttachment.getDoc().newDocument(xcontext);
            Attachment attachment = this.modelFactory.toRestAttachment(this.uriInfo.getBaseUri(),
                new com.xpn.xwiki.api.Attachment(updatedDoc, xwikiAttachment, this.xcontextProvider.get()),
                withPrettyNames,
                false);

            return new AttachmentInfo(attachment, alreadyExisting);
        } finally {
            // Restore the context to its initial value.
            xcontext.setDoc(previousDoc);
        }
    }

    protected XWikiAttachment createOrUpdateAttachment(AttachmentReference attachmentReference, InputStream content)
        throws XWikiException, AttachmentValidationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        // We clone the document because we're going to modify it and we shouldn't modify the cached instance.
        XWikiDocument document = xwiki.getDocument(attachmentReference, xcontext).clone();

        XWikiAttachment attachment;
        try {
            attachment = document.setAttachment(attachmentReference.getName(), content, xcontext);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MISC,
                String.format("Failed to create or update the attachment [%s].", attachmentReference), e);
        }

        try {
            this.componentManagerProvider.get().<AttachmentValidator>getInstance(AttachmentValidator.class)
                .validateAttachment(new XWikiAttachmentAccessWrapper(attachment, xcontext));
        } catch (ComponentLookupException e) {
            throw new XWikiException(
                String.format("Failed to instantiate a [%s] component.", AttachmentValidator.class.getName()), e);
        }

        // Set the document creator / author.
        if (document.isNew()) {
            document.getAuthors()
                .setCreator(this.documentReferenceUserReferenceResolver.resolve(xcontext.getUserReference()));
        }
        document.setAuthorReference(xcontext.getUserReference());

        // Calculate and store the attachment media type.
        attachment.resetMimeType(xcontext);

        // Remember the character encoding.
        attachment.setCharset(xcontext.getRequest().getCharacterEncoding());

        xwiki.saveDocument(document, xcontext);

        return attachment;
    }
}
