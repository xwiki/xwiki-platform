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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.Bundle;
import org.xwiki.localization.BundleContext;
import org.xwiki.localization.BundleDoesNotExistsException;
import org.xwiki.localization.BundleFactory;
import org.xwiki.localization.BundleFactoryDoesNotExistsException;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;

/**
 * Default implementation of the {@link LocalizationManager} component.
 * 
 * @version $Id$
 * @since 4.3M1
 */
@Component
public class DefaultLocalizationManager implements LocalizationManager
{
    /**
     * Provides access to different bundles based on their hint. Needed in {@link #use(String, String)}.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    private BundleContext bundleContext;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public Translation getTranslation(String key, Locale locale)
    {
        for (Bundle bundle : this.bundleContext.getBundles()) {
            Translation translation = bundle.getTranslation(key, locale);
            if (translation != null) {
                return translation;
            }
        }

        return null;
    }

    @Override
    public void use(String bundleType, String bundleId) throws BundleDoesNotExistsException,
        BundleFactoryDoesNotExistsException
    {
        BundleFactory bundleFactory;
        try {
            bundleFactory = this.componentManager.get().getInstance(BundleFactory.class, bundleType);
        } catch (ComponentLookupException e) {
            throw new BundleFactoryDoesNotExistsException(String.format("Failed to lookup BundleFactory for type [%s]",
                bundleType), e);
        }

        Bundle bundle = bundleFactory.getBundle(bundleId);

        this.bundleContext.addBundle(bundle);
    }
}
