/*
 * 
 * ===================================================================
 *
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
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
 *
 *
 * Created on 21.01.2005
 *
 */
package net.jkraemer.xwiki.plugins.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiActionRule;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

public class LucenePlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    public static final String  DOCTYPE_WIKIPAGE       = "wikipage";
    public static final String  DOCTYPE_ATTACHMENT     = "attachment";

    private static final Logger LOG                    = Logger.getLogger (LucenePlugin.class);
    private Analyzer            analyzer;
    private IndexUpdater        indexUpdater;
    private Thread              indexUpdaterThread;
    protected Properties        config;

    public static final String  PROP_INDEX_DIR         = "xwiki.plugins.lucene.indexdir";
    public static final String  PROP_ANALYZER          = "xwiki.plugins.lucene.analyzer";
    public static final String  PROP_INDEXING_INTERVAL = "xwiki.plugins.lucene.indexinterval";

    private static final String DEFAULT_ANALYZER       = "org.apache.lucene.analysis.de.GermanAnalyzer";
    private Searcher[]          searchers;
    private String              indexDirs;
    private IndexRebuilder      indexRebuilder;

    public LucenePlugin (String name, String className, XWikiContext context)
    {
        super (name, className, context);
        init (context);
    }

    /**
     * @see java.lang.Object#finalize()
     */
    protected void finalize () throws Throwable
    {
        if (indexUpdater != null) indexUpdater.doExit ();
        super.finalize ();
    }

    public synchronized int rebuildIndex (com.xpn.xwiki.api.XWiki wiki, XWikiContext context)
    {
        return indexRebuilder.rebuildIndex (wiki, context);
    }

    /**
     * Allows to search special named lucene indexes without having to configure
     * them in xwiki.cfg. Slower than {@link #getSearchResults(String)}since
     * new index searcher instances are created for every query.
     * @param query
     *            query string
     * @param myIndexDirs
     *            comma separated list of directories containing the lucene
     *            indexes to search.
     * @param languages
     *            comma separated list of language codes to search in, may be
     *            null to search all languages
     * @param context
     * @return
     * @throws Exception
     */
    public SearchResults getSearchResultsFromIndexes (String query, String myIndexDirs, String languages,
                                                      XWiki wiki) throws Exception
    {
        Searcher[] mySearchers = createSearchers (myIndexDirs);
        SearchResults retval = search (query, null, languages, mySearchers, wiki);
        closeSearchers (mySearchers);
        return retval;
    }

    /**
     * Searches all Indexes configured in xwiki.cfg (property
     * <code>xwiki.plugins.lucene.indexdir</code>)
     * @param query
     *            query String entered into a search form
     * @param context
     *            XWikiContext
     * @param virtualWikiNames
     *            Name of the virtual Wiki to search, global search when null
     * @param languages
     *            comma separated list of language codes to search in, may be
     *            null to search all languages
     * @return Searchresults as a collection of Maps
     * @throws Exception
     *             in case of error(s)
     */
    public SearchResults getSearchResults (String query, String virtualWikiNames, String languages, XWiki wiki)
                                                                                                               throws Exception
    {
        return search (query, virtualWikiNames, languages, this.searchers, wiki);
    }

    /**
     * @param query
     * @param indexes
     * @param virtualWikiNames
     *            comma separated list of virtual wiki names to search in, may
     *            be null to search all virtual wikis
     * @param languages
     *            comma separated list of language codes to search in, may be
     *            null to search all languages
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private SearchResults search (String query, String virtualWikiNames, String languages,
                                  Searcher[] indexes, XWiki wiki) throws IOException, ParseException
    {
        MultiSearcher searcher = new MultiSearcher (indexes);
        Query q = buildQuery (query, virtualWikiNames, languages);
        Hits hits = searcher.search (q);
        final int hitcount = hits.length ();
        if (LOG.isDebugEnabled ()) LOG.debug ("query " + q + " returned " + hitcount + " hits");
        return new SearchResults (hits, wiki);
    }

    /**
     * @param query
     * @param virtualWikiNames
     *            comma separated list of virtual wiki names
     * @param languages
     *            comma separated list of language codes to search in, may be
     *            null to search all languages
     * @throws ParseException
     */
    private Query buildQuery (String query, String virtualWikiNames, String languages) throws ParseException
    {
        // build a query like this: <user query string> AND <wikiNamesQuery> AND
        // <languageQuery>
        BooleanQuery bQuery = new BooleanQuery ();
        Query parsedQuery = QueryParser.parse (query, IndexFields.FULLTEXT, analyzer);
        bQuery.add (parsedQuery, true, false);
        if (virtualWikiNames != null && virtualWikiNames.length () > 0)
        {
            bQuery.add (buildOredTermQuery (virtualWikiNames, IndexFields.DOCUMENT_WIKI), true, false);
        }
        if (languages != null && languages.length () > 0)
        {
            bQuery.add (buildOredTermQuery (languages, IndexFields.DOCUMENT_LANGUAGE), true, false);
        }
        return bQuery;
    }

    /**
     * @param values
     *            comma separated list of values to look for
     * @return A query returning documents matching one of the given values in
     *         the given field
     */
    private Query buildOredTermQuery (final String values, final String fieldname)
    {
        String[] valueArray = values.split ("\\,");
        if (valueArray.length > 1)
        {
            // build a query like this: <valueArray[0]> OR <valueArray[1]> OR ...
            BooleanQuery orQuery = new BooleanQuery ();
            for (int i = 0; i < valueArray.length; i++)
            {
                orQuery.add (new TermQuery (new Term (fieldname, valueArray[i].trim ())), false, false);
            }
            return orQuery;
        }
        // exactly one value, no OR'ed Terms necessary
        return new TermQuery (new Term (fieldname, valueArray[0]));
    }

    public synchronized void init (XWikiContext context)
    {
        super.init (context);
        if (LOG.isDebugEnabled ()) LOG.debug ("lucene plugin: in init");
        config = context.getWiki ().getConfig ();
        try
        {
            analyzer = (Analyzer) Class.forName (config.getProperty (PROP_ANALYZER, DEFAULT_ANALYZER))
                    .newInstance ();
        } catch (Exception e)
        {
            e.printStackTrace ();
            LOG.error ("error instantiating analyzer : ", e);
            LOG.warn ("using default analyzer class: " + DEFAULT_ANALYZER);
            try
            {
                analyzer = (Analyzer) Class.forName (DEFAULT_ANALYZER).newInstance ();
            } catch (Exception e1)
            {
                e1.printStackTrace ();
                throw new RuntimeException ("instantiation of default analyzer " + DEFAULT_ANALYZER
                        + " failed", e1);
            }
        }
        this.indexDirs = config.getProperty (PROP_INDEX_DIR);
        openSearchers ();
        indexUpdater = new IndexUpdater ();
        indexUpdater.setAnalyzer (analyzer);
        indexUpdater.init (config, this, context.getWiki ());
        indexUpdaterThread = new Thread (indexUpdater);
        indexUpdaterThread.start ();
        indexRebuilder = new IndexRebuilder ();
        indexRebuilder.setIndexUpdater (indexUpdater);
        context.getWiki ().getNotificationManager ().addGeneralRule (new DocChangeRule (indexUpdater));
        context.getWiki ().getNotificationManager ().addGeneralRule (new XWikiActionRule (indexUpdater));
        LOG.info ("lucene plugin initialized.");
    }

    public String getName ()
    {
        return "lucene";
    }

    public Api getPluginApi (XWikiPluginInterface plugin, XWikiContext context)
    {
        return new LucenePluginApi ((LucenePlugin) plugin, context);
    }

    /**
     * Creates an array of Searchers for a number of lucene indexes.
     * @param indexDirs
     *            Comma separated list of Lucene index directories to create
     *            searchers for.
     * @return Array of searchers
     * @throws Exception
     */
    private static Searcher[] createSearchers (String indexDirs) throws Exception
    {
        String[] dirs = StringUtils.split (indexDirs, " ,");
        List searchersList = new ArrayList ();
        for (int i = 0; i < dirs.length; i++)
        {
            try
            {
                IndexReader reader = IndexReader.open (dirs[i]);
                searchersList.add (new IndexSearcher (reader));
            } catch (IOException e)
            {
                LOG.error ("cannot open index " + dirs[i], e);
                e.printStackTrace ();
            }
        }
        return (Searcher[]) searchersList.toArray (new Searcher[searchersList.size ()]);
    }

    /**
     * Opens the searchers for the configured index Dirs after closing any
     * already existing ones.
     */
    protected synchronized void openSearchers ()
    {
        try
        {
            closeSearchers (this.searchers);
            this.searchers = createSearchers (indexDirs);
        } catch (Exception e1)
        {
            LOG.error ("error opening searchers for index dirs " + config.getProperty (PROP_INDEX_DIR), e1);
            throw new RuntimeException ("error opening searchers for index dirs "
                    + config.getProperty (PROP_INDEX_DIR), e1);
        }
    }

    /**
     * @throws IOException
     */
    protected static void closeSearchers (Searcher[] searchers) throws IOException
    {
        if (searchers != null)
        {
            for (int i = 0; i < searchers.length; i++)
            {
                if (searchers[i] != null) searchers[i].close ();
            }
        }
    }

}
