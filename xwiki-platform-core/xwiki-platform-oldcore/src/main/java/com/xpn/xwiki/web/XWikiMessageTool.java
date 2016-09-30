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
package com.xpn.xwiki.web;

import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.localization.ContextualLocalizationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Internationalization service based on key/property values. The key is the id of the message being looked for and the
 * returned value is the message in the language requested. There are 3 sources where properties are looked for (in the
 * specified order):
 * <ol>
 * <li>If there's a "documentBundles" property in the XWiki Preferences page then the XWiki documents listed there
 * (separated by commas) are considered the source for properties</li>
 * <li>If there's a "xwiki.documentBundles" property in the XWiki configuration file (xwiki.cfg) then the XWiki
 * documents listed there (separated by commas) are considered for source for properties</li>
 * <li>The Resource Bundle passed in the constructor</li>
 * </ol>
 * If the property is not found in any of these 3 sources then the key is returned in place of the value. In addition
 * the property values are cached for better performance but if one of the XWiki documents containing the properties is
 * modified, its content is cached again next time a key is asked.
 *
 * @version $Id$
 * @deprecated since 4.3M2 use the {@link org.xwiki.localization.LocalizationManager} component instead
 */
@Deprecated
public class XWikiMessageTool
{
    /**
     * Log4J logger object to log messages in this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiMessageTool.class);

    /**
     * Property name used to defined internationalization document bundles in either XWikiProperties ("documentBundles")
     * or in the xwiki.cfg configuration file ("xwiki.documentBundles").
     */
    private static final String KEY = "documentBundles";

    /**
     * Format string for the error message used to log load failures.
     */
    private static final String LOAD_ERROR_MSG_FMT = "Failed to load internationalization document bundle [ %s ].";

    /**
     * The default Resource Bundle to fall back to if no document bundle is found when trying to get a key.
     */
    protected ResourceBundle bundle;

    /**
     * The {@link com.xpn.xwiki.XWikiContext} object, used to get access to XWiki primitives for loading documents.
     */
    @Deprecated
    protected XWikiContext context;

    /**
     * Cache properties loaded from the document bundles for maximum efficiency. The map is of type (Long, Properties)
     * where Long is the XWiki document ids.
     */
    private Map<Long, Properties> propsCache = new HashMap<Long, Properties>();

    /**
     * Cache for saving the last modified dates of document bundles that have been loaded. This is used so that we can
     * reload them if they've been modified since last time they were cached. The map is of type (Long, Date) where Long
     * is the XWiki document ids.
     */
    private Map<Long, Date> previousDates = new HashMap<Long, Date>();

    /**
     * List of document bundles that have been modified since the last time they were cached. The Set contains Long
     * objects which are the XWiki document ids. TODO: This instance variable should be removed as it's used internally
     * and its state shouldn't encompass several calls to get().
     */
    private Set<Long> docsToRefresh = new HashSet<Long>();

    /**
     * The localization manager.
     */
    private ContextualLocalizationManager localization;

    /**
     * @param localization the localization manager
     */
    public XWikiMessageTool(ContextualLocalizationManager localization)
    {
        this.localization = localization;
    }

    /**
     * @param bundle the default Resource Bundle to fall back to if no document bundle is found when trying to get a key
     * @param context the {@link com.xpn.xwiki.XWikiContext} object, used to get access to XWiki primitives for loading
     *            documents
     */
    public XWikiMessageTool(ResourceBundle bundle, XWikiContext context)
    {
        this.bundle = bundle;
        this.context = context;
    }

    protected XWikiContext getXWikiContext()
    {
        return this.context;
    }

    /**
     * @param key the key identifying the message to look for
     * @return the message in the defined language. The message should be a simple string without any parameters. If you
     *         need to pass parameters see {@link #get(String, java.util.List)}
     * @see com.xpn.xwiki.web.XWikiMessageTool for more details on the algorithm used to find the message
     */
    public String get(String key)
    {
        String translation;
        if (this.localization != null) {
            translation = get(key, ArrayUtils.EMPTY_OBJECT_ARRAY);
        } else {
            translation = getTranslation(key);
            if (translation == null) {
                try {
                    translation = this.bundle.getString(key);
                } catch (Exception e) {
                    translation = key;
                }
            }
        }

        return translation;
    }

    /**
     * Find a translation and then replace any parameters found in the translation by the passed params parameters. The
     * format is the one used by {@link java.text.MessageFormat}.
     * <p>
     * Note: The reason we're using a List instead of an Object array is because we haven't found how to easily create
     * an Array in Velocity whereas a List is easily created. For example: <code>$msg.get("key", ["1", "2", "3"])</code>
     * .
     * </p>
     *
     * @param key the key of the string to find
     * @param params the list of parameters to use for replacing "{N}" elements in the string. See
     *            {@link java.text.MessageFormat} for the full syntax
     * @return the translated string with parameters resolved
     */
    public String get(String key, List<?> params)
    {
        return get(key, params.toArray());
    }

    /**
     * Find a translation and then replace any parameters found in the translation by the passed parameters. The format
     * is the one used by {@link java.text.MessageFormat}.
     *
     * @param key the key of the string to find
     * @param params the list of parameters to use for replacing "{N}" elements in the string. See
     *            {@link java.text.MessageFormat} for the full syntax
     * @return the translated string with parameters resolved
     */
    public String get(String key, Object... params)
    {
        String translation;
        if (this.localization != null) {
            translation = this.localization.getTranslationPlain(key, params);
            if (translation == null) {
                translation = key;
            }
        } else {
            translation = get(key);

            if (params != null && translation != null) {
                translation = MessageFormat.format(translation, params);
            }
        }

        return translation;
    }

