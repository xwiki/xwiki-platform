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
package org.xwiki.officeimporter.internal.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.document.OfficeDocumentArtifact;

/**
 * A {@link OfficeDocumentArtifact} backed by a file.
 *
 * @version $Id$
 * @since 14.10.8
 * @since 15.3RC1
 */
public class FileOfficeDocumentArtifact implements OfficeDocumentArtifact
{
    private final String name;

    private final File content;

    /**
     * Construct a new {@link OfficeDocumentArtifact} backed by a file.
     *
     * @param name the name of the artifact
     * @param content the content of the artifact
     */
    public FileOfficeDocumentArtifact(String name, File content)
    {
        this.name = name;
        this.content = content;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public InputStream getContentInputStream() throws OfficeImporterException
    {
        try {
            return new FileInputStream(this.content);
        } catch (FileNotFoundException e) {
            throw new OfficeImporterException("Artifact file not found", e);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileOfficeDocumentArtifact that = (FileOfficeDocumentArtifact) o;

        return new EqualsBuilder().append(getName(), that.getName())
            .append(this.content, that.content).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(getName()).append(this.content).toHashCode();
    }
}
