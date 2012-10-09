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
import java.util.PriorityQueue;

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
import org.xwiki.localization.Bundle;
import org.xwiki.localization.BundleContext;

@Component
@Singleton
// TODO: store directly a key/translation Map instead of an ordered list of bundles ?
public class DefaultBundleContext implements BundleContext
{
    private static final String CKEY_BUNDLES = "localization.bundles";

    @Inject
    private Execution execution;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    private Logger logger;

    private PriorityQueue<Bundle> initializeContextBundle()
    {
        PriorityQueue<Bundle> bundles;

        try {
            bundles = new PriorityQueue<Bundle>(this.componentManager.get().<Bundle> getInstanceList(Bundle.class));
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to lookup Bundle components", e);

            bundles = new PriorityQueue<Bundle>();
        }

        return bundles;
    }

    private PriorityQueue<Bundle> getBundlesInternal()
    {
        PriorityQueue<Bundle> bundles;

        ExecutionContext context = this.execution.getContext();
        if (context != null) {
            bundles = (PriorityQueue<Bundle>) context.getProperty(CKEY_BUNDLES);
        } else {
            bundles = initializeContextBundle();
        }

        return bundles;
    }

    @Override
    public Collection<Bundle> getBundles()
    {
        return getBundlesInternal();
    }

    @Override
    public void addBundle(Bundle bundle)
    {
        getBundlesInternal().add(bundle);
    }
}
