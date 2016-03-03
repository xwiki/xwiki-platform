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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;

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
    public static final String CKEY_BUNDLES = "localization.bundles";

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
    private Provider<ComponentManager> componentManagerProvider;

    /**
     * The logger.
     */
    @Inject
    private Logger logger;

    @Inject
    private ModelContext modelContext;

    private SortedSet<TranslationBundle> initializeCurrentBundles()
    {
        SortedSet<TranslationBundle> currentBundles = new TreeSet<>();

        try {
            ComponentManager componentManager = this.componentManagerProvider.get();
            List<TranslationBundle> availableBundles =
                componentManager.<TranslationBundle>getInstanceList(TranslationBundle.class);
            currentBundles.addAll(availableBundles);
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to lookup Bundle components", e);
        }

        return currentBundles;
    }

    /**
     * @return the current bundles
     */
    private Map<String, SortedSet<TranslationBundle>> getBundlesInternal()
    {
        Map<String, SortedSet<TranslationBundle>> bundles;

        ExecutionContext context = this.execution.getContext();
        if (context != null) {
            bundles = (Map<String, SortedSet<TranslationBundle>>) context.getProperty(CKEY_BUNDLES);

            if (bundles == null) {
                // Register the Execution Context property with an empty map that will be populated for each wiki.
                bundles = new HashMap<>();
                context.newProperty(CKEY_BUNDLES).inherited().cloneValue().initial(bundles).declare();
            }
        } else {
            bundles = new HashMap<>();
        }

        return bundles;
    }

    private SortedSet<TranslationBundle> getCurrentBundlesInternal()
    {
        String currentWiki = getCurrentWiki();
        Map<String, SortedSet<TranslationBundle>> bundlesMap = getBundlesInternal();
        SortedSet<TranslationBundle> currentBundles = bundlesMap.get(currentWiki);

        if (currentBundles == null) {
            // The context wiki has changed, initialize the bundles for the new current wiki.
            currentBundles = initializeCurrentBundles();
            bundlesMap.put(currentWiki, currentBundles);
        }

        return currentBundles;
    }

    @Override
    public Collection<TranslationBundle> getBundles()
    {
        return getCurrentBundlesInternal();
    }

    @Override
    public void addBundle(TranslationBundle bundle)
    {
        // Add the bundle to the current wiki's bundles. This makes sure that onDemand bundles are visible/isolated to
        // the wiki they were demanded from (i.e. displaying a document from another wiki that includes an onDemand
        // bundle will not affect the bundles of the wiki of the calling document, when the display finishes, so they
        // will be properly isolated. This is valid the other way around as well.)
        getCurrentBundlesInternal().add(bundle);
    }

    private String getCurrentWiki()
    {
        // If, for some reason the current wiki is not set, avoid a NPE by using an empty string as key in the bundles
        // map.
        String currentWiki = "";

        EntityReference currentReference = modelContext.getCurrentEntityReference();
        if (currentReference != null) {
            EntityReference wikiReference = currentReference.extractReference(EntityType.WIKI);
            if (wikiReference != null) {
                currentWiki = wikiReference.getName();
            }
        }

        return currentWiki;
    }
}
