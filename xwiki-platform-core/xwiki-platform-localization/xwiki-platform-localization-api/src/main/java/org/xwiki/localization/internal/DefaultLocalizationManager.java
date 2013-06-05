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
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleContext;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.TranslationBundleFactoryDoesNotExistsException;

/**
 * Default implementation of the {@link LocalizationManager} component.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
public class DefaultLocalizationManager implements LocalizationManager
{
    /**
     * Provides access to different bundles based on their hint. Needed in {@link #use(String, String)}.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    /**
     * Used to access the current bundles.
     */
    @Inject
    private TranslationBundleContext bundleContext;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public Translation getTranslation(String key, Locale locale)
    {
        for (TranslationBundle bundle : this.bundleContext.getBundles()) {
            Translation translation = bundle.getTranslation(key, locale);
            if (translation != null) {
                return translation;
            }
        }

        return null;
    }

    @Override
    public TranslationBundle getTranslationBundle(String bundleType, String bundleId)
        throws TranslationBundleDoesNotExistsException, TranslationBundleFactoryDoesNotExistsException

    {
        if (this.componentManagerProvider.get().hasComponent(TranslationBundle.class, bundleType + ':' + bundleId)) {
            try {
                return this.componentManagerProvider.get().<TranslationBundle> getInstance(TranslationBundle.class,
                    bundleType + ':' + bundleId);
            } catch (ComponentLookupException e) {
                // Shoul never happen since we test it before
            }
        }

        TranslationBundleFactory bundleFactory;
        try {
            bundleFactory = this.componentManagerProvider.get().getInstance(TranslationBundleFactory.class, bundleType);
        } catch (ComponentLookupException e) {
            throw new TranslationBundleFactoryDoesNotExistsException(String.format(
                "Failed to lookup BundleFactory for type [%s]", bundleType), e);
        }

        return bundleFactory.getBundle(bundleId);
    }

    @Override
    public void use(String bundleType, String bundleId) throws TranslationBundleDoesNotExistsException,
        TranslationBundleFactoryDoesNotExistsException
    {
        TranslationBundle bundle = getTranslationBundle(bundleType, bundleId);

        this.bundleContext.addBundle(bundle);
    }
}
