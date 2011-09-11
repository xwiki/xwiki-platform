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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.xwiki.context.Execution;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.Utils;

/**
 * A plugin offering support for advanced searches using Lucene, a high performance, open source search engine. It uses
 * an {@link IndexUpdater} to monitor and submit wiki pages for indexing to the Lucene engine, and offers simple methods
 * for searching documents, with the possiblity to sort by one or several document fields (besides the default sort by
 * relevance), filter by one or several languages, and search in one, several or all virtual wikis.
 * 
 * @version $Id$
 */
public class LucenePlugin extends XWikiDefaultPlugin
{
    private static final Log LOG = LogFactory.getLog(LucenePlugin.class);

    public static final String DOCTYPE_WIKIPAGE = "wikipage";

    public static final String DOCTYPE_ATTACHMENT = "attachment";

    public static final String PROP_INDEX_DIR = "xwiki.plugins.lucene.indexdir";

    public static final String PROP_ANALYZER = "xwiki.plugins.lucene.analyzer";

    public static final String PROP_INDEXING_INTERVAL = "xwiki.plugins.lucene.indexinterval";

    public static final String PROP_MAX_QUEUE_SIZE = "xwiki.plugins.lucene.maxQueueSize";

    private static final String DEFAULT_ANALYZER = "org.apache.lucene.analysis.standard.StandardAnalyzer";

    /**
     * The Lucene text analyzer, can be configured in <tt>xwiki.cfg</tt> using the key {@link #PROP_ANALYZER} (
     * <tt>xwiki.plugins.lucene.analyzer</tt>).
     */
    private Analyzer analyzer;

    /**
     * Lucene index updater. Listens for changes and indexes wiki documents in a separate thread.
     */
    private IndexUpdater indexUpdater;

    /**
     * The thread running the index updater.
     */
    private Thread indexUpdaterThread;

    /**
     * List of Lucene indexes used for searching. By default there is only one such index for all the wiki. One searches
     * is created for each entry in {@link #indexDirs}.
     */
    private Searcher[] searchers;

    /**
     * Comma separated list of directories holding Lucene index data. The first such directory is used by the internal
     * indexer. Can be configured in <tt>xwiki.cfg</tt> using the key {@link #PROP_INDEX_DIR} (
     * <tt>xwiki.plugins.lucene.indexdir</tt>). If no directory is configured, then a subdirectory <tt>lucene</tt> in
     * the application's work directory is used.
     */
    private String indexDirs;

    private IndexRebuilder indexRebuilder;

    public LucenePlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable
    {
        LOG.error("Lucene plugin will exit !");

        if (this.indexUpdater != null) {
            this.indexUpdater.doExit();
        }

        super.finalize();
    }

    public int rebuildIndex(XWikiContext context)
    {
        return this.indexRebuilder.startRebuildIndex(context);
    }

    public int startIndex(Collection<String> wikis, String hqlFilter, boolean clearIndex, boolean onlyNew,
        XWikiContext context)
    {
        return this.indexRebuilder.startIndex(wikis, hqlFilter, clearIndex, onlyNew, context);
    }

    /**
     * Allows to search special named lucene indexes without having to configure them in <tt>xwiki.cfg</tt>. Slower than
     * {@link #getSearchResults(String, String, String, String, XWikiContext)} since new index searcher instances are
     * created for every query.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param myIndexDirs Comma separated list of directories containing the lucene indexes to search.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt> or empty to search all
     *            languages.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws Exception If the index directories cannot be read, or the query is invalid.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String myIndexDirs, String languages,
        XWikiContext context) throws Exception
    {
        Searcher[] mySearchers = createSearchers(myIndexDirs, context);
        SearchResults retval = search(query, (String) null, null, languages, mySearchers, context);
        closeSearchers(mySearchers);

        return retval;
    }

    /**
     * Allows to search special named lucene indexes without having to configure them in xwiki.cfg. Slower than
     * {@link #getSearchResults}since new index searcher instances are created for every query.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortFields A list of fields to sort results by. For each field, if the name starts with '-', then that
     *            field (excluding the -) is used for reverse sorting. If <tt>null</tt> or empty, sort by hit score.
     * @param myIndexDirs Comma separated list of directories containing the lucene indexes to search.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt> or empty to search all
     *            languages.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws Exception If the index directories cannot be read, or the query is invalid.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String[] sortFields, String myIndexDirs,
        String languages, XWikiContext context) throws Exception
    {
        Searcher[] mySearchers = createSearchers(myIndexDirs, context);
        SearchResults retval = search(query, sortFields, null, languages, mySearchers, context);
        closeSearchers(mySearchers);

        return retval;
    }

    /**
     * Allows to search special named lucene indexes without having to configure them in <tt>xwiki.cfg</tt>. Slower than
     * {@link #getSearchResults(String, String, String, String, XWikiContext)} since new index searcher instances are
     * created for every query.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortField The name of a field to sort results by. If the name starts with '-', then the field (excluding
     *            the -) is used for reverse sorting. If <tt>null</tt> or empty, sort by hit score.
     * @param myIndexDirs Comma separated list of directories containing the lucene indexes to search.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt> or empty to search all
     *            languages.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws Exception If the index directories cannot be read, or the query is invalid.
     */
    public SearchResults getSearchResultsFromIndexes(String query, String sortField, String myIndexDirs,
        String languages, XWikiContext context) throws Exception
    {
        Searcher[] mySearchers = createSearchers(myIndexDirs, context);
        SearchResults retval = search(query, sortField, null, languages, mySearchers, context);
        closeSearchers(mySearchers);

        return retval;
    }

