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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.lf5.util.StreamUtils;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.repository.ExtensionRepository;

public class DefaultLocalExtension implements LocalExtension
{
    private File file;

    private boolean enabled = true;

    private boolean isDependency;

    private String id;

    private String version;

    private String type;

    private String description;

    private String author;

    private String website;

    private List<ExtensionDependency> dependencies = new ArrayList<ExtensionDependency>();

    private DefaultLocalExtensionRepository repository;

    public DefaultLocalExtension(DefaultLocalExtensionRepository repository, String id, String version, String type)
    {
        this.repository = repository;

        this.id = id;
        this.version = version;
        this.type = type;
    }

    public DefaultLocalExtension(DefaultLocalExtensionRepository repository, Extension extension)
    {
        this(repository, extension.getId(), extension.getVersion(), extension.getType());

        this.dependencies.addAll(extension.getDependencies());
        
        setDescription(extension.getDescription());
        setAuthor(extension.getAuthor());
        setWebsite(extension.getWebSite());
    }

    public void setFile(File file)
    {
        this.file = file;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void setDependency(boolean isDependency)
    {
        this.isDependency = isDependency;
    }

    // Extension

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public void setWebsite(String website)
    {
        this.website = website;
    }

    public void download(File file) throws ExtensionException
    {
        InputStream sourceStream = null;
        OutputStream targetStream = null;

        try {
            sourceStream = new FileInputStream(getFile());
            targetStream = new FileOutputStream(file);

            StreamUtils.copy(sourceStream, targetStream);
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

    public String getId()
    {
        return this.id;
    }

    public String getVersion()
    {
        return this.version;
    }

    public String getType()
    {
        return this.type;
    }

    public String getDescription()
    {
        return this.description;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public String getWebSite()
    {
        return this.website;
    }

    public void addDependency(ExtensionDependency dependency)
    {
        this.dependencies.add(dependency);
    }

    public List<ExtensionDependency> getDependencies()
    {
        return Collections.unmodifiableList(this.dependencies);
    }

    public ExtensionRepository getRepository()
    {
        return this.repository;
    }

    // LocalExtension

    public File getFile()
    {
        return this.file;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean isDependency()
    {
        return this.isDependency;
    }

    @Override
    public String toString()
    {
        return getId() + '-' + getVersion();
    }
}
