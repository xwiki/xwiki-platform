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

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.stability.Unstable;

/**
 * Represents an office document being imported.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public interface OfficeDocument extends Closeable
{
    /**
     * Returns the content of this office document. Content document type may vary depending on the implementation. For
     * an example, a particular implementation might return an html DOM object while another implementation might return
     * an XDOM.
     * 
     * @return content of this document.
     */
    Object getContentDocument();

    /**
     * Returns the content of this document as a string.
     * 
     * @return content of this document as a string.
     */
    String getContentAsString();

    /**
     * Returns the files corresponding to all the artifacts for this office document, except the conversion of the
     * document itself.
     * Artifacts are generated during the import operation if the original office document contains embedded
     * non-textual elements. Also, some office formats (like presentations) result in multiple output files when
     * converted into html. In this case all these output files will be considered as artifacts.
     * <p>
     * The key of the map is the artifact name.
     *
     * @return the map of artifact names to artifacts related to this office document
     * @since 14.10.8
     * @since 15.3RC1
     */
    @Unstable
    default Map<String, OfficeDocumentArtifact> getArtifactsMap()
    {
        return Collections.emptyMap();
    }

    /**
     * @return the converter result.
     * @since 13.1RC1
     */
    default OfficeConverterResult getConverterResult()
    {
        return null;
    }

    @Override
    default void close() throws IOException
    {
    }
}
