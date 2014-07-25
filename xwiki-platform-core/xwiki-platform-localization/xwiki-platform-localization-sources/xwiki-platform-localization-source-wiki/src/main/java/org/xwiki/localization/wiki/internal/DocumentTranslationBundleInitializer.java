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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Trigger existing translations bundles initialization at startup.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named(DocumentTranslationBundleInitializer.NAME)
@Singleton
public class DocumentTranslationBundleInitializer implements EventListener
{
    /**
     * The name of the event listener.
     */
    protected static final String NAME = "localization.bundle.DocumentTranslationBundleInitializer";

    /**
     * The events to listen.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new ApplicationReadyEvent());

    /**
     * Lazily loaded to avoid dependency issue (default BundleFactory depends on
     * {@link org.xwiki.observation.ObservationManager}).
     */
    @Inject
    @Named(DocumentTranslationBundleFactory.ID)
    private Provider<TranslationBundleFactory> bundleFactoryProvider;

    /**
     * Used to log issues.
     */
    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void onEvent(Event arg0, Object arg1, Object arg2)
    {
        // Start DocumentBundleFactory initialization
        // TODO: do something cleaner;
        this.bundleFactoryProvider.get();
    }
}
