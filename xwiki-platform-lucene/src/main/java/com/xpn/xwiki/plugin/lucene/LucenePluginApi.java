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
package com.xpn.xwiki.plugin.lucene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * This plugin allows index based search in the contents of Wiki Pages and their attachments, as far
 * as any text can be extracted from them. Text can be extracted from OpenOffice Writer, MSWord,
 * PDF, XML/XHTML, plain text, etc. Text extraction is done with the help of various third party
 * libs such as Apache POI and PDFBox and some classes from the Daisy project.
 * <p>
 * This is the main interface for using the Lucene Plugin. It acts as a facade to the
 * {@link LucenePlugin} class.
 * </p>
 * 
 * @version $Id: $
 */
public class LucenePluginApi extends PluginApi
{
    private static final Log LOG = LogFactory.getLog(LucenePluginApi.class);

    public LucenePluginApi(LucenePlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    public LucenePlugin getLucenePlugin()
    {
        return (LucenePlugin) getProtectedPlugin();
    }

    /**
     * Starts a rebuild of the whole index.
     * 
     * @return Number of documents scheduled for indexing. -1 in case of errors
     */
    public int rebuildIndex()
    {
        int nbDocuments = -1;
        if (hasAdminRights()) {
            nbDocuments = getLucenePlugin().rebuildIndex(context);
        }

        return nbDocuments;
    }

    /**
     * Starts a rebuild of the whole index.
     * 
     * @param wiki
     * @param context
     * @return Number of documents scheduled for indexing. -1 in case of errors
     * @deprecated use rebuildIndex without context values
     */
    public int rebuildIndex(com.xpn.xwiki.api.XWiki wiki, Context context)
    {
        int nbDocuments = -1;
        if (wiki.hasAdminRights()) {
            nbDocuments = getLucenePlugin().rebuildIndex(context.getContext());
        }

        return nbDocuments;
    }

    /**
     * Searches the named indexes using the given query for documents in the given languages
     * 
     * @param query the query entered by the user
     * @param indexDirs comma separated list of lucene index directories to search in
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages
     * @param wiki reference to xwiki
     * @return {@link SearchResults}instance containing the results.
     * @deprecated call without XWiki object
     */
    public SearchResults getSearchResultsFromIndexes(String query, String indexDirs,
        String languages, com.xpn.xwiki.api.XWiki wiki)
    {
        try {
            return getLucenePlugin().getSearchResults(query, (String) null, indexDirs, languages,
                context);
        } catch (Exception e) {
            e.printStackTrace();
        } // end of try-catch

        return null;
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given
     * languages.
     * <p>
     * With virtual wikis enabled in your xwiki installation this will deliver results from all
     * virtuall wikis. For searching in a subset of your virtual wikis see {@link
     * #getSearchResults(String,String,String,com.xpn.xwiki.api.XWiki)}
     * </p>
     * 
     * @param query query entered by the user
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages. Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code>
     *            as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults}instance containing the results.
     * @deprecated call without XWiki object
     */
    public SearchResults getSearchResults(String query, String languages,
        com.xpn.xwiki.api.XWiki wiki)
    {
        return getSearchResultsFromIndexes(query, null, languages, wiki);
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given
     * languages belonging to one of the given virtual wikis.
     * <p>
     * Using this method only makes sense with virtual wikis enabled. Otherwise use
     * {@link #getSearchResults(String,String, com.xpn.xwiki.api.XWiki)} instead.
     * </p>
     * 
     * @param query query entered by the user
     * @param virtualWikiNames Names of the virtual wikis to search in. May be null for global
     *            search.
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages. Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code>
     *            as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults}instance containing the results.
     * @deprecated call without XWiki object
     */
    public SearchResults getSearchResults(String query, String virtualWikiNames,
        String languages, com.xpn.xwiki.api.XWiki wiki)
    {
        try {
            SearchResults retval =
                getLucenePlugin().getSearchResults(query, (String) null, virtualWikiNames,
                    languages, context);
            if (LOG.isDebugEnabled())
                LOG.debug("returning " + retval.getHitcount() + " results");
            return retval;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @return the number of documents in the queue.
     */
    public long getQueueSize()
    {
        return getLucenePlugin().getQueueSize();
    }

    /**
     * @return the number of documents in the second queue gave to Lucene.
     */
    public long getActiveQueueSize()
    {
        return getLucenePlugin().getActiveQueueSize();
    }

    /**
     * @return the number of documents Lucene index writer.
     */
    public long getLuceneDocCount()
    {
        return getLucenePlugin().getLuceneDocCount();
    }

    /**
     * Searches the named indexes using the given query for documents in the given languages
     * 
     * @param query the query entered by the user
     * @param indexDirs comma separated list of lucene index directories to search in
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages reference to xwiki
     * @return {@link SearchResults}instance containing the results.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String indexDirs,
        String languages)
    {
        try {
            return getLucenePlugin().getSearchResults(query, (String) null, indexDirs, languages,
                context);
        } catch (Exception e) {
            e.printStackTrace();
        } // end of try-catch

        return null;
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given
     * languages.
     * <p>
     * With virtual wikis enabled in your xwiki installation this will deliver results from all
     * virtuall wikis. For searching in a subset of your virtual wikis see
     * {@link #getSearchResults(String, String, String, com.xpn.xwiki.api.XWiki)}
     * </p>
     * 
     * @param query query entered by the user
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages. Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code>
     *            as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults}instance containing the results.
     */
    public SearchResults getSearchResults(String query, String languages)
    {
        return getSearchResultsFromIndexes(query, null, languages);
    }

    /**
     * Searches the named indexes using the given query for documents in the given languages
     * 
     * @param query the query entered by the user
     * @param sortField sortField to sort on
     * @param indexDirs comma separated list of lucene index directories to search in
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages reference to xwiki
     * @return {@link SearchResults}instance containing the results.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String sortField,
        String indexDirs, String languages)
    {
        try {
            return getLucenePlugin().getSearchResults(query, sortField, indexDirs, languages,
                context);
        } catch (Exception e) {
            e.printStackTrace();
        } // end of try-catch

        return null;
    }

    /**
     * Searches the named indexes using the given query for documents in the given languages
     * 
     * @param query the query entered by the user
     * @param sortField sortField(s) to sort on
     * @param indexDirs comma separated list of lucene index directories to search in
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages reference to xwiki
     * @return {@link SearchResults}instance containing the results.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String[] sortField,
        String indexDirs, String languages)
    {
        try {
            return getLucenePlugin().getSearchResults(query, sortField, indexDirs, languages,
                context);
        } catch (Exception e) {
            e.printStackTrace();
        } // end of try-catch

        return null;
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given
     * languages.
     * <p>
     * With virtual wikis enabled in your xwiki installation this will deliver results from all
     * virtuall wikis. For searching in a subset of your virtual wikis see
     * {@link #getSearchResults(String, String, String, com.xpn.xwiki.api.XWiki)}
     * </p>
     * 
     * @param query query entered by the user
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages. Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code>
     *            as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults}instance containing the results.
     */
    public SearchResults getSearchResults(String query, String sortField, String languages)
    {
        return getSearchResultsFromIndexes(query, sortField, null, languages);
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given
     * languages belonging to one of the given virtual wikis.
     * <p>
     * Using this method only makes sense with virtual wikis enabled. Otherwise use
     * {@link #getSearchResults(String, String, com.xpn.xwiki.api.XWiki)} instead.
     * </p>
     * 
     * @param query query entered by the user
     * @param sortField field to sort on
     * @param virtualWikiNames Names of the virtual wikis to search in. May be null for global
     *            search.
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages. Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code>
     *            as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults}instance containing the results.
     */
    public SearchResults getSearchResults(String query, String sortField,
        String virtualWikiNames, String languages)
    {
        try {
            SearchResults retval =
                getLucenePlugin().getSearchResults(query, sortField, virtualWikiNames, languages,
                    context);
            if (LOG.isDebugEnabled())
                LOG.debug("returning " + retval.getHitcount() + " results");
            return retval;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given
     * languages.
     * <p>
     * With virtual wikis enabled in your xwiki installation this will deliver results from all
     * virtuall wikis. For searching in a subset of your virtual wikis see
     * {@link #getSearchResults(String, String, String, com.xpn.xwiki.api.XWiki)}
     * </p>
     * 
     * @param query query entered by the user
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages. Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code>
     *            as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults}instance containing the results.
     */
    public SearchResults getSearchResults(String query, String[] sortField, String languages)
    {
        return getSearchResultsFromIndexes(query, sortField, null, languages);
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given
     * languages belonging to one of the given virtual wikis.
     * <p>
     * Using this method only makes sense with virtual wikis enabled. Otherwise use
     * {@link #getSearchResults(String, String, com.xpn.xwiki.api.XWiki)} instead.
     * </p>
     * 
     * @param query query entered by the user
     * @param sortField field to sort on
     * @param virtualWikiNames Names of the virtual wikis to search in. May be null for global
     *            search.
     * @param languages comma separated list of language codes to search in, may be null to search
     *            all languages. Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code>
     *            as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults}instance containing the results.
     */
    public SearchResults getSearchResults(String query, String[] sortField,
        String virtualWikiNames, String languages)
    {
        try {
            SearchResults retval =
                getLucenePlugin().getSearchResults(query, sortField, virtualWikiNames, languages,
                    context);
            if (LOG.isDebugEnabled())
                LOG.debug("returning " + retval.getHitcount() + " results");
            return retval;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
