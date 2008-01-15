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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Handles rebuilding of the whole Index. This involves the following steps:
 * <ul>
 * <li>empty the existing index</li>
 * <li>retrieve the names of all virtual wikis</li>
 * <li>get and index all documents for each virtual wiki</li>
 * <li>get and index all translations of each document</li>
 * <li>get and index all attachments of each document</li>
 * </ul>
 * The indexing of all contents fetched from the wiki is triggered by handing the data to the
 * indexUpdater thread.
 * 
 * @version $Id: $
 */
public class IndexRebuilder
{
    private static final Log LOG = LogFactory.getLog(IndexRebuilder.class);

    private IndexUpdater indexUpdater;

    public IndexRebuilder(IndexUpdater indexUpdater, XWikiContext context)
    {
        this.indexUpdater = indexUpdater;

        if (indexUpdater.needInitialBuild) {
            int retval = this.rebuildIndex(context);
            if (LOG.isInfoEnabled()) {
                LOG.info("Initializing Lucene search index, " + retval + " documents found");
            }
        }
    }

    /**
     * First empties the index, then fetches all Documents, their translations and their attachments
     * for re-addition to the index.
     * 
     * @param context
     * @return total number of documents and attachments successfully added to the indexer queue, -1
     *         when errors occured. TODO: give more detailed results
     */
    public int rebuildIndex(XWikiContext context)
    {
        this.indexUpdater.cleanIndex();
        int retval = 0;
        Collection wikiServers;
        XWiki xwiki = context.getWiki();

        if (xwiki.isVirtual()) {
            wikiServers = findWikiServers(context);
            if (LOG.isDebugEnabled()) {
                LOG.debug("found " + wikiServers.size() + " virtual wikis:");
                for (Iterator iter = wikiServers.iterator(); iter.hasNext();) {
                    LOG.debug(iter.next());
                }
            }
        } else {
            // no virtual wiki configuration, just index the wiki the context
            // belongs to
            wikiServers = new ArrayList();
            wikiServers.add(context.getDatabase());
        }

        // Iterate all found virtual wikis
        for (Iterator iter = wikiServers.iterator(); iter.hasNext();) {
            int wikiResult = indexWiki((String) iter.next(), context);
            if (retval != -1) {
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
        if (LOG.isInfoEnabled()) {
            LOG.info("reading content of wiki " + wikiName);
        }

        int retval = 0;

        XWiki xwiki = context.getWiki();

        String database = context.getDatabase();

        try {
            context.setDatabase(wikiName);

            Collection docNames = null;
            try {
                docNames = xwiki.getStore().searchDocumentsNames("", context);
            } catch (XWikiException e1) {
                LOG.error("error getting document names for wiki " + wikiName, e1);
                return -1;
            }

            for (Iterator iterator = docNames.iterator(); iterator.hasNext();) {
                String docName = (String) iterator.next();

                XWikiDocument document;
                try {
                    document = xwiki.getDocument(docName, context);
                } catch (XWikiException e2) {
                    LOG.error("error fetching document " + wikiName + ":" + docName, e2);
                    continue;
                }

                if (document != null) {
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

        Map xwikiObjects = document.getxWikiObjects();
        if (document.hasElement(XWikiDocument.HAS_OBJECTS)) {
            retval += xwikiObjects.size();
            this.indexUpdater.addObject(document, wikiContext);
        }

        return retval;
    }

    protected int addTranslationsOfDocument(XWikiDocument document, XWikiContext wikiContext)
    {
        int retval = 0;

        List translations;
        try {
            translations = document.getTranslationList(wikiContext);
        } catch (XWikiException e) {
            LOG.error("error getting list of translations from document "
                + document.getFullName(), e);
            e.printStackTrace();
            return 0;
        }

        for (Iterator iter = translations.iterator(); iter.hasNext();) {
            String lang = (String) iter.next();
            try {
                this.indexUpdater.add(document.getTranslatedDocument(lang, wikiContext),
                    wikiContext);
                retval++;
            } catch (XWikiException e1) {
                LOG.error("error getting translated document for document "
                    + document.getFullName() + " and language " + lang, e1);
            }
        }

        return retval;
    }

    private Collection findWikiServers(XWikiContext context)
    {
        List retval = Collections.EMPTY_LIST;

        try {
            retval = context.getWiki().getVirtualWikisDatabaseNames(context);

            if (!retval.contains(context.getMainXWiki())) {
                retval.add(context.getMainXWiki());
            }
        } catch (Exception e) {
            LOG.error("error getting list of wiki servers!", e);
        }

        return retval;
    }
}
