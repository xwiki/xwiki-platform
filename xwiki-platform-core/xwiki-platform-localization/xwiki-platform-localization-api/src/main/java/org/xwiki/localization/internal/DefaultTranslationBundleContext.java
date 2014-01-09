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

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleContext;

/**
 * Default implementation of {@link TranslationBundleContext}.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
public class DefaultTranslationBundleContext implements TranslationBundleContext
{
    /**
     * The key associated to the list of bundles in the {@link ExecutionContext}.
     */
    private static final String CKEY_BUNDLES = "localization.bundles";

    /**
     * Used to access the current context.
     */
    @Inject
    private Execution execution;

    /**
     * Used to access Bundles registered as components.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    /**
     * The logger.
     */
    @Inject
    private Logger logger;

    /**
     * @return the current bundles
     */
    private SortedSet<TranslationBundle> initializeContextBundle()
    {
        SortedSet<TranslationBundle> bundles;

        try {
            bundles =
                new TreeSet<TranslationBundle>(this.componentManager.get().<TranslationBundle> getInstanceList(
                    TranslationBundle.class));
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to lookup Bundle components", e);

            bundles = new TreeSet<TranslationBundle>();
        }

        return bundles;
    }

    /**
     * @return the current bundles
     */
    private SortedSet<TranslationBundle> getBundlesInternal()
    {
        SortedSet<TranslationBundle> bundles;

        ExecutionContext context = this.execution.getContext();
        if (context != null) {
            bundles = (SortedSet<TranslationBundle>) context.getProperty(CKEY_BUNDLES);

            if (bundles == null) {
                bundles = initializeContextBundle();
                context.setProperty(CKEY_BUNDLES, bundles);
            }
        } else {
            bundles = initializeContextBundle();
        }

        return bundles;
    }

    @Override
    public Collection<TranslationBundle> getBundles()
    {
        return getBundlesInternal();
    }

    @Override
    public void addBundle(TranslationBundle bundle)
    {
        getBundlesInternal().add(bundle);
    }
}
