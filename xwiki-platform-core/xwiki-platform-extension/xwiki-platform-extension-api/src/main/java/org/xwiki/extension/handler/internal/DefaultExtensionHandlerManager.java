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
package org.xwiki.extension.handler.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionHandler;
import org.xwiki.extension.handler.ExtensionHandlerManager;

/**
 * Default implementation of {@link ExtensionHandlerManager}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultExtensionHandlerManager implements ExtensionHandlerManager
{
    /**
     * Message used when falling to find a proper extension handler.
     */
    private static final String LOOKUPERROR = "Can't find any extension handler for the extension ";

    /**
     * Use to lookup {@link ExtensionHandler} implementations.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Get the handler corresponding to the provided extension.
     * 
     * @param localExtension the extension to handler
     * @return the handler
     * @throws ComponentLookupException failed to find a proper handler for the provided extension
     */
    private ExtensionHandler getExtensionHandler(LocalExtension localExtension) throws ComponentLookupException
    {
        // Load extension
        return this.componentManager.lookup(ExtensionHandler.class, localExtension.getType().toString().toLowerCase());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.handler.ExtensionHandlerManager#install(org.xwiki.extension.LocalExtension,
     *      java.lang.String)
     */
    public void install(LocalExtension localExtension, String namespace) throws InstallException
    {
        ExtensionHandler extensionHandler;
        try {
            // Load extension
            extensionHandler = getExtensionHandler(localExtension);
        } catch (ComponentLookupException e) {
            throw new InstallException(LOOKUPERROR + '[' + localExtension + ']', e);
        }

        try {
            extensionHandler.install(localExtension, namespace);
        } catch (Exception e) {
            // TODO: cleanup

            throw new InstallException("Failed to install extension [" + localExtension.getId() + "]", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.handler.ExtensionHandlerManager#uninstall(org.xwiki.extension.LocalExtension,
     *      java.lang.String)
     */
    public void uninstall(LocalExtension localExtension, String namespace) throws UninstallException
    {
        try {
            // Load extension
            ExtensionHandler extensionHandler = getExtensionHandler(localExtension);

            extensionHandler.uninstall(localExtension, namespace);
        } catch (ComponentLookupException e) {
            throw new UninstallException(LOOKUPERROR + '[' + localExtension + ']');
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.handler.ExtensionHandlerManager#upgrade(org.xwiki.extension.LocalExtension,
     *      org.xwiki.extension.LocalExtension, java.lang.String)
     */
    public void upgrade(LocalExtension previousLocalExtension, LocalExtension newLocalExtension, String namespace)
        throws InstallException
    {
        try {
            // Load extension
            ExtensionHandler extensionInstaller = getExtensionHandler(previousLocalExtension);

            extensionInstaller.upgrade(previousLocalExtension, newLocalExtension, namespace);
        } catch (ComponentLookupException e) {
            throw new InstallException(LOOKUPERROR + '[' + newLocalExtension + ']');
        }
    }
}
