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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;

public class DefaultLocalExtension extends AbstractExtension implements LocalExtension
{
    private Set<String> namespaces;

    public DefaultLocalExtension(DefaultLocalExtensionRepository repository, ExtensionId id, String type)
    {
        super(repository, id, type);

    }

    public DefaultLocalExtension(DefaultLocalExtensionRepository repository, Extension extension)
    {
        super(repository, extension);
    }

    public void setFile(File file)
    {
        putProperty(PKEY_FILE, file);
    }

    public void setInstalled(boolean installed)
    {
        putProperty(PKEY_INSTALLED, installed);
    }

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

    public void setDependency(boolean dependency)
    {
        putProperty(PKEY_DEPENDENCY, dependency);
    }

    public Collection<String> getNamespaces()
    {
        return namespaces;
    }

    public void setNamespaces(Collection<String> namespaces)
    {
        this.namespaces = namespaces != null ? new HashSet<String>(namespaces) : null;
    }

    public void addNamespace(String namespace)
    {
        if (this.namespaces == null) {
            this.namespaces = new HashSet<String>();
        }

        this.namespaces.add(namespace);
    }

    // Extension

    public void download(File file) throws ExtensionException
    {
        InputStream sourceStream = null;
        OutputStream targetStream = null;

        try {
            sourceStream = new FileInputStream(getFile());
            targetStream = new FileOutputStream(file);

            IOUtils.copy(sourceStream, targetStream);
        } catch (Exception e) {
            throw new ExtensionException("Failed to copy file", e);
        } finally {
            IOException closeException = null;

            if (sourceStream != null) {
                try {
                    sourceStream.close();
                } catch (IOException e) {
                    closeException = e;
                }
            }

            if (targetStream != null) {
                try {
                    targetStream.close();
                } catch (IOException e) {
                    closeException = e;
                }
            }

            if (closeException != null) {
                throw new ExtensionException("Failed to close file", closeException);
            }
        }
    }

    // LocalExtension

    public File getFile()
    {
        return getProperty(PKEY_FILE);
    }

    public boolean isInstalled()
    {
        return getProperty(PKEY_INSTALLED, false);
    }

    public boolean isInstalled(String namespace)
    {
        return isInstalled() && (this.namespaces == null || this.namespaces.contains(namespace));
    }

    public boolean isDependency()
    {
        return getProperty(PKEY_DEPENDENCY, false);
    }
}
