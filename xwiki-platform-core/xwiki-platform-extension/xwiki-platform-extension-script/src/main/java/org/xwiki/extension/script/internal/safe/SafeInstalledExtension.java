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
package org.xwiki.extension.script.internal.safe;

import java.util.Collection;

import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;

/**
 * Provide a public script access to a installed extension.
 * 
 * @param <T> the extension type
 * @version $Id$
 * @since 4.0M2
 */
public class SafeInstalledExtension<T extends InstalledExtension> extends SafeLocalExtension<T> implements
    InstalledExtension
{
    /**
     * @param localExtension the wrapped local extension
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeInstalledExtension(T localExtension, ScriptSafeProvider<Object> safeProvider)
    {
        super(localExtension, safeProvider);
    }

    // InstalledExtension

    @Override
    public LocalExtension getLocalExtension()
    {
        return safe(getWrapped().getLocalExtension());
    }

    @Override
    public boolean isInstalled()
    {
        return getWrapped().isInstalled();
    }

    @Override
    public boolean isInstalled(String namespace)
    {
        return getWrapped().isInstalled(namespace);
    }

    @Override
    public Collection<String> getNamespaces()
    {
        return safe(getWrapped().getNamespaces());
    }

    @Override
    public boolean isDependency()
    {
        return getWrapped().isDependency();
    }
}
