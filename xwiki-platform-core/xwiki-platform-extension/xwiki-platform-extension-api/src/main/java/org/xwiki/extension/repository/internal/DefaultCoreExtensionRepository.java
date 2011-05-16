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
package org.xwiki.extension.repository.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.AbstractExtensionRepository;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;

/**
 * Default implementation of {@link CoreExtensionRepository}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultCoreExtensionRepository extends AbstractExtensionRepository implements CoreExtensionRepository,
    Initializable
{
    /**
     * The core extensions.
     */
    protected Map<String, DefaultCoreExtension> extensions;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public DefaultCoreExtensionRepository()
    {
        super(new ExtensionRepositoryId("core", "xwiki-core", null));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        DefaultCoreExtensionScanner scanner = new DefaultCoreExtensionScanner();
        try {
            this.extensions = scanner.loadExtensions(this);
        } catch (Exception e) {
            this.logger.warn("Failed to load core extensions", e);
        }
    }

    // Repository

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepository#resolve(org.xwiki.extension.ExtensionId)
     */
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        Extension extension = getCoreExtension(extensionId.getId());

        if (extension == null
            || (extensionId.getVersion() != null && !extension.getId().getVersion().equals(extensionId.getVersion()))) {
            throw new ResolveException("Could not find extension [" + extensionId + "]");
        }

        return extension;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepository#exists(org.xwiki.extension.ExtensionId)
     */
    public boolean exists(ExtensionId extensionId)
    {
        Extension extension = getCoreExtension(extensionId.getId());

        if (extension == null
            || (extensionId.getVersion() != null && !extension.getId().getVersion().equals(extensionId.getVersion()))) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.CoreExtensionRepository#exists(java.lang.String)
     */
    public boolean exists(String id)
    {
        return this.extensions.containsKey(id);
    }

    // CoreExtensionRepository

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.CoreExtensionRepository#countExtensions()
     */
    public int countExtensions()
    {
        return this.extensions.size();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.extension.repository.CoreExtensionRepository#getCoreExtensions()
     */
    public Collection<CoreExtension> getCoreExtensions()
    {
        return new ArrayList<CoreExtension>(this.extensions.values());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.CoreExtensionRepository#getCoreExtension(java.lang.String)
     */
    public CoreExtension getCoreExtension(String id)
    {
        return this.extensions.get(id);
    }
}
