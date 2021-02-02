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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.stability.Unstable;
import org.xwiki.xml.html.HTMLUtils;

/**
 * A {@link OfficeDocument} backed by a w3c {@link Document} containing xhtml markup.
 *
 * @version $Id$
 * @since 2.1M1
 */
public class XHTMLOfficeDocument implements OfficeDocument
{
    /**
     * Document holding xhtml corresponding to office document content.
     */
    private Document document;

    /**
     * Artifacts for this office document.
     */
    private Set<File> artifactFiles;

    private OfficeConverterResult converterResult;

    @Deprecated
    private Map<String, byte[]> artifacts;

    /**
     * Creates a new {@link XHTMLOfficeDocument}.
     * 
     * @param document the w3c dom representing the office document.
     * @param artifacts artifacts for this office document.
     * @deprecated Since 13.1RC1 use {@link #XHTMLOfficeDocument(Document, Set, OfficeConverterResult)}.
     */
    @Deprecated
    public XHTMLOfficeDocument(Document document, Map<String, byte[]> artifacts)
    {
        this.document = document;
        this.artifacts = artifacts;
        this.artifactFiles = Collections.emptySet();
    }

    /**
     * Creates a new {@link XHTMLOfficeDocument}.
     *
     * @param document the w3c dom representing the office document.
     * @param artifactFiles artifacts for this office document.
     * @param converterResult the {@link OfficeConverterResult} used to build that object.
     * @since 13.1RC1
     */
    @Unstable
    public XHTMLOfficeDocument(Document document, Set<File> artifactFiles, OfficeConverterResult converterResult)
    {
        this.document = document;
        this.artifactFiles = artifactFiles;
        this.converterResult = converterResult;
    }

    @Override
    public Document getContentDocument()
    {
        return this.document;
    }

    @Override
    public String getContentAsString()
    {
        return HTMLUtils.toString(this.document);
    }

    @Override
    public Map<String, byte[]> getArtifacts()
    {
        if (this.artifacts == null) {
            this.artifacts = new HashMap<>();
            FileInputStream fis = null;

            for (File file : this.artifactFiles) {
                try {
                    fis = new FileInputStream(file);
                    this.artifacts.put(file.getName(), IOUtils.toByteArray(fis));
                } catch (IOException e) {
                    // FIXME
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            }
        }
        return this.artifacts;
    }

    @Override
    public Set<File> getArtifactsFiles()
    {
        return this.artifactFiles;
    }

    @Override
    public void cleanupArtifacts()
    {
        if (this.converterResult != null) {
            this.converterResult.cleanup();
        }
    }

    @Override
    public OfficeConverterResult getConverterResult()
    {
        return this.converterResult;
    }
}
