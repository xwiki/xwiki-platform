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
package org.xwiki.wiki.template.internal;

import java.text.MessageFormat;
import java.util.Collection;

import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPlugin;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPluginApi;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XObjectDocument;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplication;

/**
 * Utility classe used to fill a newly created wiki.
 *
 * @since 5.3M1
 */
public class WikiCopy
{
    /**
     * Copy all documents from <code>sourceWiki</code> wiki to <code>targetWiki</code> wiki.
     * <p>
     * It also take care of ApplicationManager descriptors "documents to include" and "documents to link".
     * </p>
     *
     * @param sourceWiki the wiki from where to copy documents and get lists of "document to link" and "documents to
     *            copy".
     * @param targetWiki the wiki where to copy documents.
     * @param comment the comment to use when saving documents.
     * @param context the XWiki context.
     * @throws com.xpn.xwiki.XWikiException error when:
     *             <ul>
     *             <li>copying on of the source wiki to target wiki.</li>
     *             <li>or getting documents to include.</li>
     *             <li>or getting documents to link.</li>
     *             </ul>
     */
    public void copyWiki(String sourceWiki, String targetWiki, String comment, XWikiContext context)
            throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        // Copy all the wiki
        xwiki.copyWiki(sourceWiki, targetWiki, null, context);

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
        ApplicationManagerPluginApi appmanager =
                (ApplicationManagerPluginApi) context.getWiki().getPluginApi(ApplicationManagerPlugin.PLUGIN_NAME, context);

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
}
