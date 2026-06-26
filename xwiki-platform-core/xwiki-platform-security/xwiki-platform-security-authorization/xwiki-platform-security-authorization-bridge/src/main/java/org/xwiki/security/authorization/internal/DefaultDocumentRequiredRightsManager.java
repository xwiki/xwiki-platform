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
package org.xwiki.security.authorization.internal;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.internal.document.DocumentRequiredRightsReader;
import org.xwiki.internal.document.SimpleDocumentCache;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link DocumentRequiredRightsManager}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultDocumentRequiredRightsManager implements DocumentRequiredRightsManager
{
    /**
     * Default capacity of the required rights cache. The cache size is rather large to avoid issues on larger wikis.
     * Further, even non-existing pages can be cached, and the cached values should be mostly very small, so the
     * primary memory usage is from the keys (document references).
     */
    private static final int DEFAULT_CAPACITY = 10000;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentRequiredRightsReader documentRequiredRightsReader;

    @Inject
    private SimpleDocumentCache<Optional<DocumentRequiredRights>, AuthorizationException> cache;

    @Inject
    private Logger logger;

    private final AtomicBoolean initializing = new AtomicBoolean();

    private final AtomicBoolean initialized = new AtomicBoolean();

    @Override
    public Optional<DocumentRequiredRights> getRequiredRights(DocumentReference documentReference)
        throws AuthorizationException
    {
        if (documentReference != null) {
            // The cache is initialized on demand as initializing the cache during the initialization of the
            // components creates component loading cycles. The main problem is that the authorization manager is
            // injected before event listeners have been initialized and is also injected in event listeners, and
            // initializing the cache triggers the event listener initialization. In the event listener, another
            // instance of the required rights manager is then created. As these cycles are hard to break and might
            // also occur in extensions, this component is designed to be as safe as possible in this regard.
            if (this.initialized.getAcquire() || tryInitializeCache()) {
                return this.cache.get(documentReference.withoutLocale(), this::loadRequiredRights);
            }

            // Continue without cache if the cache failed to initialize or is currently being initialized by another
            // thread or a right check is triggered by the cache initialization itself.
            return loadRequiredRights(documentReference.withoutLocale());
        }

        return Optional.empty();
    }

    private Optional<DocumentRequiredRights> loadRequiredRights(DocumentReference documentReference)
        throws AuthorizationException
    {
        XWikiContext context = this.contextProvider.get();

        // Load the document.
        try {
            XWikiDocument document = context.getWiki().getDocument(documentReference, context);
            if (!document.isNew()) {
                return Optional.of(this.documentRequiredRightsReader.readRequiredRights(document));
            }
        } catch (XWikiException e) {
            throw new AuthorizationException("Failed to load the document", e);
        }

        return Optional.empty();
    }

    private boolean tryInitializeCache()
    {
        if (!this.initializing.compareAndExchange(false, true)) {
            try {
                if (!this.initialized.get()) {
                    LRUCacheConfiguration cacheConfiguration =
                        new LRUCacheConfiguration("platform.security.authorization.requiredrights.cache",
                            DEFAULT_CAPACITY);
                    this.cache.initializeCache(cacheConfiguration);
                    this.initialized.set(true);
                }

                // Return true if either the cache has been initialized or it had already been initialized before.
                return true;
            } catch (Exception e) {
                this.logger.warn("Failed to initialize the required rights cache: [{}]",
                    ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full stack trace of required rights cache initialization failure:", e);
            } finally {
                this.initializing.set(false);
            }
        }

        return false;
    }
}
