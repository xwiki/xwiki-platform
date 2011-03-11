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
package org.xwiki.extension.task.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.event.ExtensionUninstalled;
import org.xwiki.extension.handler.ExtensionHandlerManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.task.UninstallRequest;
import org.xwiki.observation.ObservationManager;

@Component("uninstall")
public class UninstallTask extends AbstractTask<UninstallRequest>
{
    @Requirement
    private LocalExtensionRepository localExtensionRepository;

    @Requirement
    private ExtensionHandlerManager extensionHandlerManager;
    
    @Requirement
    private ObservationManager observationManager;

    @Override
    protected void start() throws Exception
    {
        for (ExtensionId extensionId : getRequest().getExtensions()) {
            if (getRequest().hasNamespaces()) {
                for (String namespace : getRequest().getNamespaces()) {
                    uninstallExtension(extensionId.getId(), namespace);
                }
            } else {
                uninstallExtension(extensionId.getId(), null);
            }
        }
    }

    public void uninstallExtension(String name, String namespace) throws UninstallException
    {
        LocalExtension localExtension = this.localExtensionRepository.getInstalledExtension(name, namespace);

        if (localExtension == null) {
            throw new UninstallException("[" + name + "]: extension is not installed");
        }

        try {
            uninstallExtension(localExtension, namespace);
        } catch (Exception e) {
            throw new UninstallException("Failed to uninstall extension", e);
        }
    }

    public void uninstallExtension(LocalExtension localExtension, String namespace) throws UninstallException
    {
        if (namespace != null && localExtension.getNamespaces() != null
            && !localExtension.getNamespaces().contains(namespace)) {
            throw new UninstallException("[" + namespace + "]: extension is not installed on wiki [" + namespace + "]");
        }

        // Uninstall backward dependencies
        try {
            for (LocalExtension backardDependency : this.localExtensionRepository.getBackwardDependencies(
                localExtension.getId().getId(), namespace)) {
                uninstallExtension(backardDependency, namespace);
            }
        } catch (ResolveException e) {
            throw new UninstallException("Failed to resolve backward dependencies of extension [" + localExtension
                + "]", e);
        }

        // Unload extension
        this.extensionHandlerManager.uninstall(localExtension, namespace);

        // Remove from local repository if it's removed from all namespaces or if it was installed only on this
        // namespace
        if (namespace == null || localExtension.getNamespaces().size() == 1) {
            this.localExtensionRepository.uninstallExtension(localExtension, namespace);
        }
        
        this.observationManager.notify(new ExtensionUninstalled(localExtension.getId()), localExtension, null);
    }
}
