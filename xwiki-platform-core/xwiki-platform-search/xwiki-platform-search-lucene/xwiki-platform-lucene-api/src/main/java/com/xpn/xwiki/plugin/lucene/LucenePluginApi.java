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

import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * This plugin allows index based search in the contents of Wiki Pages and their attachments, as far as any text can be
 * extracted from them. Text can be extracted from OpenOffice Writer, MSWord, PDF, XML/XHTML, plain text, etc. Text
 * extraction is done with the help of various third party libs such as Apache POI and PDFBox and some classes from the
 * Daisy project.
 * <p>
 * This is the main interface for using the Lucene Plugin. It acts as a facade to the {@link LucenePlugin} class.
 * </p>
 * 
 * @version $Id$
 */
public class LucenePluginApi extends PluginApi<LucenePlugin>
{
    /** Logging helper. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LucenePluginApi.class);

    /**
     * Return value for {@link #rebuildIndex()} meaning that the caller does not have admin rights.
     */
    public static final int REBUILD_NOT_ALLOWED = -1;

    /**
     * Return value for {@link #rebuildIndex()} meaning that another rebuild is already in progress.
     */
    public static final int REBUILD_IN_PROGRESS = -2;

    public LucenePluginApi(LucenePlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Starts a rebuild of the whole index.
     * 
     * @return number of documents scheduled for indexing. -1 in case of errors
     */
    public int rebuildIndex()
    {
        if (hasAdminRights()) {
            Collection<String> wikis = null;

            String database = this.context.getWikiId();
            try {
                this.context.setWikiId(this.context.getMainXWiki());

                // if not farm administrator, the user does not have right to rebuild index of the whole farm
                if (!hasAdminRights()) {
                    wikis = Collections.singletonList(database);
                }
            } finally {
                this.context.setWikiId(database);
            }

            return getProtectedPlugin().startIndex(wikis, "", true, false, this.context);
        }

        return REBUILD_NOT_ALLOWED;
    }

    /**
     * Starts a rebuild of the whole index.
     * 
     * @param hqlFilter
     * @param clearIndex
     * @param onlyNew
     * @return number of documents scheduled for indexing. -1 in case of errors
     */
    public int startIndex(Collection<String> wikis, String hqlFilter, boolean clearIndex, boolean onlyNew)
    {
        if (hasAdminRights()) {
            // protected custom list of wikis
            Collection<String> secureWikis = wikis;
            String currentWiki = this.context.getWikiId();
            try {
                this.context.setWikiId(this.context.getMainXWiki());

                if (!hasAdminRights()) {
                    secureWikis = Collections.singletonList(currentWiki);
                }
            } finally {
                this.context.setWikiId(currentWiki);
            }

            // protected hql custom filter
            String secureHqlFilter = hasProgrammingRights() ? hqlFilter : null;

            return getProtectedPlugin().startIndex(secureWikis, secureHqlFilter, clearIndex, onlyNew, this.context);
        }

        return REBUILD_NOT_ALLOWED;
    }

    /**
     * Starts a rebuild of the whole index.
     * 
     * @param wiki
     * @param context
     * @return Number of documents scheduled for indexing. -1 in case of errors
     * @deprecated use rebuildIndex without context values
     */
    @Deprecated
    public int rebuildIndex(com.xpn.xwiki.api.XWiki wiki, Context context)
    {
        if (wiki.hasAdminRights()) {
            return getProtectedPlugin().rebuildIndex(context.getContext());
        }

        return REBUILD_NOT_ALLOWED;
    }

    /**
     * @return the remaining number of documents to index in the queue.
     */
    public long getQueueSize()
    {
        try {
            return getProtectedPlugin().getQueueSize();
        } catch (Exception e) {
            LOGGER.error("Failed to get the remaining number of documents to index in the queue", e);

            return 0;
        }
    }

    /**
     * @return the number of documents Lucene index writer.
     */
    public long getLuceneDocCount()
    {
        try {
            return getProtectedPlugin().getLuceneDocCount();
        } catch (Exception e) {
            LOGGER.error("Failed to get the number of documents Lucene index writer", e);

            return 0;
        }
    }

    /**
     * Searches the named indexes using the given query for documents in the given languages
     * 
     * @param query the query entered by the user
     * @param indexDirs comma separated list of lucene index directories to search in
     * @param languages comma separated list of language codes to search in, may be null to search all languages
     *            reference to xwiki
     * @return {@link SearchResults} instance containing the results.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String indexDirs, String languages)
    {
        try {
            return getProtectedPlugin().getSearchResults(query, (String) null, indexDirs, languages, this.context);
        } catch (Exception ex) {
            LOGGER.error("Failed to search: query=[{}], indexDirs=[{}], languages=[{}]", new Object[] {query,
            indexDirs, languages, ex});
        }

        return null;
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given languages.
     * <p>
     * With virtual wikis enabled in your xwiki installation this will deliver results from all virtuall wikis. For
     * searching in a subset of your virtual wikis see
     * {@link #getSearchResults(String, String, String, com.xpn.xwiki.api.XWiki)}
     * </p>
     * 
     * @param query query entered by the user
     * @param languages comma separated list of language codes to search in, may be null to search all languages.
     *            Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code> as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults} instance containing the results.
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
     * @param languages comma separated list of language codes to search in, may be null to search all languages
     *            reference to xwiki
     * @return {@link SearchResults} instance containing the results.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String sortField, String indexDirs, String languages)
    {
        try {
            return getProtectedPlugin().getSearchResults(query, sortField, indexDirs, languages, this.context);
        } catch (Exception ex) {
            LOGGER.error("Failed to search: query=[{}], sortField=[{}], indexDirs=[{}], languages=[{}]", new Object[] {
            query, sortField, indexDirs, languages, ex});
        }

        return null;
    }

    /**
     * Searches the named indexes using the given query for documents in the given languages
     * 
     * @param query the query entered by the user
     * @param sortField sortField(s) to sort on
     * @param indexDirs comma separated list of lucene index directories to search in
     * @param languages comma separated list of language codes to search in, may be null to search all languages
     *            reference to xwiki
     * @return {@link SearchResults} instance containing the results.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String[] sortField, String indexDirs,
        String languages)
    {
        try {
            return getProtectedPlugin().getSearchResults(query, sortField, indexDirs, languages, this.context);
        } catch (Exception ex) {
            LOGGER.error("Failed to search: query=[{}], sortField=[{}], indexDirs=[{}], languages=[{}]", new Object[] {
            query, sortField, indexDirs, languages, ex});
        }

        return null;
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given languages.
     * <p>
     * With virtual wikis enabled in your xwiki installation this will deliver results from all virtuall wikis. For
     * searching in a subset of your virtual wikis see
     * {@link #getSearchResults(String, String, String, com.xpn.xwiki.api.XWiki)}
     * </p>
     * 
     * @param query query entered by the user
     * @param sortField field to use to sort the results list (ex: date, author)
     * @param languages comma separated list of language codes to search in, may be null to search all languages.
     *            Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code> as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults} instance containing the results.
     */
    public SearchResults getSearchResults(String query, String sortField, String languages)
    {
        return getSearchResultsFromIndexes(query, sortField, null, languages);
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given languages belonging
     * to one of the given virtual wikis.
     * <p>
     * Using this method only makes sense with virtual wikis enabled. Otherwise use
     * {@link #getSearchResults(String, String, com.xpn.xwiki.api.XWiki)} instead.
     * </p>
     * 
     * @param query query entered by the user
     * @param sortField field to sort on
     * @param virtualWikiNames Names of the virtual wikis to search in. May be null for global search.
     * @param languages comma separated list of language codes to search in, may be null to search all languages.
     *            Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code> as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults} instance containing the results.
     */
    public SearchResults getSearchResults(String query, String sortField, String virtualWikiNames, String languages)
    {
        try {
            SearchResults retval =
                getProtectedPlugin().getSearchResults(query, sortField, virtualWikiNames, languages, this.context);
            LOGGER.debug("returning {} results", retval.getHitcount());
            return retval;
        } catch (Exception ex) {
            LOGGER.error("Failed to search: query=[{}], sortField=[{}], languages=[{}]", new Object[] {query,
            sortField, languages, ex});
        }

        return null;
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given languages.
     * <p>
     * With virtual wikis enabled in your xwiki installation this will deliver results from all virtuall wikis. For
     * searching in a subset of your virtual wikis see
     * {@link #getSearchResults(String, String, String, com.xpn.xwiki.api.XWiki)}
     * </p>
     * 
     * @param query query entered by the user
     * @param languages comma separated list of language codes to search in, may be null to search all languages.
     *            Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code> as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults} instance containing the results.
     */
    public SearchResults getSearchResults(String query, String[] sortField, String languages)
    {
        return getSearchResultsFromIndexes(query, sortField, null, languages);
    }

    /**
     * Searches the configured Indexes using the specified lucene query for documents in the given languages belonging
     * to one of the given virtual wikis.
     * <p>
     * Using this method only makes sense with virtual wikis enabled. Otherwise use
     * {@link #getSearchResults(String, String, com.xpn.xwiki.api.XWiki)} instead.
     * </p>
     * 
     * @param query query entered by the user
     * @param sortField field to sort on
     * @param virtualWikiNames Names of the virtual wikis to search in. May be null for global search.
     * @param languages comma separated list of language codes to search in, may be null to search all languages.
     *            Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>, <code>de</code> as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults} instance containing the results.
     */
    public SearchResults getSearchResults(String query, String[] sortField, String virtualWikiNames, String languages)
    {
        try {
            SearchResults retval =
                getProtectedPlugin().getSearchResults(query, sortField, virtualWikiNames, languages, this.context);
            LOGGER.debug("returning {} results", retval.getHitcount());
            return retval;
        } catch (Exception ex) {
            LOGGER.error("Failed to search: query=[{}], sortField=[{}], virtualWikiNames=[{}], languages=[{}]",
                new Object[] {query, sortField, virtualWikiNames, languages, ex});
        }

        return null;
    }

}
