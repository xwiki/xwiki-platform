/*
 * 
 * ===================================================================
 *
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created on 21.01.2005
 *
 */
package com.xpn.xwiki.plugin.lucene;

import org.apache.log4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Context;

/**
 * This is the main interface for using the Plugin. It basically acts as a
 * facade to the {@link LucenePlugin}class.
 * <p>
 * The methods intended for use in wiki pages are
 * </p>
 * <ul>
 * <li>{@link #rebuildIndex(com.xpn.xwiki.api.XWiki, Context)}for rebuilding
 * the whole index</li>
 * <li>{@link #getSearchResults(String, String, com.xpn.xwiki.api.XWiki)}for
 * searching the index</li>
 * <li>
 * {@link #getSearchResults(String, String, String, com.xpn.xwiki.api.XWiki)}
 * for searching specific virtual wikis</li>
 * <li>and
 * {@link #getSearchResultsFromIndexes(String, String, String, com.xpn.xwiki.api.XWiki)}
 * for searching other lucene indexes than thos configured in
 * <code>xwiki.cfg</code></li>
 * </ul>
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class LucenePluginApi extends Api
{
    private LucenePlugin        plugin;
    private static final Logger LOG = Logger.getLogger (LucenePluginApi.class);

    public LucenePluginApi (LucenePlugin plugin, XWikiContext context)
    {
        super (context);
        setPlugin (plugin);
    }

    /**
     * Starts a rebuild of the whole index.
     * @param wiki
     * @param context
     * @return Number of documents scheduled for indexing. -1 in case of errors
     */
    public int rebuildIndex (com.xpn.xwiki.api.XWiki wiki, Context context)
    {
        if (wiki.hasAdminRights ())
        {
            return getPlugin().rebuildIndex (wiki, context.getContext());
        }
        LOG.info ("access denied to rebuildIndex: insufficient rights");
        return -1;
    }

    /**
     * Searches the named indexes using the given query for documents in the
     * given languages
     * @param query
     *            the query entered by the user
     * @param indexDirs
     *            comma separated list of lucene index directories to search in
     * @param languages
     *            comma separated list of language codes to search in, may be
     *            null to search all languages
     * @param wiki
     *            reference to xwiki
     * @return {@link SearchResults}instance containing the results.
     */
    public SearchResults getSearchResultsFromIndexes (String query, String indexDirs, String languages,
                                                      com.xpn.xwiki.api.XWiki wiki)
    {
        try
        {
            return getPlugin ().getSearchResults (query, indexDirs, languages, wiki);
        } catch (Exception e)
        {
            e.printStackTrace ();
        } // end of try-catch
        return null;
    }

    /**
     * Searches the configured Indexes using the specified lucene query for
     * documents in the given languages.
     * <p>
     * With virtual wikis enabled in your xwiki installation this will deliver
     * results from all virtuall wikis. For searching in a subset of your
     * virtual wikis see
     * {@link #getSearchResults(String, String, String, com.xpn.xwiki.api.XWiki)}
     * </p>
     * @param query
     *            query entered by the user
     * @param languages
     *            comma separated list of language codes to search in, may be
     *            null to search all languages. Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific
     *            language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>,
     *            <code>de</code> as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults}instance containing the results.
     */
    public SearchResults getSearchResults (String query, String languages, com.xpn.xwiki.api.XWiki wiki)
    {
        return getSearchResultsFromIndexes (query, null, languages, wiki);
    }

    /**
     * Searches the configured Indexes using the specified lucene query for
     * documents in the given languages belonging to one of the given virtual
     * wikis.
     * <p>
     * Using this method only makes sense with virtual wikis enabled. Otherwise
     * use {@link #getSearchResults(String, String, com.xpn.xwiki.api.XWiki)}
     * instead.
     * </p>
     * @param query
     *            query entered by the user
     * @param virtualWikiNames
     *            Names of the virtual wikis to search in. May be null for
     *            global search.
     * @param languages
     *            comma separated list of language codes to search in, may be
     *            null to search all languages. Language codes can be:
     *            <ul>
     *            <li><code>default</code> for content having no specific
     *            language information</li>
     *            <li>lower case 2-letter language codes like <code>en</code>,
     *            <code>de</code> as used by xwiki</li>
     *            </ul>
     * @return a {@link SearchResults}instance containing the results.
     */
    public SearchResults getSearchResults (String query, String virtualWikiNames, String languages,
                                           com.xpn.xwiki.api.XWiki wiki)
    {
        try
        {
            SearchResults retval = getPlugin ().getSearchResults (query, virtualWikiNames, languages, wiki);
            if (LOG.isDebugEnabled ()) LOG.debug ("returning " + retval.getHitcount () + " results");
            return retval;
        } catch (Exception e)
        {
            e.printStackTrace ();
        }
        return null;
    }

    /*
    @return the number of documents in the queue
     */
    public long getQueueSize() {
        return plugin.getQueueSize();
    }


    /**
     * @param plugin
     *            plugin instance we are the facade for.
     */
    public void setPlugin (LucenePlugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * @return the plugin instance we are the facade for.
     */
    public LucenePlugin getPlugin ()
    {
        return this.plugin;
    }

}