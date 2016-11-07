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
package org.xwiki.extension.internal.validator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionValidator;
import org.xwiki.job.Request;

/**
 * Default right needed to install/uninstall an extension in XWiki.
 * 
 * @version $Id$
 * @since 4.2M2
 */
// The rationale for being in this module is that checking right is useless if you don't also provide public script
// service but if there is other things to put in a new xwiki-platform-extension-xwiki we might want to move it.
@Component
@Singleton
public class XWikiExtensionValidator extends AbstractExtensionValidator
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    private ExtensionValidator getExtensionValidator(String type)
    {
        ComponentManager componentManager = this.componentManagerProvider.get();
        if (componentManager.hasComponent(ExtensionValidator.class, type)) {
            try {
                return componentManager.<ExtensionValidator>getInstance(ExtensionValidator.class, type);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to get extension validator. Fallback on programming right.", e);
            }
        }

        return null;
    }

    @Override
    public void checkInstallInternal(Extension extension, String namespace, Request request) throws InstallException
    {
        // Try custom ExtensionValidator
        ExtensionValidator validator = getExtensionValidator(extension.getType());
        if (validator != null) {
            validator.checkInstall(extension, namespace, request);
        }

        // Fallback on programming right
        super.checkInstallInternal(extension, namespace, request);
    }

    @Override
    public void checkUninstallInternal(InstalledExtension extension, String namespace, Request request)
        throws UninstallException
    {
        // Try custom ExtensionValidator
        ExtensionValidator validator = getExtensionValidator(extension.getType());
        if (validator != null) {
            validator.checkUninstall(extension, namespace, request);
        }

        // Fallback on programming right
        super.checkUninstallInternal(extension, namespace, request);
    }
}
