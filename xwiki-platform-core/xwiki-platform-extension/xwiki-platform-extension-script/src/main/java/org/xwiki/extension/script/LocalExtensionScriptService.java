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
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.repository.LocalExtensionRepository;

/**
 * Various script APIs related to installed extensions.
 * 
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Named(ExtensionManagerScriptService.ROLEHINT + '.' + LocalExtensionScriptService.ID)
@Singleton
public class LocalExtensionScriptService extends AbstractExtensionScriptService
{
    /**
     * The identifier of the sub extension {@link org.xwiki.script.service.ScriptService}.
     */
    public static final String ID = "local";

    /**
     * The repository containing local extensions.
     */
    @Inject
    private LocalExtensionRepository localExtensionRepository;

    /**
     * @return the local extensions repository
     */
    public LocalExtensionRepository getRepository()
    {
        return safe(this.localExtensionRepository);
    }

    /**
     * Get a list of cached extensions from the local extension repository. This doesn't include core extensions, only
     * custom extensions fetched or installed.
     * 
     * @return a list of read-only handlers corresponding to the local extensions, an empty list if nothing is available
     *         in the local repository
     */
    public Collection<LocalExtension> getLocalExtensions()
    {
        return safe(this.localExtensionRepository.getLocalExtensions());
    }
}
