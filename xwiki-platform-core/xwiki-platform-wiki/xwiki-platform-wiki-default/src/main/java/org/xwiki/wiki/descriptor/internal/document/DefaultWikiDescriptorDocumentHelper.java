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
package org.xwiki.wiki.descriptor.internal.document;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Component to load and resolve wiki descriptor documents.
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Singleton
public class DefaultWikiDescriptorDocumentHelper implements WikiDescriptorDocumentHelper
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiManager wikiManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public DocumentReference getDocumentReferenceFromId(String wikiId)
    {
        return new DocumentReference(wikiManager.getMainWikiId(),
                XWiki.SYSTEM_SPACE, String.format("XWikiServer%s", StringUtils.capitalize(wikiId)));
    }

    @Override
    public XWikiDocument getDocumentFromWikiId(String wikiId) throws WikiManagerException
    {
        return getDocument(getDocumentReferenceFromId(wikiId));
    }

    @Override
    public DocumentReference findXWikiServerClassDocumentReference(String wikiAlias) throws WikiManagerException
    {
        DocumentReference result = null;

        try {
            Query query = this.queryManager.createQuery(
                    "where doc.object(XWiki.XWikiServerClass).server = :wikiAlias and doc.name like 'XWikiServer%'",
                    Query.XWQL);
            query.bindValue("wikiAlias", wikiAlias);
            query.setWiki(wikiManager.getMainWikiId());
            List<String> documentNames = query.execute();

            // Resolve the document name into a references
            if (documentNames != null && !documentNames.isEmpty()) {
                result = documentReferenceResolver.resolve(documentNames.get(0));
            }
        } catch (QueryException e) {
            throw new WikiManagerException(String.format(
                    "Failed to locate XWiki.XWikiServerClass document for wiki alias [%s]", wikiAlias), e);
        }

        return result;
    }

    @Override
    public XWikiDocument findXWikiServerClassDocument(String wikiAlias) throws WikiManagerException
    {
        XWikiDocument document = null;

        DocumentReference documentReference = findXWikiServerClassDocumentReference(wikiAlias);
        if (documentReference != null) {
            document = getDocument(documentReference);
        }

        return document;
    }

    @Override
    public List<XWikiDocument> getAllXWikiServerClassDocument() throws WikiManagerException
    {
        List<XWikiDocument> result = new ArrayList<XWikiDocument>();

        try {
            Query query = this.queryManager.createQuery(
                    "from doc.object(XWiki.XWikiServerClass) as descriptor where doc.name like 'XWikiServer%'",
                    Query.XWQL);
            query.setWiki(wikiManager.getMainWikiId());
            List<String> documentNames = query.execute();

            if (documentNames != null && !documentNames.isEmpty()) {
                for (String documentName : documentNames) {
                    // Resolve the document names into references and for each one extract the Wiki
                    result.add(getDocument(documentReferenceResolver.resolve(documentName)));
                }
            }
        } catch (Exception e) {
            throw new WikiManagerException("Failed to locate XWiki.XWikiServerClass documents", e);
        }

        return result;
    }

    private XWikiDocument getDocument(DocumentReference reference) throws WikiManagerException
    {
        XWikiContext context = xcontextProvider.get();
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        try {
            return xwiki.getDocument(reference, context);
        } catch (XWikiException e) {
            throw new WikiManagerException(String.format(
                    "Failed to get document [%s] containing a XWiki.XWikiServerClass object", reference), e);
        }
    }
}
