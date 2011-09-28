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
package com.xpn.xwiki.plugin.globalsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.applicationmanager.core.api.XWikiExceptionApi;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;
import com.xpn.xwiki.plugin.globalsearch.tools.GlobalSearchQuery;
import com.xpn.xwiki.plugin.globalsearch.tools.GlobalSearchResult;

/**
 * API tool to be able to make and merge multi wikis search queries.
 * 
 * @version $Id$
 */
public class GlobalSearchPluginApi extends PluginApi<GlobalSearchPlugin>
{
    /**
     * Field name of the last error code inserted in context.
     */
    public static final String CONTEXT_LASTERRORCODE = "lasterrorcode";

    /**
     * Field name of the last API exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    /**
     * Logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(GlobalSearchPluginApi.class);

    /**
     * The plugin internationalization service.
     */
    private XWikiPluginMessageTool messageTool;

    /**
     * Tool to be able to make and merge multi wikis search queries.
     */
    private GlobalSearch search;

    /**
     * Create an instance of GlobalSearchPluginApi.
     * 
     * @param plugin the entry point of the Global Search plugin.
     * @param context the XWiki context.
     */
    public GlobalSearchPluginApi(GlobalSearchPlugin plugin, XWikiContext context)
    {
        super(plugin, context);

        // Message Tool
        Locale locale = (Locale) context.get("locale");
        this.messageTool = new GlobalSearchMessageTool(locale, plugin, context);
        context.put(GlobalSearchMessageTool.MESSAGETOOL_CONTEXT_KEY, this.messageTool);

        this.search = new GlobalSearch(this.messageTool);
    }

    /**
     * Log error and store details in the context.
     * 
     * @param errorMessage error message.
     * @param e the catched exception.
     */
    public void logError(String errorMessage, XWikiException e)
    {
        LOGGER.error(errorMessage, e);

        context.put(CONTEXT_LASTERRORCODE, Integer.valueOf(e.getCode()));
        context.put(CONTEXT_LASTEXCEPTION, new XWikiExceptionApi(e, context));
    }

    /**
     * Create a new instance of {@link GlobalSearchQuery} and return it.
     * 
     * @return a new instance of {@link GlobalSearchQuery} and return it.
     */
    public GlobalSearchQuery newQuery()
    {
        return new GlobalSearchQuery();
    }

    /**
     * Execute query in all provided wikis and return list containing all results. Compared to XWiki Platform search,
     * searchDocuments and searchDocumentsName it's potentially "time-consuming" since it issues one request per
     * provided wiki.
     * 
     * @param query the query parameters. The hql has some constraints:
     *            <ul>
     *            <li>"*" is not supported in SELECT clause.</li>
     *            <li>All ORDER BY fields has to be listed in SELECT clause.</li>
     *            </ul>
     * @return the search result as list of {@link GlobalSearchResult} containing all selected fields values.
     * @throws XWikiException error when executing query.
     */
    public Collection<GlobalSearchResult> search(GlobalSearchQuery query) throws XWikiException
    {
        Collection<GlobalSearchResult> results;

        try {
            if (hasProgrammingRights()) {
                results = this.search.search(query, this.context);
            } else {
                results = Collections.emptyList();
            }
        } catch (GlobalSearchException e) {
            logError(this.messageTool.get(GlobalSearchMessageTool.LOG_SEARCHDOCUMENTS), e);

            results = Collections.emptyList();
        }

        return results;
    }

    /**
     * Search wiki pages in all provided wikis and return list containing found {@link com.xpn.xwiki.api.Document}.
     * Compared to XWiki Platform search, searchDocuments and searchDocumentsName it's potentially "time-consuming"
     * since it issues one request per provided wiki.
     * 
     * @param query the query parameters. The hql has some constraints:
     *            <ul>
     *            <li>"*" is not supported in SELECT clause.</li>
     *            <li>All ORDER BY fields has to be listed in SELECT clause.</li>
     *            </ul>
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @return the found {@link Document}.
     * @throws XWikiException error when executing query.
     */
    public Collection<Document> searchDocuments(GlobalSearchQuery query, boolean distinctbylanguage)
        throws XWikiException
    {
        Collection<Document> documentList;

        try {
            Collection<XWikiDocument> documents =
                this.search.searchDocuments(query, distinctbylanguage, false, true, this.context);

            documentList = new ArrayList<Document>(documents.size());
            for (XWikiDocument doc : documents) {
                documentList.add(doc.newDocument(this.context));
            }
        } catch (GlobalSearchException e) {
            logError(this.messageTool.get(GlobalSearchMessageTool.LOG_SEARCHDOCUMENTS), e);

            documentList = Collections.emptyList();
        }

        return documentList;
    }

    /**
     * Search wiki pages in all provided wikis and return list containing found {@link com.xpn.xwiki.api.Document}.
     * Compared to XWiki Platform search, searchDocuments and searchDocumentsName it's potentially "time-consuming"
     * since it issues one request per provided wiki.
     * 
     * @param query the query parameters. The hql has some constraints:
     *            <ul>
     *            <li>"*" is not supported in SELECT clause.</li>
     *            <li>All ORDER BY fields has to be listed in SELECT clause.</li>
     *            </ul>
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param checkRight if true check for each found document if context's user has "view" rights for it.
     * @return the found {@link com.xpn.xwiki.api.Document}.
     * @throws XWikiException error when executing query.
     */
    public Collection<String> searchDocumentsNames(GlobalSearchQuery query, boolean distinctbylanguage,
        boolean checkRight) throws XWikiException
    {
        Collection<String> results;

        try {
            results = this.search.searchDocumentsNames(query, distinctbylanguage, false, checkRight, this.context);
        } catch (GlobalSearchException e) {
            logError(this.messageTool.get(GlobalSearchMessageTool.LOG_SEARCHDOCUMENTS), e);

            results = Collections.emptyList();
        }

        return results;
    }
}
