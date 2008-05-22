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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * <p>
 * Handles rebuilding of the whole Lucene Search Index. This involves the following steps:
 * <ul>
 * <li>empty the existing index</li>
 * <li>retrieve the names of all virtual wikis</li>
 * <li>foreach document in each virtual wiki:
 * <ul>
 * <li>index the document</li>
 * <li>get and index all translations of the document</li>
 * <li>get and index all attachments of the document</li>
 * <li>get and index all objects of the document</li>
 * </ul>
 * </li>
 * </ul>
 * The rebuild can be triggered using the {@link LucenePluginApi#rebuildIndex()} method of the
 * {@link LucenePluginApi}. Once a rebuild request is made, a new thread is created, so the
 * requesting script can continue processing, while the rebuilding is done in the background. The
 * actual indexing is done by the IndexUpdater thread, this thread just gathers the data and passes
 * it to the IndexUpdater.
 * </p>
 * <p>
 * As a summary, this plugin:
 * <ul>
 * <li>cleans the Lucene search indexes and re-submits all the contents of all the wikis for
 * indexing</li>
 * <li>without clogging the indexing thread (since 1.2)</li>
 * <li>all in a background thread (since 1.2)</li>
 * <li>making sure that only one rebuild is in progress (since 1.2)</li>
 * </ul>
 * </p>
 * 
 * @version $Id: $
 */
public class IndexRebuilder extends AbstractXWikiRunnable
{
    /** Logging helper. */
    private static final Log LOG = LogFactory.getLog(IndexRebuilder.class);

    /** The actual object/thread that indexes data. */
    private IndexUpdater indexUpdater;

    /** The XWiki context. */
    private XWikiContext context;

    /** Amount of time (milliseconds) to sleep while waiting for the indexing queue to empty. */
    private static int retryInterval = 30000;

    /** Variable used for indicating that a rebuild is already in progress. */
    private boolean rebuildInProgress = false;

    public IndexRebuilder(IndexUpdater indexUpdater, XWikiContext context)
    {
        this.indexUpdater = indexUpdater;
        if (indexUpdater.needInitialBuild) {
            this.startRebuildIndex(context);
            LOG.info("Launched initial lucene indexing");
        }
    }

    public synchronized int startRebuildIndex(XWikiContext context)
    {
        if (rebuildInProgress) {
            LOG.warn("Cannot launch rebuild because a build is in progress");
            return LucenePluginApi.REBUILD_IN_PROGRESS;
        } else {
            this.rebuildInProgress = true;
            this.context = context;
            Thread indexRebuilderThread = new Thread(this, "Lucene Index Rebuilder");
            // The JVM should be allowed to shutdown while this thread is running
            indexRebuilderThread.setDaemon(true);
            // Client requests are more important than indexing
            indexRebuilderThread.setPriority(3);
            // Finally, start the rebuild in the background
            indexRebuilderThread.start();
            // Too bad that now we can't tell how many items are there to be indexed...
            return 0;
        }
    }

    public void run()
    {
        MDC.put("url", "Lucene index rebuilder thread");
        LOG.debug("Starting lucene index rebuild");
        XWikiContext context = null;
        try {
            // The context must be cloned, as otherwise setDatabase() might affect the response to
            // the current request.
            // TODO This is not a good way to do this; ideally there would be a method that creates
            // a new context and copies only a few needed objects, as some objects are not supposed
            // to be used in 2 different contexts.
            // TODO This seems to work on a simple run:
            // context = new XWikiContext();
            // context.setWiki(this.context.getWiki());
            // context.setEngineContext(this.context.getEngineContext());
            // context.setMode(this.context.getMode());
            // context.setAction(this.context.getAction());
            // context.put("msg", this.context.get("msg"));
            // context.setMainXWiki(this.context.getMainXWiki());
            // context.setURLFactory(this.context.getURLFactory());
            // context.setLanguage(this.context.getLanguage());
            // context.setDatabase(this.context.getDatabase());
            // context.put("org.xwiki.component.manager.ComponentManager", this.context
            // .get("org.xwiki.component.manager.ComponentManager"));
            context = (XWikiContext) this.context.clone();
            this.context = null;
            // For example, we definitely don't want to use the same hibernate session...
            context.remove("hibsession");
            context.remove("hibtransaction");
            // This is also causing seriuos problems, as the same xcontext gets shared between
            // threads and causes the hibernate session to be shared in the end. The vcontext is
            // automatically recreated by the velocity renderer, if it isn't found in the xcontext.
            context.remove("vcontext");

            // Since this is where a new thread is created this is where we need to initialize the Container 
            // ThreadLocal variables and not in the init() method. Otherwise we would simply overwrite the
            // Container values for the main thread...
            initXWikiContainer(context);
            
            // The original request and response should not be used outside the actual request
            // processing thread, as they will be cleaned later by the container.
            context.setRequest(null);
            context.setResponse(null);

            rebuildIndex(context);
        } catch (Exception e) {
            LOG.error("Error in lucene rebuild thread", e);
        } finally {
            rebuildInProgress = false;

            // Cleanup Container component (it has ThreadLocal variables)
            cleanupXWikiContainer(context);

            if (context != null) {
                context.getWiki().getStore().cleanUp(context);
            }
            MDC.remove("url");
        }
        LOG.debug("Lucene index rebuild done");
    }

    /**
     * First empties the index, then fetches all Documents, their translations and their attachments
     * for re-addition to the index.
     * 
     * @param context
     * @return total number of documents and attachments successfully added to the indexer queue, -1
     *         when errors occured.
     */
    private int rebuildIndex(XWikiContext context)
    {
        this.indexUpdater.cleanIndex();
        int retval = 0;
        Collection<String> wikiServers;
        XWiki xwiki = context.getWiki();
        if (xwiki.isVirtualMode()) {
            wikiServers = findWikiServers(context);
            if (LOG.isDebugEnabled()) {
                LOG.debug("found " + wikiServers.size() + " virtual wikis:");
                for (String wikiName : wikiServers) {
                    LOG.debug(wikiName);
                }
            }
        } else {
            // No virtual wiki configuration, just index the wiki the context belongs to
            wikiServers = new ArrayList<String>();
            wikiServers.add(context.getDatabase());
        }

        // Iterate all found virtual wikis
        for (String wikiName : wikiServers) {
            int wikiResult = indexWiki(wikiName, context);
            if (wikiResult > 0) {
                retval += wikiResult;
            }
        }

        return retval;
    }

    /**
     * Adds the content of a given wiki to the indexUpdater's queue.
     * 
     * @param wikiName
     * @param context
     * @return
     */
    protected int indexWiki(String wikiName, XWikiContext context)
    {
        LOG.info("Reading content of wiki " + wikiName);
        // Number of index entries processed
        int retval = 0;
        XWiki xwiki = context.getWiki();
        String database = context.getDatabase();

        try {
            context.setDatabase(wikiName);
            Collection<String> docNames = null;
            try {
                docNames = xwiki.getStore().searchDocumentsNames("", context);
            } catch (XWikiException ex) {
                LOG.warn(String.format(
                    "Error getting document names for wiki [%s]. Internal error is: $s",
                    wikiName, ex.getMessage()));
                return -1;
            }
            for (String docName : docNames) {
                XWikiDocument document;
                try {
                    document = xwiki.getDocument(docName, context);
                } catch (XWikiException e2) {
                    LOG.error("error fetching document " + wikiName + ":" + docName, e2);
                    continue;
                }

                if (document != null) {
                    // In order not to load the whole database in memory, we're limiting the number
                    // of documents that are in the processing queue at a moment. We could use a
                    // Bounded Queue in the index updater, but that would generate exceptions in the
                    // rest of the platform, as the index rebuilder could fill the queue, and then a
                    // user trying to save a document would cause an exception. Thus, it is better
                    // to limit the index rebuilder thread only, and not the index updater.
                    while (this.indexUpdater.getQueueSize() > this.indexUpdater.maxQueueSize) {
                        try {
                            // Don't leave any database connections open while sleeping
                            // This shouldn't be needed, but we never know what bugs might be there
                            context.getWiki().getStore().cleanUp(context);
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException e) {
                            return -2;
                        }
                    }
                    this.indexUpdater.add(document, context);
                    retval++;
                    retval += addTranslationsOfDocument(document, context);
                    retval += this.indexUpdater.addAttachmentsOfDocument(document, context);
                    retval += addObjectsOfDocument(document, context);
                } else {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("XWiki delivered null for document name " + wikiName + ":"
                            + docName);
                    }
                }
            }
        } finally {
            context.setDatabase(database);
        }

        return retval;
    }

    /**
     * Getting the content(values of title/category/content/extract properties ) from the
     * XWiki.ArticleClass objects
     */
    private int addObjectsOfDocument(XWikiDocument document, XWikiContext wikiContext)
    {
        int retval = 0;

        if (document.hasElement(XWikiDocument.HAS_OBJECTS)) {
            retval += document.getxWikiObjects().size();
            this.indexUpdater.addObject(document, wikiContext);
        }

        return retval;
    }

    protected int addTranslationsOfDocument(XWikiDocument document, XWikiContext wikiContext)
    {
        int retval = 0;

        List<String> translations;
        try {
            translations = document.getTranslationList(wikiContext);
        } catch (XWikiException e) {
            LOG.error("error getting list of translations from document "
                + document.getFullName(), e);
            e.printStackTrace();
            return 0;
        }

        for (String lang : translations) {
            try {
                this.indexUpdater.add(document.getTranslatedDocument(lang, wikiContext),
                    wikiContext);
                retval++;
            } catch (XWikiException e1) {
                LOG.error("Error getting translated document for document "
                    + document.getFullName() + " and language " + lang, e1);
            }
        }

        return retval;
    }

    private Collection<String> findWikiServers(XWikiContext context)
    {
        List<String> retval = Collections.emptyList();

        try {
            retval = context.getWiki().getVirtualWikisDatabaseNames(context);

            if (!retval.contains(context.getMainXWiki())) {
                retval.add(context.getMainXWiki());
            }
        } catch (Exception e) {
            LOG.error("Error getting list of wiki servers!", e);
        }

        return retval;
    }
}
