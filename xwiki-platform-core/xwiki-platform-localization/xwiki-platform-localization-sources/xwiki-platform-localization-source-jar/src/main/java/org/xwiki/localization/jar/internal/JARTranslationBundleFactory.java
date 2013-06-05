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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactory;

/**
 * Generate and manage resource based translations bundles.
 * 
 * @version $Id$
 * @since 4.5M1
 */
@Component
@Named(JARTranslationBundleFactory.ID)
@Singleton
public class JARTranslationBundleFactory implements TranslationBundleFactory
{
    /**
     * The identifier of this {@link TranslationBundleFactory}.
     */
    public static final String ID = "jar";

    /**
     * Used to access the right {@link ComponentManager} depending on the current context.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    /**
     * Used to log.
     */
    @Inject
    private Logger logger;

    @Override
    public TranslationBundle getBundle(String bundleId) throws TranslationBundleDoesNotExistsException
    {
        String id = ID + ':' + bundleId;

        if (this.contextComponentManagerProvider.get().hasComponent(TranslationBundle.class, id)) {
            try {
                return this.contextComponentManagerProvider.get().getInstance(TranslationBundle.class, id);
            } catch (ComponentLookupException e) {
                this.logger.debug("Failed to lookup component [{}] with hint [{}].", TranslationBundle.class, bundleId,
                    e);
            }
        }

        throw new TranslationBundleDoesNotExistsException(String.format("Can't find any JAR resource for jar [%s]",
            bundleId));
    }
}
