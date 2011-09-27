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
package org.xwiki.wysiwyg.server.internal.wiki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPage;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wysiwyg.server.wiki.EntityReferenceConverter;
import org.xwiki.wysiwyg.server.wiki.LinkService;

/**
 * Put here only the methods that can be implemented without depending on the old XWiki core.
 * 
 * @version $Id$
 * @since 3.2
 */
public abstract class AbstractWikiService implements WikiService
{
    /**
     * Logger.
     */
    @Inject
    protected Logger logger;

    /**
     * The object used to convert between client and server entity reference.
     */
    protected final EntityReferenceConverter entityReferenceConverter = new EntityReferenceConverter();

    /**
     * The component used to create queries.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * The service used to create links.
     */
    @Inject
    private LinkService linkService;

    /**
     * The component used to access documents. This is temporary till XWiki model is moved into components.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component that protects us against cross site request forgery by using a secret token validation mechanism.
     * The secret token is added to the query string of the upload URL and then checked in the upload action when files
     * are uploaded.
     */
    @Inject
    private CSRFToken csrf;

    @Override
    public List<String> getPageNames(String wikiName, String spaceName)
    {
        String query = "where doc.space = ? order by doc.fullName asc";
        List<String> parameters = Collections.singletonList(spaceName);
        List<String> pagesNames = new ArrayList<String>();
        for (DocumentReference documentReference : searchDocumentReferences(wikiName, query, parameters, 0, 0)) {
            pagesNames.add(documentReference.getName());
        }
        return pagesNames;
    }

    @Override
    public List<WikiPage> getRecentlyModifiedPages(String wikiName, int start, int count)
    {
        String query = "where doc.author = ? order by doc.date desc";
        List<String> parameters = Collections.singletonList(getCurrentUserRelativeTo(wikiName));
        return getWikiPages(searchDocumentReferences(wikiName, query, parameters, start, count));
    }

    /**
     * @param wikiName the name of a wiki
     * @return the name of the current user, relative to the specified wiki
     */
    protected abstract String getCurrentUserRelativeTo(String wikiName);

    @Override
    public List<WikiPage> getMatchingPages(String wikiName, String keyword, int start, int count)
    {
        List<String> blackListedSpaces = getBlackListedSpaces();
        String notInBlackListedSpaces = "";
        if (blackListedSpaces.size() > 0) {
            notInBlackListedSpaces =
                "doc.web not in (?" + StringUtils.repeat(",?", blackListedSpaces.size() - 1) + ") and ";
        }
        String query =
            "where " + notInBlackListedSpaces + "(lower(doc.title) like '%'||?||'%' or"
                + " lower(doc.fullName) like '%'||?||'%')" + " order by doc.fullName asc";
        List<String> parameters = new ArrayList<String>(blackListedSpaces);
        // Add twice the keyword, once for the document title and once for the document name.
        parameters.add(keyword.toLowerCase());
        parameters.add(keyword.toLowerCase());

        return getWikiPages(searchDocumentReferences(wikiName, query, parameters, start, count));
    }

    /**
     * Helper function to retrieve the blacklisted spaces in this session, as they've been set in xwikivars.vm, when the
     * page edited with this WYSIWYG was loaded.
     * <p>
     * TODO: remove this when the public API will exclude them by default, or they'll be set in the configuration.
     * 
     * @return the list of blacklisted spaces from the session
     */
    protected abstract List<String> getBlackListedSpaces();

    /**
     * Helper function to create a list of {@link WikiPage}s from a list of document references.
     * 
     * @param documentReferences a list of document references
     * @return the list of {@link WikiPage}s corresponding to the given document references
     */
    protected abstract List<WikiPage> getWikiPages(List<DocumentReference> documentReferences);

    /**
     * Searches documents in the specified wiki.
     * 
     * @param wikiName the wiki where to search for documents
     * @param whereSql the WHERE clause of the select HQL query
     * @param parameters the query parameters
     * @param offset the position in the result set where to start
     * @param limit the maximum number of documents to retrieve
     * @return the documents from the specified wiki that match the given query
     */
    @SuppressWarnings("unchecked")
    private List<DocumentReference> searchDocumentReferences(String wikiName, String whereSql, List< ? > parameters,
        int offset, int limit)
    {
        try {
            String statement = "select distinct doc.space, doc.name from XWikiDocument as doc " + whereSql;
            Query query = queryManager.createQuery(statement, Query.HQL);
            query.setWiki(wikiName).setOffset(offset).setLimit(limit);
            query.bindValues((List<Object>) parameters);
            List<DocumentReference> documentReferences = new ArrayList<DocumentReference>();
            List<String[]> results = query.execute();
            for (String[] result : results) {
                documentReferences.add(new DocumentReference(result[1], new SpaceReference(result[0],
                    new WikiReference(wikiName))));
            }
            return documentReferences;
        } catch (QueryException e) {
            this.logger.error(e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to search XWiki pages.", e);
        }
    }

    @Override
    public EntityConfig getEntityConfig(org.xwiki.gwt.wysiwyg.client.wiki.EntityReference origin,
        ResourceReference destination)
    {
        return linkService.getEntityConfig(origin, destination);
    }

    @Override
    public ResourceReference parseLinkReference(String linkReference,
        org.xwiki.gwt.wysiwyg.client.wiki.EntityReference baseReference)
    {
        return linkService.parseLinkReference(linkReference, baseReference);
    }

    @Override
    public List<Attachment> getImageAttachments(WikiPageReference reference)
    {
        List<Attachment> imageAttachments = new ArrayList<Attachment>();
        List<Attachment> allAttachments = getAttachments(reference);
        for (Attachment attachment : allAttachments) {
            if (attachment.getMimeType().startsWith("image/")) {
                imageAttachments.add(attachment);
            }
        }
        return imageAttachments;
    }

    @Override
    public String getUploadURL(WikiPageReference reference)
    {
        String queryString = "form_token=" + csrf.getToken();
        return documentAccessBridge.getDocumentURL(entityReferenceConverter.convert(reference), "upload", queryString,
            null);
    }
}
