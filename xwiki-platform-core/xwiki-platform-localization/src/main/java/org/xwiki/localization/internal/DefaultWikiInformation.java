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
package org.xwiki.localization.internal;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.context.Execution;
import org.xwiki.localization.WikiInformation;

/**
 * Default implementation of the {@link WikiInformation} component. Uses the {@link org.xwiki.context.ExecutionContext}
 * to access information the current request, and the {@link DocumentAccessBridge} to access the wiki configuration.
 * 
 * @version $Id$
 */
@Component
public class DefaultWikiInformation extends AbstractLogEnabled implements WikiInformation
{
    /** The default wiki name to use when the context does not define one. */
    private static final String DEFAULT_WIKI = "xwiki";

    /** The default language to use when the wiki does not define one. */
    private static final String DEFAULT_LANGUAGE = "en";

    /** The name of the property containing the default wiki language. */
    private static final String DEFAULT_LANGUAGE_PROPERTY_NAME = "default_language";

    /**
     * The key used for placing the old XWikiContext in the current execution context. Needed in order to retrieve the
     * name of the current wiki and the current language.
     * 
     * @todo To be removed once we can access document identities and wikis in the new model.
     */
    private static final String XWIKICONTEXT_KEY = "xwikicontext";

    /** The key used for placing the configured locale in the current execution context. */
    private static final String LOCALE_CONTEXT_KEY = "locale";

    /** Provides access to the request context. */
    @Requirement
    private Execution execution;

    /** Provides access to documents. */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public String getDefaultWikiLanguage()
    {
        return getDefaultWikiLanguage(getCurrentWikiName());
    }

    @Override
    public String getDefaultWikiLanguage(String wiki)
    {
        try {
            return StringUtils.defaultIfEmpty(this.documentAccessBridge.getProperty(wiki + ":"
                + PREFERENCES_DOCUMENT_NAME, PREFERENCES_CLASS_NAME, DEFAULT_LANGUAGE_PROPERTY_NAME), DEFAULT_LANGUAGE);
        } catch (Exception ex) {
            getLogger().warn("Error getting the default language of the wiki [{0}]", ex, wiki);
        }
        return DEFAULT_LANGUAGE;
    }

    @Override
    public String getContextLanguage()
    {
        return getContextLocale().getLanguage();
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiInformation#getContextLocale()
     */
    @SuppressWarnings("unchecked")
    public Locale getContextLocale()
    {
        try {
            Map<Object, Object> xcontext =
                (Map<Object, Object>) this.execution.getContext().getProperty(XWIKICONTEXT_KEY);
            if (xcontext.containsKey(LOCALE_CONTEXT_KEY)) {
                return (Locale) xcontext.get(LOCALE_CONTEXT_KEY);
            }
        } catch (Exception ex) {
            getLogger().warn("Error getting the current locale", ex);
        }
        return new Locale(getDefaultWikiLanguage());
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiInformation#getCurrentWikiName()
     */
    @SuppressWarnings("unchecked")
    public String getCurrentWikiName()
    {
        try {
            Map<Object, Object> xcontext =
                (Map<Object, Object>) this.execution.getContext().getProperty(XWIKICONTEXT_KEY);
            return StringUtils.defaultIfEmpty((String) xcontext.get("wikiName"), DEFAULT_WIKI);
        } catch (Exception ex) {
            getLogger().warn("Error getting the current wiki name", ex);
            return DEFAULT_WIKI;
        }
    }
}
