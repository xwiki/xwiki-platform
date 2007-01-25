/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 */
package com.xpn.xwiki.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import org.apache.log4j.Logger;

/**
 * Internationalization service based on key/property values. The key is the id of the message
 * being looked for and the returned value is the message in the language requested. There are
 * 3 sources where properties are looked for (in the specified order):
 * <ol>
 *   <li>If there's a "bundledocs" property in the XWiki Preferences page then the XWiki documents
 *       listed there (separated by commas) are considered the source for properties</li>
 *   <li>If there's a "xwiki.bundledodcs" property in the XWiki configuration file (xwiki.cfg) then
 *       the XWiki documents listed there (separated by commas) are considered for source for
 *       properties</li>
 *   <li>The Resource Bundle passed in the constructor</li>
 * </ol>
 * If the property is not found in any of these 3 sources then the key is returned in place of the
 * value.
 * In addition the property values are cached for better performance but if one of the XWiki
 * documents containing the properties is modified, its content is cached again next time a key
 * is asked.
 *
 * @version $Id: $
 */
public class XWikiMessageTool
{
    private static final Logger LOG = Logger.getLogger(XWikiMessageTool.class);

    private static final String KEY = "bundledocs";

    private ResourceBundle bundle;

    private XWikiContext context;

    private Map previousDates = new HashMap(); // <Long, Date>

    private Map propsCache = new HashMap(); // <Long, Properties>

    private Set docsToRefresh = new HashSet(); // <Long>

    public XWikiMessageTool(ResourceBundle bundle, XWikiContext context)
    {
        this.bundle = bundle;
        this.context = context;
    }

    public String get(String key)
    {
        String translation = getTranslation(key);
        if (translation == null) {
            try {
                translation = this.bundle.getString(key);
            } catch (Exception e) {
                translation = key;
            }
        }
        return translation;
    }
    
    /**
     * Tries first to get the preference "bundledocs" in the wiki preferences.
     * If not found, this method tries to find the the entry in the config file
     * under the key "xwiki.bundledocs".
     *
     * @return the list of translation documents as XWiki page names separated by commas
     *         (eg XWiki.trans1,XWiki.trans2, etc) or null if no translation have been found
     */
    protected String getDocumentBundleNames()
    {
        String docNames = this.context.getWiki().getXWikiPreference(KEY, this.context);
        if (docNames == null || "".equals(docNames)) {
            docNames = this.context.getWiki().Param("xwiki." + KEY);
        }
        return docNames;
    }

    /**
     * Retrieve a list of bundle documents. This is done by looking for the
     * document names in the preferences, then looking for the translated
     * versions of these documents.
     *
     * @return the bundle documents if at least one can be found, else an empty list
     */
    protected List getDocumentBundles()
    {
        XWikiDocument docBundle = null;
        List result = new ArrayList();
        String docNames = getDocumentBundleNames();
        if (docNames != null) {
            List docNamesList = Arrays.asList(docNames.split(","));
            Iterator it = docNamesList.iterator();
            while (it.hasNext()) {
                String docName = ((String) it.next()).trim();
                if (!docName.equals("")) {
                    try {
                        // First, looks for a document suffixed by the language
                        docBundle = this.context.getWiki().getDocument(docName, this.context);
                        docBundle = docBundle.getTranslatedDocument(this.context);
                        if (!docBundle.isNew()) {
                            // Checks for a name update
                            Long docId = new Long(docBundle.getId());
                            Date docDate = docBundle.getDate();
                            // Check for a doc modification
                            if (!docDate.equals(this.previousDates.get(docId))) {
                                this.docsToRefresh.add(docId);
                                this.previousDates.put(docId, docDate);
                            }
                            result.add(docBundle);
                        }
                        else
                        {
                            // The document listed as a document bundle doesn't exist. Do nothing
                            // and log.
                            LOG.warn("The document [" + docBundle.getFullName() + "] is listed "
                                + "as an internationalization document bundle but it does not "
                                + "exist.");
                        }
                    } catch (XWikiException e) {
                        // Error while loading the document.
                        // TODO: A runtime exception should be thrown that will bubble up till the
                        // topmost level. For now simply log the error
                        LOG.error("Failed to load internationalization document bundle ["
                            + docBundle + "].", e);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Looks for a translation in the list of documents. It first checks if the
     * translation can be found in the cache.
     *
     * @param key
     *            the key identifying the translation
     * @return the translation or null if not found or if the passed key is null
     */
    protected String getTranslation(String key)
    {
        if (key == null) {
            return null;
        }
        Iterator it = getDocumentBundles().iterator();
        while(it.hasNext()) {
            XWikiDocument docBundle = (XWikiDocument) it.next();
            Long docId = new Long(docBundle.getId());
            Properties props = null;
            if (this.docsToRefresh.contains(docId) || !this.propsCache.containsKey(docId)) {
                // Cache needs to be updated
                props = new Properties();
                if (docBundle != null) {
                    String content = docBundle.getContent();
                    InputStream is = new ByteArrayInputStream(content.getBytes());
                    try {
                        props.load(is);
                    } catch (IOException e) {
                        // Cannot do anything
                    }
                }
                // updates cache
                this.propsCache.put(docId, props);
                this.docsToRefresh.remove(docId);
            } else {
                // gets from cache
                props = (Properties) this.propsCache.get(docId);
            }
            String trad = props.getProperty(key);
            if (trad != null) {
                return trad;
            }
        }
        return null;
    }
}
