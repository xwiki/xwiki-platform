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
package org.xwiki.extension.jar.internal.validator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionValidator;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.extension.jar.internal.handler.JarExtensionHandler;
import org.xwiki.job.Request;

/**
 * Check rights for webjar extensions.
 * 
 * @version $Id$
 * @since 9.0RC1
 */
@Component
@Named(JarExtensionHandler.JAR)
@Singleton
public class JarExtensionValidator extends AbstractExtensionValidator
{
    @Inject
    @Named(JarExtensionHandler.WEBJAR)
    private ExtensionValidator webjarValidator;

    private boolean isWebjar(Extension extension)
    {
        // FIXME: ideally webjar extension should have "webjar" type but it's not the case for webjar.org releases (i.e.
        // most of the webjars) so for now we assume "org.webjars:*" id means webjar
        return extension.getId().getId().startsWith("org.webjars:");
    }

    @Override
    protected void checkInstallInternal(Extension extension, String namespace, Request request) throws InstallException
    {
        if (isWebjar(extension)) {
            this.webjarValidator.checkInstall(extension, namespace, request);
        } else {
            super.checkInstallInternal(extension, namespace, request);
        }
    }

    @Override
    public void checkUninstall(InstalledExtension extension, String namespace, Request request)
        throws UninstallException
    {
        if (isWebjar(extension)) {
            this.webjarValidator.checkUninstall(extension, namespace, request);
        } else {
            super.checkUninstallInternal(extension, namespace, request);
        }
    }
}
