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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.localization.Bundle;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.FixedNameEventFilter;

/**
 * Base class for {@link Bundle}s using wiki documents as resources. Provides methods for loading properties from
 * documents, watching loaded documents and invalidating cached translations.
 * 
 * @version $Id$
 * @since 4.3M1
 */
public abstract class AbstractWikiDocumentBundle extends AbstractCachedBundle implements Bundle
{
    /**
     * The encoding used for storing unicode characters as bytes.
     */
    protected static final String UNICODE_BYTE_ENCODING = "UTF-8";

    /**
     * Provides access to documents.
     */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    protected TranslationMessageParser translationMessageParser;

    private DocumentReference reference;

    protected LocaleBundle loadDocumentBundle(DocumentReference reference, Locale locale) throws Exception
    {
        String content = this.documentAccessBridge.getDocumentContent(reference, locale.toString());

        Properties properties = new Properties();
        properties.load(new StringReader(content));

        // Convert to LocalBundle
        DefaultLocaleBundle localeBundle;

        localeBundle = new DefaultLocaleBundle(this, locale);

        TranslationMessageParser parser = getTranslationMessageParser();

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                String key = (String) entry.getKey();
                String message = (String) entry.getValue();

                TranslationMessage translationMessage = parser.parse(message);

                localeBundle.addTranslation(new DefaultTranslation(this.bundleContext, localeBundle, key,
                    translationMessage));
            }
        }

        return localeBundle;
    }

    protected TranslationMessageParser getTranslationMessageParser()
    {
        
    }
}
