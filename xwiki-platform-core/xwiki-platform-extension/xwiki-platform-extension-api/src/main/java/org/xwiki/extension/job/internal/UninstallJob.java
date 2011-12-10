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
package org.xwiki.extension.job.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.handler.ExtensionHandlerManager;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.repository.LocalExtensionRepository;

/**
 * Extension uninstallation related task.
 * <p>
 * This task generates related events.
 * 
 * @version $Id$
 */
@Component
@Named("uninstall")
public class UninstallJob extends AbstractJob<UninstallRequest>
{
    /**
     * Error message used in exception throw when trying to uninstall an extension which is not installed.
     */
    private static final String ERROR_NOTINSTALLED = "Extension [{0}] is not installed";

    /**
     * Error message used in exception throw when trying to uninstall an extension which is not installed.
     */
    private static final String ERROR_NOTINSTALLEDNAMESPACE = ERROR_NOTINSTALLED + " on namespace [{1}]";

    /**
     * Used to manipulate local repository.
     */
    @Inject
    private LocalExtensionRepository localExtensionRepository;

    /**
     * Used to uninstall extensions.
     */
    @Inject
    private ExtensionHandlerManager extensionHandlerManager;

    @Override
    protected void start() throws Exception
    {
        List<ExtensionId> extensions = getRequest().getExtensions();

        notifyPushLevelProgress(extensions.size());

        try {
            for (ExtensionId extensionId : extensions) {
                if (extensionId.getVersion() != null) {
                    LocalExtension localExtension = (LocalExtension) this.localExtensionRepository.resolve(extensionId);

                    if (getRequest().hasNamespaces()) {
                        uninstallExtension(localExtension, getRequest().getNamespaces());
                    } else if (localExtension.getNamespaces() != null) {
                        // Duplicate the namespace list to avoid ConcurrentModificationException
                        uninstallExtension(localExtension, new ArrayList<String>(localExtension.getNamespaces()));
                    } else {
                        uninstallExtension(localExtension, (String) null);
                    }
                } else {
                    if (getRequest().hasNamespaces()) {
                        uninstallExtension(extensionId.getId(), getRequest().getNamespaces());
                    } else {
                        uninstallExtension(extensionId.getId(), (String) null);
                    }
                }

                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * @param extensionId the identifier of the extension to uninstall
     * @param namespaces the namespaces from where to uninstall the extension
     * @throws UninstallException error when trying to uninstall provided extensions
     */
    private void uninstallExtension(String extensionId, Collection<String> namespaces) throws UninstallException
    {
        notifyPushLevelProgress(namespaces.size());

        try {
            for (String namespace : namespaces) {
                uninstallExtension(extensionId, namespace);

                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * @param extensionId the identifier of the extension to uninstall
     * @param namespace the namespace from where to uninstall the extension
     * @throws UninstallException error when trying to uninstall provided extension
     */
    private void uninstallExtension(String extensionId, String namespace) throws UninstallException
    {
        LocalExtension localExtension = this.localExtensionRepository.getInstalledExtension(extensionId, namespace);

        if (localExtension == null) {
            throw new UninstallException(MessageFormat.format(ERROR_NOTINSTALLED, extensionId));
        }

        try {
            uninstallExtension(localExtension, namespace);
        } catch (Exception e) {
            throw new UninstallException("Failed to uninstall extension", e);
        }
    }

    /**
     * @param localExtension the extension to uninstall
     * @param namespaces the namespaces from where to uninstall the extension
     * @throws UninstallException error when trying to uninstall provided extension
     */
    private void uninstallExtension(LocalExtension localExtension, Collection<String> namespaces)
        throws UninstallException
    {
        for (String namespace : namespaces) {
            uninstallExtension(localExtension, namespace);
        }
    }

    /**
     * @param extensions the local extensions to uninstall
     * @param namespace the namespaces from where to uninstall the extensions
     * @throws UninstallException error when trying to uninstall provided extensions
     */
    private void uninstallExtensions(Collection<LocalExtension> extensions, String namespace) throws UninstallException
    {
        for (LocalExtension backardDependency : extensions) {
            uninstallExtension(backardDependency, namespace);
        }
    }

    /**
     * @param localExtension the extension to uninstall
     * @param namespace the namespace from where to uninstall the extension
     * @throws UninstallException error when trying to uninstall provided extension
     */
    private void uninstallExtension(LocalExtension localExtension, String namespace) throws UninstallException
    {
        if (!localExtension.isInstalled()) {
            throw new UninstallException(MessageFormat.format(ERROR_NOTINSTALLED, localExtension, namespace));
        } else if (namespace != null
            && (localExtension.getNamespaces() == null || !localExtension.getNamespaces().contains(namespace))) {
            throw new UninstallException(MessageFormat.format(ERROR_NOTINSTALLEDNAMESPACE, localExtension, namespace));
        }

        // Log progression
        if (namespace != null) {
            this.logger.info("Uninstalling extension [{}] from namespace [{}]", localExtension, namespace);
        } else {
            this.logger.info("Uninstalling extension [{}]", localExtension);
        }

        notifyPushLevelProgress(3);

        try {
            // Uninstall backward dependencies
            try {
                if (namespace != null) {
                    uninstallExtensions(this.localExtensionRepository.getBackwardDependencies(localExtension.getId()
                        .getId(), namespace), namespace);
                } else {
                    for (Map.Entry<String, Collection<LocalExtension>> entry : this.localExtensionRepository
                        .getBackwardDependencies(localExtension.getId()).entrySet()) {
                        uninstallExtensions(entry.getValue(), entry.getKey());
                    }
                }
            } catch (ResolveException e) {
                throw new UninstallException("Failed to resolve backward dependencies of extension [" + localExtension
                    + "]", e);
            }

            notifyStepPropress();

            // Unload extension
            this.extensionHandlerManager.uninstall(localExtension, namespace);

            notifyStepPropress();

            // Remove from local repository if it's removed from all namespaces or if it was installed only on this
            // namespace
            this.localExtensionRepository.uninstallExtension(localExtension, namespace);
        } finally {
            notifyPopLevelProgress();
        }

        this.observationManager.notify(new ExtensionUninstalledEvent(localExtension.getId()), localExtension, null);
    }
}