    /**
     * @return the list of internationalization document bundle names as a list of XWiki page names ("Space.Document")
     *         or an empty list if no such documents have been found
     * @see com.xpn.xwiki.web.XWikiMessageTool for more details on the algorithm used to find the document bundles
     */
    protected List<String> getDocumentBundleNames()
    {
        List<String> docNamesList;

        XWikiContext context = getXWikiContext();

        String docNames = context.getWiki().getXWikiPreference(KEY, context);
        if (docNames == null || "".equals(docNames)) {
            docNames = context.getWiki().Param("xwiki." + KEY);
        }

        if (docNames == null) {
            docNamesList = new ArrayList<String>();
        } else {
            docNamesList = Arrays.asList(docNames.split(","));
        }

        return docNamesList;
    }

    /**
     * @return the internationalization document bundles (a list of {@link XWikiDocument})
     * @see com.xpn.xwiki.web.XWikiMessageTool for more details on the algorithm used to find the document bundles
     */
    public List<XWikiDocument> getDocumentBundles()
    {
        XWikiContext context = getXWikiContext();

        String defaultLanguage = context.getWiki().getDefaultLanguage(context);
        List<XWikiDocument> result = new ArrayList<XWikiDocument>();
        for (String docName : getDocumentBundleNames()) {
            for (XWikiDocument docBundle : getDocumentBundles(docName.trim(), defaultLanguage)) {
                if (docBundle != null) {
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
                    } else {
                        // The document listed as a document bundle doesn't exist. Do nothing
                        // and log.
                        LOGGER.warn("The document [" + docBundle.getFullName() + "] is listed "
                            + "as an internationalization document bundle but it does not " + "exist.");
                    }
                }
            }
        }
        return result;
    }

    /**
     * Helper method to help get a translated version of a document. It handles any exception raised to make it easy to
     * use.
     *
     * @param documentName the document's name (eg Space.Document)
     * @return the document object corresponding to the passed document's name. A translated version of the document for
     *         the current Locale is looked for.
     */
    public XWikiDocument getDocumentBundle(String documentName)
    {
        XWikiDocument docBundle;

        if (documentName.length() == 0) {
            docBundle = null;
        } else {
            try {
                XWikiContext context = getXWikiContext();

                // First, looks for a document suffixed by the language
                docBundle = context.getWiki().getDocument(documentName, context);
                docBundle = docBundle.getTranslatedDocument(context);
            } catch (XWikiException e) {
                // Error while loading the document.
                // TODO: A runtime exception should be thrown that will bubble up till the
                // topmost level. For now simply log the error
                LOGGER.error(String.format(LOAD_ERROR_MSG_FMT, documentName), e);
                docBundle = null;
            }
        }

        return docBundle;
    }

    /**
     * Helper method to help get a translated version of a document. It handles any exception raised to make it easy to
     * use.
     *
     * @param documentName the document's name (eg Space.Document)
     * @param defaultLanguage default language
     * @return the document object corresponding to the passed document's name. A translated version of the document for
     *         the current Locale is looked for.
     */
    public List<XWikiDocument> getDocumentBundles(String documentName, String defaultLanguage)
    {
        List<XWikiDocument> list = new ArrayList<XWikiDocument>();

        if (documentName.length() != 0) {
            try {
                XWikiContext context = getXWikiContext();

                // First, looks for a document suffixed by the language
                XWikiDocument docBundle = context.getWiki().getDocument(documentName, context);
                XWikiDocument tdocBundle = docBundle.getTranslatedDocument(context);
                list.add(tdocBundle);
                if (!tdocBundle.getRealLanguage().equals(defaultLanguage)) {
                    XWikiDocument defdocBundle = docBundle.getTranslatedDocument(defaultLanguage, context);
                    if (tdocBundle != defdocBundle) {
                        list.add(defdocBundle);
                    }
                }

            } catch (XWikiException e) {
                // Error while loading the document.
                // TODO: A runtime exception should be thrown that will bubble up till the
                // topmost level. For now simply log the error
                LOGGER.error(String.format(LOAD_ERROR_MSG_FMT, documentName), e);
            }
        }

        return list;
    }

    /**
     * @param docBundle the document bundle.
     * @return properties of the document bundle.
     */
    public Properties getDocumentBundleProperties(XWikiDocument docBundle)
    {
        Properties props = new Properties();
        String content = docBundle.getContent();
        try {
            props.load(new StringReader(content));
        } catch (IOException e) {
            LOGGER.error("Failed to parse content of document [" + docBundle + "] as translation content", e);
        }

        return props;
    }

    /**
     * Looks for a translation in the list of internationalization document bundles. It first checks if the translation
     * can be found in the cache.
     *
     * @param key the key identifying the translation
     * @return the translation or null if not found or if the passed key is null
     */
    protected String getTranslation(String key)
    {
        String returnValue = null;

        if (key != null) {
            for (XWikiDocument docBundle : getDocumentBundles()) {
                if (docBundle != null) {
                    Long docId = new Long(docBundle.getId());
                    Properties props = null;
                    if (this.docsToRefresh.contains(docId) || !this.propsCache.containsKey(docId)) {
                        // Cache needs to be updated
                        props = getDocumentBundleProperties(docBundle);
                        // updates cache
                        this.propsCache.put(docId, props);
                        this.docsToRefresh.remove(docId);
                    } else {
                        // gets from cache
                        props = this.propsCache.get(docId);
                    }

                    returnValue = props.getProperty(key);
                    if (returnValue != null) {
                        break;
                    }
                }
            }
        }

        return returnValue;
    }
}
