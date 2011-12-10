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

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;

/**
 * Default implementation of {@link LocalExtension}.
 * 
 * @version $Id$
 */
public class DefaultLocalExtension extends AbstractExtension implements LocalExtension
{
    /**
     * The namespace in which extension is installed.
     */
    private Set<String> namespaces;

    /**
     * @param repository the repository where this extension comes from
     * @param id the extension identifier
     * @param type the extension type
     */
    public DefaultLocalExtension(DefaultLocalExtensionRepository repository, ExtensionId id, String type)
    {
        super(repository, id, type);
    }

    /**
     * Create new extension descriptor by copying provided one.
     * 
     * @param repository the repository where this extension comes from
     * @param extension the extension to copy
     */
    public DefaultLocalExtension(DefaultLocalExtensionRepository repository, Extension extension)
    {
        super(repository, extension);
    }

    // LocalExtension

    @Override
    public Collection<String> getNamespaces()
    {
        return this.namespaces;
    }

    /**
     * @param namespaces the namespaces in which this extension is enabled. Null means root namespace (i.e all
     *            namespaces).
     * @see #getNamespaces()
     */
    public void setNamespaces(Collection<String> namespaces)
    {
        this.namespaces = namespaces != null ? new HashSet<String>(namespaces) : null;
    }

    /**
     * @param namespace the namespace
     * @see #getNamespaces()
     */
    public void addNamespace(String namespace)
    {
        if (this.namespaces == null) {
            this.namespaces = new HashSet<String>();
        }

        this.namespaces.add(namespace);
    }

    @Override
    public DefaultLocalExtensionFile getFile()
    {
        return (DefaultLocalExtensionFile) super.getFile();
    }

    /**
     * @param file the extension file in the filesystem
     * @see #getFile()
     */
    public void setFile(File file)
    {
        setFile(new DefaultLocalExtensionFile(file));
        putProperty(PKEY_FILE, file);
    }

    @Override
    public boolean isInstalled()
    {
        return getProperty(PKEY_INSTALLED, false);
    }

    @Override
    public boolean isInstalled(String namespace)
    {
        return isInstalled() && (this.namespaces == null || this.namespaces.contains(namespace));
    }

    /**
     * @param installed indicate if the extension is installed
     * @see #isInstalled()
     */
    public void setInstalled(boolean installed)
    {
        putProperty(PKEY_INSTALLED, installed);
    }

    /**
     * @param installed indicate if the extension is installed
     * @param namespace the namespace to look at, if null it means the extension is installed for all the namespaces
     * @see #isInstalled(String)
     */
    public void setInstalled(boolean installed, String namespace)
    {
        if (namespace == null) {
            setInstalled(installed);
            setNamespaces(null);
        } else {
            if (installed) {
                setInstalled(true);
                addNamespace(namespace);
            } else {
                if (this.namespaces != null) {
                    this.namespaces.remove(namespace);

                    if (this.namespaces.isEmpty()) {
                        setInstalled(false);
                        this.namespaces = null;
                    }
                }
            }
        }
    }

    @Override
    public boolean isDependency()
    {
        return getProperty(PKEY_DEPENDENCY, false);
    }

    /**
     * @param dependency indicate if the extension as been installed as a dependency of another one.
     * @see #isDependency()
     */
    public void setDependency(boolean dependency)
    {
        putProperty(PKEY_DEPENDENCY, dependency);
    }
}