    /**
     * Searches all Indexes configured in <tt>xwiki.cfg</tt> (property <code>xwiki.plugins.lucene.indexdir</code>).
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortField The name of a field to sort results by. If the name starts with '-', then the field (excluding
     *            the -) is used for reverse sorting. If <tt>null</tt> or empty, sort by hit score.
     * @param virtualWikiNames Comma separated list of virtual wiki names to search in, may be <tt>null</tt> to search
     *            all virtual wikis.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt> or empty to search all
     *            languages.
     * @return The list of search results.
     * @param context The context of the request.
     * @throws Exception If the index directories cannot be read, or the query is invalid.
     */
    public SearchResults getSearchResults(String query, String sortField, String virtualWikiNames, String languages,
        XWikiContext context) throws Exception
    {
        return search(query, sortField, virtualWikiNames, languages, this.searchers, context);
    }

    /**
     * Searches all Indexes configured in <tt>xwiki.cfg</tt> (property <code>xwiki.plugins.lucene.indexdir</code>).
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortField The name of a field to sort results by. If the name starts with '-', then the field (excluding
     *            the -) is used for reverse sorting. If <tt>null</tt> or empty, sort by hit score.
     * @param virtualWikiNames Comma separated list of virtual wiki names to search in, may be <tt>null</tt> to search
     *            all virtual wikis.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt> or empty to search all
     *            languages.
     * @return The list of search results.
     * @param context The context of the request.
     * @throws Exception If the index directories cannot be read, or the query is invalid.
     */
    public SearchResults getSearchResults(String query, String[] sortField, String virtualWikiNames, String languages,
        XWikiContext context) throws Exception
    {
        return search(query, sortField, virtualWikiNames, languages, this.searchers, context);
    }

    /**
     * Creates and submits a query to the Lucene engine.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortField The name of a field to sort results by. If the name starts with '-', then the field (excluding
     *            the -) is used for reverse sorting. If <tt>null</tt> or empty, sort by hit score.
     * @param virtualWikiNames Comma separated list of virtual wiki names to search in, may be <tt>null</tt> to search
     *            all virtual wikis.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt> or empty to search all
     *            languages.
     * @param indexes List of Lucene indexes (searchers) to search.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws IOException If the Lucene searchers encounter a problem reading the indexes.
     * @throws ParseException If the query is not valid.
     */
    private SearchResults search(String query, String sortField, String virtualWikiNames, String languages,
        Searcher[] indexes, XWikiContext context) throws IOException, ParseException
    {
        SortField sort = getSortField(sortField);

        // Perform the actual search
        return search(query, (sort != null) ? new Sort(sort) : null, virtualWikiNames, languages, indexes, context);
    }

    /**
     * Creates and submits a query to the Lucene engine.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sortFields A list of fields to sort results by. For each field, if the name starts with '-', then that
     *            field (excluding the -) is used for reverse sorting. If <tt>null</tt> or empty, sort by hit score.
     * @param virtualWikiNames Comma separated list of virtual wiki names to search in, may be <tt>null</tt> to search
     *            all virtual wikis.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt> or empty to search all
     *            languages.
     * @param indexes List of Lucene indexes (searchers) to search.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws IOException If the Lucene searchers encounter a problem reading the indexes.
     * @throws ParseException If the query is not valid.
     */
    private SearchResults search(String query, String[] sortFields, String virtualWikiNames, String languages,
        Searcher[] indexes, XWikiContext context) throws IOException, ParseException
    {
        // Turn the sorting field names into SortField objects.
        SortField[] sorts = null;
        if (sortFields != null && sortFields.length > 0) {
            sorts = new SortField[sortFields.length];
            for (int i = 0; i < sortFields.length; ++i) {
                sorts[i] = getSortField(sortFields[i]);
            }
            // Remove any null values from the list.
            int prevLength = -1;
            while (prevLength != sorts.length) {
                prevLength = sorts.length;
                sorts = (SortField[]) ArrayUtils.removeElement(sorts, null);
            }
        }

        // Perform the actual search
        return search(query, (sorts != null) ? new Sort(sorts) : null, virtualWikiNames, languages, indexes, context);
    }

