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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.status.PopLevelProgressEvent;
import org.xwiki.job.event.status.PushLevelProgressEvent;
import org.xwiki.job.event.status.StepProgressEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPlugin;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPluginApi;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XObjectDocument;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;

/**
 * Default implementation for {@link WikiCopier}.
 * @version $Id$
 * @since 5.3M2
 */
@Component
public class DefaultWikiCopier implements WikiCopier
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private ObservationManager observationManager;

    @Override
    public void copyDocuments(String fromWikiId, String toWikiId, boolean withHistory) throws WikiManagerException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        try {
            Query query = queryManager.createQuery("select doc.fullName from Document as doc", Query.XWQL);
            query.setWiki(fromWikiId);
            List<String> documentFullnames = query.execute();

            observationManager.notify(new PushLevelProgressEvent(documentFullnames.size()), this);

            WikiReference fromWikiReference = new WikiReference(fromWikiId);
            for (String documentFullName : documentFullnames) {
                DocumentReference origDocReference = documentReferenceResolver.resolve(documentFullName,
                        fromWikiReference);
                DocumentReference newDocReference = new DocumentReference(toWikiId,
                        origDocReference.getLastSpaceReference().getName(), origDocReference.getName());
                xwiki.copyDocument(origDocReference, newDocReference, !withHistory, context);

                observationManager.notify(new StepProgressEvent(), this);
            }
            handleAppDescriptors(fromWikiId, toWikiId);

            observationManager.notify(new PopLevelProgressEvent(), this);
        } catch (QueryException e) {
            throw new WikiManagerException("Unable to get the source wiki documents.", e);
        } catch (XWikiException e) {
            throw new WikiManagerException("Failed to copy document.", e);
        }

    }

    @Override
    public void copyDeletedDocuments(String fromWikiId, String toWikiId) throws WikiManagerException
    {
        throw new WikiManagerException("This method is not implemented yet");
    }

    /**
     * Get the documents for which copied document content will be replace by an #includeInContext(SourceDocument) or
     * #includeTopic(SourceDocument) macro call.
     *
     * @param wiki the name of the wiki where to find the list of documents.
     * @param context the XWiki context.
     * @return a pair of list of documents names to include and list of documents names to link.
     * @throws XWikiException error when getting Applications descriptors where searched documents are listed.
     */
    private Collection<String>[] getDocsNames(String wiki, XWikiContext context) throws XWikiException
    {
        Collection<String>[] docsNames = new Collection[2];

        // Get applications manger
        ApplicationManagerPluginApi appmanager = (ApplicationManagerPluginApi) context.getWiki().getPluginApi(
                ApplicationManagerPlugin.PLUGIN_NAME, context);

        if (appmanager == null) {
            return null;
        }

        // //////////////////////////////////
        // Get documents to include or link

        String database = context.getDatabase();

        try {
            context.setDatabase(wiki);

            XWikiApplication rootApp = appmanager.getRootApplication();

            if (rootApp != null) {
                docsNames[0] = rootApp.getDocsNameToInclude(true);
                docsNames[1] = rootApp.getDocsNameToLink(true);
            } else {
                Collection<XWikiApplication> applications = appmanager.getApplicationDocumentList();
                docsNames[0] = XWikiApplication.getDocsNameToInclude(applications);
                docsNames[1] = XWikiApplication.getDocsNameToLink(applications);
            }
        } finally {
            context.setDatabase(database);
        }

        return docsNames;
    }

    /**
     * Take care of ApplicationManager descriptors "documents to include" and "documents to link".
     *
     * @param sourceWiki the wiki from where to copy documents and get lists of "document to link" and "documents to
     *            copy".
     * @param targetWiki targetWiki the wiki where to copy documents.
     * @throws XWikiException
     */
    private void handleAppDescriptors(String sourceWiki, String targetWiki) throws XWikiException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        String database = context.getDatabase();
        try {
            context.setDatabase(targetWiki);

            Collection<String>[] docsNames = getDocsNames(sourceWiki, context);

            if (docsNames != null) {
                Object[] includeFormatParams = new Object[] {sourceWiki, XObjectDocument.WIKI_SPACE_SEPARATOR, null};

                // Replace documents contents to include
                for (Object item : docsNames[0]) {
                    String docFullName = (String) item;
                    XWikiDocument targetDoc = xwiki.getDocument(docFullName, context);

                    includeFormatParams[2] = docFullName;
                    targetDoc.setContent(MessageFormat.format("#includeInContext(\"{0}{1}{2}\")", includeFormatParams));
                    targetDoc.setSyntax(Syntax.XWIKI_1_0);
                    xwiki.saveDocument(targetDoc, context);
                }

                // Replace documents contents to link
                for (Object item : docsNames[1]) {
                    String docFullName = (String) item;
                    XWikiDocument targetDoc = xwiki.getDocument(docFullName, context);

                    includeFormatParams[2] = docFullName;
                    targetDoc.setContent(MessageFormat.format("#includeTopic(\"{0}{1}{2}\")", includeFormatParams));
                    targetDoc.setSyntax(Syntax.XWIKI_1_0);
                    xwiki.saveDocument(targetDoc, context);
                }
            }
        } finally {
            context.setDatabase(database);
        }
    }
}
