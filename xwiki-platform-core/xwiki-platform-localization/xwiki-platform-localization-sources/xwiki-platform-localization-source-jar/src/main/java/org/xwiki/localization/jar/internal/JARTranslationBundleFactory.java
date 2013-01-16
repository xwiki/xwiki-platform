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
package org.xwiki.localization.jar.internal;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.management.Query;

import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

/**
 * Generate and manage resource based translations bundles.
 * 
 * @version $Id$
 * @since 4.5M1
 */
@Component
@Named("resource")
@Singleton
public class JARTranslationBundleFactory implements TranslationBundleFactory, Initializable, Disposable
{
    public final static String ID = "jar";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private ObservationManager observation;

    @Inject
    @Named("messagetool/1.0")
    private TranslationMessageParser translationParser;

    @Inject
    private ComponentManagerManager cmManager;

    @Inject
    private Logger logger;

    private Cache<TranslationBundle> bundlesCache;

    private EventListener listener = new EventListener()
    {
        @Override
        public void onEvent(Event event, Object arg1, Object arg2)
        {
            XWikiDocument document = (XWikiDocument) arg1;

            if (event instanceof XObjectAddedEvent) {
                translationObjectAdded(document);
            } else if (event instanceof XObjectDeletedEvent) {
                translationObjectDeleted(document);
            } else {
                translationObjectUpdated(document);
            }
        }

        @Override
        public String getName()
        {
            return "localization.bundle.resource";
        }

        @Override
        public List<Event> getEvents()
        {
            return EVENTS;
        }
    };

    @Override
    public void initialize() throws InitializationException
    {
        // Load core resources
    }

    @Override
    public TranslationBundle getBundle(String bundleId) throws TranslationBundleDoesNotExistsException
    {
        String id = ID + ':' + bundleId;

        if (this.componentManagerProvider.get().hasComponent(TranslationBundle.class, id)) {
            try {
                return this.componentManagerProvider.get().getInstance(TranslationBundle.class, id);
            } catch (ComponentLookupException e) {
                this.logger.debug("Failed to lookup component [{}] with hint [{}].", TranslationBundle.class, bundleId,
                    e);
            }
        }

        throw new TranslationBundleDoesNotExistsException(String.format("Can't find any JAR resource for jar [%s]",
            bundleId));
    }

    /**
     * @param documentReference the translation document reference
     * @return the component descriptor to use to register/unregister the translation bundle
     */
    private ComponentDescriptor<TranslationBundle> createComponentDescriptor(URL jarURL)
    {
        DefaultComponentDescriptor<TranslationBundle> descriptor = new DefaultComponentDescriptor<TranslationBundle>();

        descriptor.setImplementation(JARFileTranslationBundle.class);
        descriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        descriptor.setRoleHint(ID + ':' + jarURL);
        descriptor.setRoleType(TranslationBundle.class);

        return descriptor;
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.observation.removeListener(this.listener.getName());
    }
}
