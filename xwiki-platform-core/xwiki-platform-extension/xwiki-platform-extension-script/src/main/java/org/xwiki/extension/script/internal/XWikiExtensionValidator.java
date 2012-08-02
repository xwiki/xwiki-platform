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
package org.xwiki.extension.script.internal;

import javax.inject.Inject;

import org.xwiki.bridge.DocumentAccessBridge;
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
public class XWikiExtensionValidator implements ExtensionValidator
{
    /**
     * Needed for checking programming rights.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public void checkInstall(Extension extension, String namespace, Request request) throws InstallException
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            String errorMessage;
            if (namespace != null) {
                errorMessage =
                    String.format("Programming right is required to install extension [%s]", extension.toString(),
                        namespace);
            } else {
                errorMessage =
                    String.format("Programming right is required to install extension [%s] on namespace [%]",
                        extension.toString(), namespace);
            }

            throw new InstallException(errorMessage);
        }
    }

    @Override
    public void checkUninstall(InstalledExtension extension, String namespace, Request request)
        throws UninstallException
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            String errorMessage;
            if (namespace != null) {
                errorMessage =
                    String.format("Programming right is required to uninstall extension [%s]", extension.toString(),
                        namespace);
            } else {
                errorMessage =
                    String.format("Programming right is required to uninstall extension [%s] on namespace [%]",
                        extension.toString(), namespace);
            }

            throw new UninstallException(errorMessage);
        }
    }
}
