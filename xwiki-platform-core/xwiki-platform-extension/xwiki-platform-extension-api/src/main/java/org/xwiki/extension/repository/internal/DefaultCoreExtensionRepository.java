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
     * Used to scan jars to find extensions.
     */
    @Inject
    private CoreExtensionScanner scanner;

    /**
     * Default constructor.
     */
    public DefaultCoreExtensionRepository()
    {
        super(new ExtensionRepositoryId("core", "xwiki-core", null));
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.extensions = this.scanner.loadExtensions(this);
        } catch (Exception e) {
            this.logger.warn("Failed to load core extensions", e);
        }
    }

    // Repository

    @Override
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        Extension extension = getCoreExtension(extensionId.getId());

        if (extension == null
            || (extensionId.getVersion() != null && !extension.getId().getVersion().equals(extensionId.getVersion()))) {
            throw new ResolveException("Could not find extension [" + extensionId + "]");
        }

        return extension;
    }

    @Override
    public boolean exists(ExtensionId extensionId)
    {
        Extension extension = getCoreExtension(extensionId.getId());

        if (extension == null
            || (extensionId.getVersion() != null && !extension.getId().getVersion().equals(extensionId.getVersion()))) {
            return false;
        }

        return true;
    }

    @Override
    public boolean exists(String id)
    {
        return this.extensions.containsKey(id);
    }

    // CoreExtensionRepository

    @Override
    public int countExtensions()
    {
        return this.extensions.size();
    }

    @Override
    public Collection<CoreExtension> getCoreExtensions()
    {
        return new ArrayList<CoreExtension>(this.extensions.values());
    }

    @Override
    public CoreExtension getCoreExtension(String id)
    {
        return this.extensions.get(id);
    }
}
