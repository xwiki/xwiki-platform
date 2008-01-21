package com.xpn.xwiki.plugin.globalsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.applicationmanager.core.api.XWikiExceptionApi;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;
import com.xpn.xwiki.plugin.globalsearch.tools.GlobalSearchQuery;

/**
 * API tool to be able to make and merge multi wikis search queries.
 * 
 * @version $Id: $
 */
public class GlobalSearchPluginApi extends PluginApi
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
    protected static final Log LOG = LogFactory.getLog(GlobalSearchPluginApi.class);

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
        context.put(GlobalSearch.MESSAGETOOL_CONTEXT_KEY, this.messageTool);

        this.search = new GlobalSearch(messageTool);
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
     * Execute query in all provided wikis and return list containing all results. Compared to XWiki
     * Platform search, searchDocuments and searchDocumentsName it's potentially "time-consuming"
     * since it issues one request per provided wiki.
     * 
     * @param query the query parameters. The hql has some constraints:
     *            <ul>
     *            <li>"*" is not supported in SELECT clause.</li>
     *            <li>All ORDER BY fields has to be listed in SELECT clause.</li>
     *            </ul>
     * @return the search result as list of
     *         {@link com.xpn.xwiki.plugin.globalsearch.tools.GlobalSearchResult} containing all
     *         selected fields values.
     * @throws XWikiException error when executing query.
     */
    public Collection search(GlobalSearchQuery query) throws XWikiException
    {
        Collection results = Collections.EMPTY_LIST;

        try {
            if (hasProgrammingRights()) {
                results = search.search(query, this.context);
            }
        } catch (GlobalSearchException e) {
            LOG.error(messageTool.get(GlobalSearchMessageTool.LOG_SEARCHDOCUMENTS), e);

            this.context.put(CONTEXT_LASTERRORCODE, new Integer(e.getCode()));
            this.context.put(CONTEXT_LASTEXCEPTION, new XWikiExceptionApi(e, this.context));
        }

        return results;
    }

    /**
     * Search wiki pages in all provided wikis and return list containing found
     * {@link com.xpn.xwiki.api.Document}. Compared to XWiki Platform search, searchDocuments and
     * searchDocumentsName it's potentially "time-consuming" since it issues one request per
     * provided wiki.
     * 
     * @param query the query parameters. The hql has some constraints:
     *            <ul>
     *            <li>"*" is not supported in SELECT clause.</li>
     *            <li>All ORDER BY fields has to be listed in SELECT clause.</li>
     *            </ul>
     * @param distinctbylanguage when a document has multiple version for each language it is
     *            returned as one document a language.
     * @return the found {@link com.xpn.xwiki.api.Document}.
     * @throws XWikiException error when executing query.
     */
    public Collection searchDocuments(GlobalSearchQuery query, boolean distinctbylanguage)
        throws XWikiException
    {
        Collection documentList = Collections.EMPTY_LIST;

        try {
            Collection documents =
                search.searchDocuments(query, distinctbylanguage, false, true, this.context);

            documentList = new ArrayList(documents.size());
            for (Iterator it = documents.iterator(); it.hasNext();) {
                documentList.add(((XWikiDocument) it.next()).newDocument(context));
            }
        } catch (GlobalSearchException e) {
            LOG.error(messageTool.get(GlobalSearchMessageTool.LOG_SEARCHDOCUMENTS), e);

            this.context.put(CONTEXT_LASTERRORCODE, new Integer(e.getCode()));
            this.context.put(CONTEXT_LASTEXCEPTION, new XWikiExceptionApi(e, this.context));
        }

        return documentList;
    }

    /**
     * Search wiki pages in all provided wikis and return list containing found
     * {@link com.xpn.xwiki.api.Document}. Compared to XWiki Platform search, searchDocuments and
     * searchDocumentsName it's potentially "time-consuming" since it issues one request per
     * provided wiki.
     * 
     * @param query the query parameters. The hql has some constraints:
     *            <ul>
     *            <li>"*" is not supported in SELECT clause.</li>
     *            <li>All ORDER BY fields has to be listed in SELECT clause.</li>
     *            </ul>
     * @param distinctbylanguage when a document has multiple version for each language it is
     *            returned as one document a language.
     * @param checkRight if true check for each found document if context's user has "view" rights
     *            for it.
     * @return the found {@link com.xpn.xwiki.api.Document}.
     * @throws XWikiException error when executing query.
     */
    public Collection searchDocumentsNames(GlobalSearchQuery query, boolean distinctbylanguage,
        boolean checkRight) throws XWikiException
    {
        Collection results = Collections.EMPTY_LIST;

        try {
            results =
                search.searchDocumentsNames(query, distinctbylanguage, false, checkRight,
                    this.context);
        } catch (GlobalSearchException e) {
            LOG.error(messageTool.get(GlobalSearchMessageTool.LOG_SEARCHDOCUMENTS), e);

            this.context.put(CONTEXT_LASTERRORCODE, new Integer(e.getCode()));
            this.context.put(CONTEXT_LASTEXCEPTION, new XWikiExceptionApi(e, this.context));
        }

        return results;
    }
}
