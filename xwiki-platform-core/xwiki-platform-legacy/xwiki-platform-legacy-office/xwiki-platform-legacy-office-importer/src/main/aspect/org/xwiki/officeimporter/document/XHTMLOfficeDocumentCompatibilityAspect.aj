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
package org.xwiki.officeimporter.document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.officeimporter.internal.document.ByteArrayOfficeDocumentArtifact;
import org.xwiki.officeimporter.internal.document.FileOfficeDocumentArtifact;

public privileged aspect XHTMLOfficeDocumentCompatibilityAspect
{
    @Deprecated
    private Map<String, byte[]> XHTMLOfficeDocument.artifacts;

    @Deprecated
    private Set<File> XHTMLOfficeDocument.fileArtifacts;

    /**
     * Creates a new {@link XHTMLOfficeDocument}.
     *
     * @param document the w3c dom representing the office document.
     * @param artifacts artifacts for this office document.
     * @deprecated Since 13.1RC1 use {@link #XHTMLOfficeDocument(Document, Set, OfficeConverterResult)}.
     */
    @Deprecated
    public XHTMLOfficeDocument.new(Document document, Map<String, byte[]> artifacts)
    {
        this(document, artifacts.entrySet().stream()
                .map(entry -> new ByteArrayOfficeDocumentArtifact(entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(ByteArrayOfficeDocumentArtifact::getName, Function.identity())),
            null);
        this.artifacts = artifacts;
    }

    /**
     * Creates a new {@link XHTMLOfficeDocument}.
     *
     * @param document the w3c dom representing the office document.
     * @param artifactFiles artifacts for this office document.
     * @param converterResult the {@link OfficeConverterResult} used to build that object.
     * @since 13.1RC1
     * @deprecated Use {@link #XHTMLOfficeDocument(Document, Map, OfficeConverterResult)} instead.
     */
    @Deprecated(since = "14.10.8, 15.3RC1")
    public XHTMLOfficeDocument.new(Document document, Set<File> artifactFiles, OfficeConverterResult converterResult)
    {
        this(document, artifactFiles.stream().collect(Collectors.toMap(File::getName,
                file -> new FileOfficeDocumentArtifact(file.getName(), file))),
            converterResult);
        this.fileArtifacts = artifactFiles;
    }

    /**
     * Overrides {@link CompatibilityOfficeDocument#getArtifacts()}.
     */
    @Deprecated
    public Map<String, byte[]> XHTMLOfficeDocument.getArtifacts()
    {
        if (this.artifacts == null) {
            this.artifacts = new HashMap<>();
            for (Map.Entry<String, OfficeDocumentArtifact> mapItem : this.artifactsMap.entrySet()) {
                OfficeDocumentArtifact artifact = mapItem.getValue();
                String fileName = mapItem.getKey();
                try (InputStream is = artifact.getContentInputStream()) {
                    this.artifacts.put(fileName, IOUtils.toByteArray(is));
                } catch (OfficeImporterException | IOException e) {
                    // FIXME
                    e.printStackTrace();
                }
            }
        }
        return this.artifacts;
    }

    /**
     * Overrides {@link CompatibilityOfficeDocument#getArtifactsFiles()}.
     */
    @Deprecated(since = "14.10.8, 15.3RC1")
    public Set<File> XHTMLOfficeDocument.getArtifactsFiles()
    {
        return this.fileArtifacts != null ? this.fileArtifacts : Collections.emptySet();
    }
}