    /**
     * Creates and submits a query to the Lucene engine.
     * 
     * @param query The base query, using the query engine supported by Lucene.
     * @param sort A Lucene sort object, can contain one or more sort criterias. If <tt>null</tt>, sort by hit score.
     * @param virtualWikiNames Comma separated list of virtual wiki names to search in, may be <tt>null</tt> to search
     *            all virtual wikis.
     * @param languages Comma separated list of language codes to search in, may be <tt>null</tt> or empty to search all
     *            languages.
     * @param indexes List of Lucene indexes (searchers) to search.
     * @param context The context of the request.
     * @return The list of search results.
     * @throws IOException If the Lucene searchers encounter a problem reading the indexes.
     * @throws ParseException If the query is not valid.
     */
    private SearchResults search(String query, Sort sort, String virtualWikiNames, String languages,
        Searcher[] indexes, XWikiContext context) throws IOException, ParseException
    {
        MultiSearcher searcher = new MultiSearcher(indexes);

        // Enhance the base query with wiki names and languages.
        Query q = buildQuery(query, virtualWikiNames, languages);

        // Perform the actual search
        Hits hits = (sort == null) ? searcher.search(q) : searcher.search(q, sort);
        if (LOG.isDebugEnabled()) {
            LOG.debug("query " + q + " returned " + hits.length() + " hits");
        }

        // Transform the raw Lucene search results into XWiki-aware results
        return new SearchResults(hits, new com.xpn.xwiki.api.XWiki(context.getWiki(), context), context);
    }

    /**
     * Create a {@link SortField} corresponding to the field name. If the field name starts with '-', then the field
     * (excluding the leading -) will be used for reverse sorting.
     * 
     * @param sortField The name of the field to sort by. If <tt>null</tt>, return a <tt>null</tt> SortField. If starts
     *            with '-', then return a SortField that does a reverse sort on the field.
     * @return A SortFiled that sorts on the given field, or <tt>null</tt>.
     */
    private SortField getSortField(String sortField)
    {
        SortField sort = null;
        if (!StringUtils.isEmpty(sortField)) {
            if (sortField.startsWith("-")) {
                sort = new SortField(sortField.substring(1), true);
            } else {
                sort = new SortField(sortField);
            }
        }

        return sort;
    }

    /**
     * @param query
     * @param virtualWikiNames comma separated list of virtual wiki names
     * @param languages comma separated list of language codes to search in, may be null to search all languages
     */
    private Query buildQuery(String query, String virtualWikiNames, String languages) throws ParseException
    {
        // build a query like this: <user query string> AND <wikiNamesQuery> AND
        // <languageQuery>
        BooleanQuery bQuery = new BooleanQuery();
        Query parsedQuery = null;

        // for object search
        if (query.startsWith("PROP ")) {
            String property = query.substring(0, query.indexOf(":"));
            query = query.substring(query.indexOf(":") + 1, query.length());
            QueryParser qp = new QueryParser(Version.LUCENE_29, property, analyzer);
            parsedQuery = qp.parse(query);
            bQuery.add(parsedQuery, BooleanClause.Occur.MUST);
        } else if (query.startsWith("MULTI ")) {
            // for fulltext search
            List<String> fieldList = IndexUpdater.fields;
            String[] fields = fieldList.toArray(new String[fieldList.size()]);
            BooleanClause.Occur[] flags = new BooleanClause.Occur[fields.length];
            for (int i = 0; i < flags.length; i++) {
                flags[i] = BooleanClause.Occur.SHOULD;
            }
            parsedQuery = MultiFieldQueryParser.parse(Version.LUCENE_29, query, fields, flags, analyzer);
            bQuery.add(parsedQuery, BooleanClause.Occur.MUST);
        } else {
            QueryParser qp = new QueryParser(Version.LUCENE_29, "ft", analyzer);
            parsedQuery = qp.parse(query);
            bQuery.add(parsedQuery, BooleanClause.Occur.MUST);
        }

        if (virtualWikiNames != null && virtualWikiNames.length() > 0) {
            bQuery.add(buildOredTermQuery(virtualWikiNames, IndexFields.DOCUMENT_WIKI), BooleanClause.Occur.MUST);
        }
        if (languages != null && languages.length() > 0) {
            bQuery.add(buildOredTermQuery(languages, IndexFields.DOCUMENT_LANGUAGE), BooleanClause.Occur.MUST);
        }

        return bQuery;
    }

