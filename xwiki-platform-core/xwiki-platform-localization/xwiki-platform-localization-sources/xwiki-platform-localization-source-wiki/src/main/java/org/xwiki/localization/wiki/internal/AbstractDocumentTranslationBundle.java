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
package org.xwiki.localization.wiki.internal;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;

import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.cache.DisposableCacheValue;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleContext;
import org.xwiki.localization.internal.AbstractCachedTranslationBundle;
import org.xwiki.localization.internal.DefaultLocalizedTranslationBundle;
import org.xwiki.localization.internal.DefaultTranslation;
import org.xwiki.localization.internal.LocalizedTranslationBundle;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Base class for {@link TranslationBundle}s using wiki documents as resources. Provides methods for loading properties
 * from documents, watching loaded documents and invalidating cached translations.
 * 
 * @version $Id$
 * @since 4.3M2
 */
public abstract class AbstractDocumentTranslationBundle extends AbstractCachedTranslationBundle implements
    DisposableCacheValue, Disposable, EventListener
{
    /**
     * Make default wiki document based translation priority a bit higher than the default one.
     */
    public static final int DEFAULTPRIORITY_WIKI = DEFAULTPRIORITY - 100;

    protected ComponentManager componentManager;

    protected TranslationBundleContext bundleContext;

    protected EntityReferenceSerializer<String> serializer;

    protected Provider<XWikiContext> contextProvider;

    protected ObservationManager observation;

    protected TranslationMessageParser translationMessageParser;

    protected List<Event> events;

    /**
     * Indicate if it should try to access translations or not. A document bundle is usually in disposed state because
     * the associated document does not exist anymore but something is still (wrongly) holding a reference to it.
     */
    protected boolean disposed;

    protected DocumentReference documentReference;

    /**
     * The prefix to use when generating the bundle unique identifier.
     */
    protected String idPrefix;

    public AbstractDocumentTranslationBundle(String idPrefix, DocumentReference reference,
        ComponentManager componentManager, TranslationMessageParser translationMessageParser)
        throws ComponentLookupException
    {
        this.idPrefix = idPrefix;

        this.componentManager = componentManager;
        this.bundleContext = componentManager.getInstance(TranslationBundleContext.class);
        this.serializer = componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);
        this.contextProvider = componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
        this.observation = componentManager.getInstance(ObservationManager.class);

        this.translationMessageParser = translationMessageParser;

        this.logger = LoggerFactory.getLogger(getClass());

        setPriority(DEFAULTPRIORITY_WIKI);

        setReference(reference);

        initialize();
    }

    private void initialize()
    {
        this.events =
            Arrays.<Event>asList(new DocumentUpdatedEvent(this.documentReference), new DocumentCreatedEvent(
                this.documentReference), new DocumentDeletedEvent(this.documentReference), new WikiDeletedEvent(
                this.documentReference.getWikiReference().getName()));

        this.observation.addListener(this, EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY);
    }

    protected void setReference(DocumentReference reference)
    {
        this.documentReference = reference;

        setId(this.idPrefix + this.serializer.serialize(reference));
    }

    /**
     * Gets the document that defines the translation bundle for a given locale.
     *
     * @param locale the requested locale
     * @return the document defining the translation bundle, or null if it could not be fetched yet and requires a retry
     */
    protected XWikiDocument getDocumentLocaleBundle(Locale locale) throws Exception
    {
        XWikiContext context = this.contextProvider.get();

        if (context == null) {
            // No context for some reason, let's try later.
            return null;
        }

        XWiki xwiki = context.getWiki();

        if (xwiki == null) {
            // No XWiki instance ready, let's try later.
            return null;
        }

        XWikiDocument document = xwiki.getDocument(this.documentReference, context);

        if (locale != null && !locale.equals(Locale.ROOT) && !locale.equals(document.getDefaultLocale())) {
            document = xwiki.getDocument(new DocumentReference(document.getDocumentReference(), locale), context);
        }

        return document;
    }

    protected LocalizedTranslationBundle loadDocumentLocaleBundle(Locale locale) throws Exception
    {
        XWikiDocument document = getDocumentLocaleBundle(locale);

        if (document == null) {
            // Either no context or XWiki instance not ready, let's try later.
            return null;
        }

        if (document.isNew()) {
            // No document found for this locale.
            return LocalizedTranslationBundle.EMPTY;
        }

        String content = document.getContent();

        Properties properties = new Properties();
        properties.load(new StringReader(content));

        // Convert to LocalBundle.
        DefaultLocalizedTranslationBundle localeBundle = new DefaultLocalizedTranslationBundle(this, locale);

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
        return this.translationMessageParser;
    }

    @Override
    protected LocalizedTranslationBundle createBundle(Locale locale) throws Exception
    {
        return loadDocumentLocaleBundle(locale);
    }

    @Override
    public Translation getTranslation(String key, Locale locale)
    {
        if (this.disposed) {
            return null;
        }

        return super.getTranslation(key, locale);
    }

    // DisposableCacheValue Disposable

    @Override
    public void dispose()
    {
        this.disposed = true;
        this.bundleCache.clear();
        this.observation.removeListener(getName());
    }

    // EventListener

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof WikiDeletedEvent) {
            this.bundleCache.clear();

            this.disposed = true;
        } else {
            XWikiDocument document = (XWikiDocument) source;

            this.bundleCache.remove(document.getLocale());

            if (document.getLocale().equals(Locale.ROOT)) {
                this.bundleCache.remove(document.getDefaultLocale());
            }
        }
    }

    @Override
    public String getName()
    {
        return "localization.bundle." + getId();
    }

    @Override
    public List<Event> getEvents()
    {
        return events;
    }
}
