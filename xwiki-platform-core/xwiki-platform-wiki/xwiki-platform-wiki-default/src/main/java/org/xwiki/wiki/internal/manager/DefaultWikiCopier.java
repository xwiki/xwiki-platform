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
package org.xwiki.wiki.internal.manager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;

@Component
public class DefaultWikiCopier implements WikiCopier
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public void copyDocuments(String fromWikiId, String toWikiId, boolean withHistory) throws WikiManagerException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        try {
            Query query = queryManager.createQuery("select doc.fullName from Document as doc", Query.XWQL);
            query.setWiki(fromWikiId);
            List<String> documentFullnames = queryExecutor.execute(query);
            WikiReference fromWikiReference = new WikiReference(fromWikiId);
            WikiReference newWikiReference = new WikiReference(toWikiId);
            for (String documentFullName : documentFullnames) {
                DocumentReference origDocReference = documentReferenceResolver.resolve(documentFullName,
                        fromWikiReference);
                DocumentReference newDocReference = new DocumentReference(toWikiId,
                        origDocReference.getLastSpaceReference().getName(), origDocReference.getName());
                xwiki.copyDocument(origDocReference, newDocReference, !withHistory, context);
            }
        } catch (QueryException e) {
            throw new WikiManagerException("Unable to get the source wiki documents.", e);
        } catch (XWikiException e) {
            throw new WikiManagerException("Failed to copy document.", e);
        }

    }

    @Override
    public void copyDeletedDocuments(String fromWikiId, String toWikiId) throws WikiManagerException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();
        XWikiRecycleBinStoreInterface recycleBinStore = xwiki.getRecycleBinStore();

        // Note: I don't like this implementation,
        // but we don't have a clean API for this.
        // technical debt.
        try {
            Query query = queryManager.createQuery("select ddoc.id from XWikiDeletedDocument as ddoc", Query.XWQL);
            query.setWiki(fromWikiId);
            List<String> deletedDocumentIds = queryExecutor.execute(query);
            for (String deletedDocumentId : deletedDocumentIds) {
//
//                recycleBinStore.
//                recycleBinStore.saveToRecycleBin();
            }
        } catch (QueryException e) {
            throw new WikiManagerException("Unable to get the deleted documents.", e);
        }
    }
}
