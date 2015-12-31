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
package org.xwiki.filter.instance.output;

import java.io.IOException;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.extension.ExtensionFilter;
import org.xwiki.filter.event.model.WikiFilter;
import org.xwiki.filter.output.AbstractBeanOutputFilterStream;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Named(ExtensionInstanceOutputFilterStreamFactory.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ExtensionInstanceOutputFilterStream
    extends AbstractBeanOutputFilterStream<ExtensionInstanceOutputProperties> implements ExtensionFilter, WikiFilter
{
    private static final String WIKINAMESPACE = "wiki:";

    @Inject
    private LocalExtensionRepository localRepository;

    @Inject
    private ExtensionRepositoryManager extensionRepository;

    @Inject
    private InstalledExtensionRepository installedRepository;

    @Inject
    private ModelContext modelContext;

    @Inject
    private Logger logger;

    private Stack<String> currentNamespace = new Stack<>();

    @Override
    public void close() throws IOException
    {
        // Nothing to close
    }

    // Events

    private String getCurrentNamespace()
    {
        if (!this.currentNamespace.isEmpty()) {
            return this.currentNamespace.peek();
        }

        String namespace = null;

        // TODO: This makes impossible to register an extension at root level through events, should find a cleaner way
        EntityReference currentEntityReference = this.modelContext.getCurrentEntityReference();
        if (currentEntityReference != null) {
            namespace = WIKINAMESPACE + currentEntityReference.extractReference(EntityType.WIKI).getName();
        }

        return namespace;
    }

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentNamespace.push(WIKINAMESPACE + name);
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentNamespace.pop();
    }

    @Override
    public void beginNamespace(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentNamespace.push(name);
    }

    @Override
    public void endNamespace(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentNamespace.pop();
    }

    @Override
    public void beginExtension(String id, String version, FilterEventParameters parameters) throws FilterException
    {
        // TODO: add support for complete extension
    }

    @Override
    public void endExtension(String id, String version, FilterEventParameters parameters) throws FilterException
    {
        // TODO: add support for complete extension

        ExtensionId extensionId = new ExtensionId(id, version);

        try {
            LocalExtension localExtension = this.localRepository.getLocalExtension(extensionId);
            if (localExtension == null) {
                Extension extension;
                try {
                    // Try to find and download the extension from a repository
                    extension = this.extensionRepository.resolve(extensionId);
                } catch (ResolveException e) {
                    this.logger.debug("Can't find extension [{}]", extensionId, e);

                    // FIXME: Create a dummy extension. Need support for partial/lazy extension.
                    return;
                }

                localExtension = this.localRepository.storeExtension(extension);
            }

            String namespace = getCurrentNamespace();

            // Make sure it's not already there
            // TODO: should probably make it configurable
            if (installedRepository.getInstalledExtension(localExtension.getId().getId(), namespace) == null) {
                for (ExtensionId feature : localExtension.getExtensionFeatures()) {
                    if (installedRepository.getInstalledExtension(feature.getId(), namespace) != null) {
                        // Already exist so don't register it or it could create a mess
                        return;
                    }
                }
            } else {
                return;
            }

            // Register the extension as installed
            installedRepository.installExtension(localExtension, namespace, false);
        } catch (Exception e) {
            this.logger.error("Failed to register extenion [{}] from the XAR", extensionId, e);
        }
    }
}
