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
 * Created on 01.02.2005
 *
 */
package com.xpn.xwiki.plugin.lucene;

import java.util.*;

import org.apache.log4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Handles rebuilding of the whole Index. This involves the following steps:
 * <ul>
 * <li>empty the existing index</li>
 * <li>retrieve the names of all virtual wikis</li>
 * <li>get and index all documents for each virtual wiki</li>
 * <li>get and index all translations of each document</li>
 * <li>get and index all attachments of each document</li>
 * </ul>
 * The indexing of all content fetched from the wiki is triggered by handing the
 * data to the indexUpdater thread.
 *
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class IndexRebuilder {
    private IndexUpdater indexUpdater;
    private static final Logger LOG = Logger.getLogger(IndexRebuilder.class);

    /**
     * First empties the index, then fetches all Documents, their translations
     * and their attachments for re-addition to the index.
     *
     * @param wiki
     * @param context
     * @return total number of documentes and attachments successfully added to
     *         the indexer queue, -1 when errors occured.
     * @throws XWikiException
     * @todo TODO: give more detailed results
     */
    public int rebuildIndex(com.xpn.xwiki.api.XWiki wiki, XWikiContext context) {
        indexUpdater.cleanIndex();
        int retval = 0;
        Collection wikiServers;
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        if (wiki.isVirtual()) {
            wikiServers = findWikiServers(wiki, context);
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
            ((ArrayList) wikiServers).add(context.getDatabase());
        }
        // Iterate all found virtual wikis
        for (Iterator iter = wikiServers.iterator(); iter.hasNext();) {
            int wikiResult = indexWiki(xwiki, (String) iter.next());
            if (retval != -1) retval += wikiResult;
        }
        return retval;
    }

    /**
     * Adds the content of a given wiki to the indexUpdater's queue.
     *
     * @param xwiki
     * @param context
     * @param wikiName
     * @return
     */
    protected int indexWiki(com.xpn.xwiki.XWiki xwiki, String wikiName) {
        LOG.info("reading content of wiki " + wikiName);
        int retval = 0;
        XWikiContext wikiContext = new XWikiContext();
        wikiContext.setWiki(xwiki);
        wikiContext.setDatabase(wikiName);
        Collection docNames = null;
        try {
            docNames = xwiki.getStore().searchDocumentsNames("", wikiContext);
        } catch (XWikiException e1) {
            LOG.error("error getting document names for wiki " + wikiName);
            e1.printStackTrace();
            return -1;
        }
        for (Iterator iterator = docNames.iterator(); iterator.hasNext();) {
            String docName = (String) iterator.next();
            XWikiDocument document;
            try {
                document = xwiki.getDocument(docName, wikiContext);
            } catch (XWikiException e2) {
                LOG.error("error fetching document " + wikiName + ":" + docName);
                e2.printStackTrace();
                continue;
            }
            if (document != null) {
                indexUpdater.add(document, wikiContext);
                retval++;
                retval += addTranslationsOfDocument(document, wikiContext);
                retval += addAttachmentsOfDocument(document, wikiContext);
                retval += addObjectsOfDocument(document, wikiContext);
            } else {
                LOG.info("XWiki delivered null for document name " + wikiName + ":" + docName);
            }
        }
        return retval;
    }

    /**
     * Getting the content(values of title/category/content/extract properties ) from the XWiki.ArticleClass objects
     * @param document
     * @param wikiContext
     */
    private int addObjectsOfDocument(XWikiDocument document, XWikiContext wikiContext) {
        int retval = 0;
        Map xwikiObjects = document.getxWikiObjects();
        if (document.hasElement(XWikiDocument.HAS_OBJECTS)) {
            retval += xwikiObjects.size();
            indexUpdater.addObject(document, wikiContext);
        }
        return retval;
    }

    /**
     * @param document
     * @param wikiContext
     */
    private int addAttachmentsOfDocument(XWikiDocument document, XWikiContext wikiContext) {
        int retval = 0;
        final List attachmentList = document.getAttachmentList();
        retval += attachmentList.size();
        for (Iterator attachmentIter = attachmentList.iterator(); attachmentIter.hasNext();) {
            try {
                XWikiAttachment attachment = (XWikiAttachment) attachmentIter.next();
                indexUpdater.add(document, attachment, wikiContext);
            } catch (Exception e) {
                LOG.error("error retrieving attachment of document " + document.getFullName(), e);
            }
        }
        return retval;
    }

    /**
     * @param document
     * @param wikiContext
     * @throws XWikiException
     */
    protected int addTranslationsOfDocument(XWikiDocument document, XWikiContext wikiContext) {
        int retval = 0;
        List translations;
        try {
            translations = document.getTranslationList(wikiContext);
        } catch (XWikiException e) {
            LOG.error("error getting list of translations from document " + document.getFullName(), e);
            e.printStackTrace();
            return 0;
        }
        for (Iterator iter = translations.iterator(); iter.hasNext();) {
            String lang = (String) iter.next();
            try {
                indexUpdater.add(document.getTranslatedDocument(lang, wikiContext), wikiContext);
                retval++;
            } catch (XWikiException e1) {
                LOG.error("error getting translated document for document " + document.getFullName()
                        + " and language " + lang);
                e1.printStackTrace();
            }
        }
        return retval;
    }

    /**
     * @param wiki
     * @return
     */
    private Collection findWikiServers(XWiki wiki, XWikiContext context) {
        List retval = new ArrayList();
        final String hql = ", BaseObject as obj, StringProperty as prop "
                + "where doc.fullName=obj.name and obj.className='XWiki.XWikiServerClass'"
                + " and prop.id.id = obj.id " + "and prop.id.name = 'server'";
        List result = null;
        try {
            result = wiki.getXWiki().getStore().searchDocumentsNames(hql, context);
        } catch (Exception e) {
            LOG.error("error getting list of wiki servers!");
        }
        if (result != null) {
            for (Iterator iter = result.iterator(); iter.hasNext();) {
                String docname = (String) iter.next();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("possible server name: " + docname);
                }
                if (docname.startsWith("XWiki.XWikiServer")) {
                    retval.add(docname.substring("XWiki.XWikiServer".length()).toLowerCase());
                }
            }
        }
        return retval;
    }

    public void setIndexUpdater(IndexUpdater indexUpdater) {
        this.indexUpdater = indexUpdater;
    }
}