    /**
     * @param values comma separated list of values to look for
     * @return A query returning documents matching one of the given values in the given field
     */
    private Query buildOredTermQuery(final String values, final String fieldname)
    {
        String[] valueArray = values.split("\\,");
        if (valueArray.length > 1) {
            // build a query like this: <valueArray[0]> OR <valueArray[1]> OR ...
            BooleanQuery orQuery = new BooleanQuery();
            for (int i = 0; i < valueArray.length; i++) {
                orQuery.add(new TermQuery(new Term(fieldname, valueArray[i].trim())), BooleanClause.Occur.SHOULD);
            }

            return orQuery;
        }

        // exactly one value, no OR'ed Terms necessary
        return new TermQuery(new Term(fieldname, valueArray[0]));
    }

    public synchronized void init(XWikiContext context)
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Lucene plugin: in init");
        }

        this.indexDirs = context.getWiki().Param(PROP_INDEX_DIR);
        if (StringUtils.isEmpty(this.indexDirs)) {
            File workDir = context.getWiki().getWorkSubdirectory("lucene", context);
            this.indexDirs = workDir.getAbsolutePath();
        }
        String indexDir = StringUtils.split(this.indexDirs, ",")[0];

        File f = new File(indexDir);
        Directory directory;
        try {
            if (!f.exists()) {
                f.mkdirs();
            }
            directory = FSDirectory.open(f);
        } catch (IOException e) {
            LOG.error("Failed to open the index directory: ", e);
            throw new RuntimeException(e);
        }

        init(directory, context);
    }

    public void init(Directory directory, XWikiContext context)
    {
        int indexingInterval;
        try {
            indexingInterval = 1000 * (int) context.getWiki().ParamAsLong(PROP_INDEXING_INTERVAL, 30);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid indexing interval in configuration.");
            indexingInterval = 30000;
        }

        int maxQueueSize;
        try {
            maxQueueSize = (int) context.getWiki().ParamAsLong(LucenePlugin.PROP_MAX_QUEUE_SIZE, 1000);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid max queue size in configuration.");
            maxQueueSize = 1000;
        }

        IndexUpdater indexUpdater = new IndexUpdater(directory, indexingInterval, maxQueueSize, this, context);

        init(indexUpdater, context);
    }

    public void init(IndexUpdater indexUpdater, XWikiContext context)
    {
        Directory directory = indexUpdater.getDirectory();

        boolean needInitialRebuild = true;
        try {
            needInitialRebuild = !IndexReader.indexExists(directory);
        } catch (IOException e) {
            LOG.warn("Failed to check if index exists: " + e);
        }

        IndexRebuilder indexRebuilder = new IndexRebuilder(indexUpdater, context);
        if (needInitialRebuild) {
            indexRebuilder.startRebuildIndex(context);
            LOG.info("Launched initial lucene indexing");
        }

        init(indexUpdater, indexRebuilder, context);
    }

    public void init(IndexUpdater indexUpdater, IndexRebuilder indexRebuilder, XWikiContext context)
    {
        super.init(context);

        try {
            this.analyzer =
                (Analyzer) Class.forName(context.getWiki().Param(PROP_ANALYZER, DEFAULT_ANALYZER)).newInstance();
        } catch (Exception e) {
            LOG.error("Error instantiating analyzer : ", e);
            LOG.warn("Using default analyzer class: " + DEFAULT_ANALYZER);
            try {
                this.analyzer = (Analyzer) Class.forName(DEFAULT_ANALYZER).newInstance();
            } catch (Exception e1) {
                throw new RuntimeException("Instantiation of default analyzer " + DEFAULT_ANALYZER + " failed", e1);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Assigning index updater: " + indexUpdater);
        }

        if (this.indexDirs == null) {
            this.indexDirs = context.getWiki().Param(PROP_INDEX_DIR);
            if (StringUtils.isEmpty(this.indexDirs)) {
                File workDir = context.getWiki().getWorkSubdirectory("lucene", context);
                this.indexDirs = workDir.getAbsolutePath();
            }
        }

        this.indexUpdater = indexUpdater;
        this.indexUpdater.setAnalyzer(this.analyzer);
        this.indexUpdaterThread = new Thread(indexUpdater, "Lucene Index Updater");
        this.indexUpdaterThread.start();
        this.indexRebuilder = indexRebuilder;

        openSearchers(context);

        // Register the Index Updater as an Event Listener so that modified documents/attachments are added to the
        // Lucene indexing queue.
        // If the Index Updater is already registered don't do anything.
        ObservationManager observationManager = Utils.getComponent(ObservationManager.class);
        if (observationManager.getListener(indexUpdater.getName()) == null) {
            observationManager.addListener(indexUpdater);
        }

        LOG.debug("Lucene plugin initialized.");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#flushCache(com.xpn.xwiki.XWikiContext)
     */
    public void flushCache(XWikiContext context)
    {
        // take care of crappy code calling #flushCache with no context...
        if (context == null) {
            context =
                (XWikiContext) Utils.getComponent(Execution.class).getContext()
                    .getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        }

        if (this.indexUpdater != null) {
            Utils.getComponent(ObservationManager.class).removeListener(this.indexUpdater.getName());

            // set the thread to exit
            this.indexUpdater.doExit();

            try {
                // wait for the thread to finish
                this.indexUpdaterThread.join();
            } catch (InterruptedException ex) {
                LOG.warn("Error while waiting for indexUpdaterThread to die.", ex);
            }

            this.indexUpdater = null;
            this.indexUpdaterThread = null;
        }

        this.indexRebuilder = null;

        try {
            closeSearchers(this.searchers);
        } catch (IOException e) {
            LOG.warn("Cannot close searchers", e);
        }

        this.analyzer = null;

        init(context);
    }

    public String getName()
    {
        return "lucene";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new LucenePluginApi((LucenePlugin) plugin, context);
    }

    /**
     * Creates an array of Searchers for a number of lucene indexes.
     * 
     * @param indexDirs Comma separated list of Lucene index directories to create searchers for.
     * @return Array of searchers
     */
    public Searcher[] createSearchers(String indexDirs, XWikiContext context) throws Exception
    {
        String[] dirs = StringUtils.split(indexDirs, ",");
        List<IndexSearcher> searchersList = new ArrayList<IndexSearcher>();
        for (String dir : dirs) {
            while (true) {
                try {
                    if (!IndexReader.indexExists(dir)) {
                        // If there's no index there, create an empty one; otherwise the reader
                        // constructor will throw an exception and fail to initialize
                        new IndexWriter(dir, this.analyzer).close();
                    }

                    searchersList.add(new IndexSearcher(dir, true));
                    break;
                } catch (CorruptIndexException e) {
                    handleCorruptIndex(context);
                }
            }
        }

        return searchersList.toArray(new Searcher[searchersList.size()]);
    }

    /**
     * Opens the searchers for the configured index Dirs after closing any already existing ones.
     */
    protected synchronized void openSearchers(XWikiContext context)
    {
        try {
            closeSearchers(this.searchers);
            this.searchers = createSearchers(this.indexDirs, context);
        } catch (Exception e) {
            LOG.error("Error opening searchers for index dirs " + context.getWiki().Param(PROP_INDEX_DIR), e);
            throw new RuntimeException("Error opening searchers for index dirs "
                + context.getWiki().Param(PROP_INDEX_DIR), e);
        }
    }

    /**
     * @throws IOException
     */
    protected static void closeSearchers(Searcher[] searchers) throws IOException
    {
        if (searchers != null) {
            for (int i = 0; i < searchers.length; i++) {
                if (searchers[i] != null) {
                    searchers[i].close();
                }
            }
        }
    }

    public String getIndexDirs()
    {
        return this.indexDirs;
    }

    public long getQueueSize()
    {
        return this.indexUpdater.getQueueSize();
    }

    public void queueDocument(XWikiDocument doc, XWikiContext context)
    {
        this.indexUpdater.queueDocument(doc, context, false);
    }

    public void queueAttachment(XWikiDocument doc, XWikiAttachment attach, XWikiContext context)
    {
        this.indexUpdater.queueAttachment(attach, context, false);
    }

    public void queueAttachment(XWikiDocument doc, XWikiContext context)
    {
        this.indexUpdater.queueAttachments(doc, context);
    }

    /**
     * @return the number of documents Lucene index writer.
     */
    public long getLuceneDocCount()
    {
        return this.indexUpdater.getLuceneDocCount();
    }

    /**
     * Handle a corrupt index by clearing it and rebuilding from scratch.
     */
    void handleCorruptIndex(XWikiContext context) throws IOException
    {
        rebuildIndex(context);
    }
}
