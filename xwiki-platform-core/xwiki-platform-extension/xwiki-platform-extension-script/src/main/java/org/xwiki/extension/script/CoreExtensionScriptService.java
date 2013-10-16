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
package org.xwiki.extension.script;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.repository.CoreExtensionRepository;

/**
 * Various script APIs related to core extensions.
 * 
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Named(ExtensionManagerScriptService.ROLEHINT + '.' + CoreExtensionScriptService.ID)
@Singleton
public class CoreExtensionScriptService extends AbstractExtensionScriptService
{
    /**
     * The identifier of the sub extension {@link org.xwiki.script.service.ScriptService}.
     */
    public static final String ID = "core";

    /**
     * The repository with core modules provided by the platform.
     */
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    /**
     * @return the core extensions repository
     */
    public CoreExtensionRepository getRepository()
    {
        return safe(this.coreExtensionRepository);
    }

    /**
     * Get a list of core extensions provided by the current version of the platform.
     * 
     * @return a list of read-only handlers corresponding to the core extensions
     */
    public Collection<CoreExtension> getCoreExtensions()
    {
        return safe(this.coreExtensionRepository.getCoreExtensions());
    }

    /**
     * Get the extension handler corresponding to the given core extension ID. The returned handler can be used to get
     * more information about the extension, such as the authors, an extension description, its license...
     * 
     * @param feature the extension id or provided feature (virtual extension) of the extension to resolve
     * @return the read-only handler corresponding to the requested extension, or {@code null} if the extension isn't
     *         provided by the platform
     */
    public CoreExtension getCoreExtension(String feature)
    {
        return safe(this.coreExtensionRepository.getCoreExtension(feature));
    }
}
