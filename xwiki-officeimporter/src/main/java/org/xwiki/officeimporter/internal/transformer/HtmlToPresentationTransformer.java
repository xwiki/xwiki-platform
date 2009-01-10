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
package org.xwiki.officeimporter.internal.transformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.xwiki.officeimporter.OfficeImporterContext;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.transformer.DocumentTransformer;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;

/**
 * Transforms an html document (+artifacts) into an xwiki presentation.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class HtmlToPresentationTransformer extends AbstractLogEnabled implements DocumentTransformer
{
    /**
     * {@inheritDoc}
     */
    public void transform(OfficeImporterContext importerContext) throws OfficeImporterException
    {
        importerContext.setTargetDocumentSyntaxId(new Syntax(SyntaxType.XWIKI, "1.0")
            .toIdString());
        // Build the xwiki presentation.
        try {
            byte[] archive = buildArchive(importerContext.getArtifacts());
            String archiveName = "presentation.zip";
            importerContext.addAttachment(archive, archiveName);
            importerContext.setTargetDocumentContent(buildPresentationFrameCode(archiveName,
                "output.html"));
            importerContext.finalizeDocument(true);
        } catch (IOException ex) {
            getLogger().error("Error while building artifacts archive.", ex);
            throw new OfficeImporterException(ex);
        }
    }

    /**
     * Builds the zip archive required for the presentation.
     * 
     * @param artifacts Artifacts.
     * @return The zipped artifacts.
     * @throws IOException Indicates a problem with zipping.
     */
    private byte[] buildArchive(Map<String, byte[]> artifacts) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);
        for (String artifactName : artifacts.keySet()) {
            ZipEntry entry = new ZipEntry(artifactName);
            zos.putNextEntry(entry);
            zos.write(artifacts.get(artifactName));
            zos.closeEntry();
        }
        zos.close();
        return bos.toByteArray();
    }

    /**
     * Generates the code necessary to display a presentation within an iframe. For more information
     * refer to : <a>http://code.xwiki.org/xwiki/bin/view/Snippets /ViewOfficeDocumentSnippet</a>.
     * 
     * @param zipFilename Name of the zip archive containing the html form of the presentation.
     * @param index The html document from which the presentation starts from.
     * @return The resulting code snippet.
     */
    private String buildPresentationFrameCode(String zipFilename, String index)
    {
        return "#set ($url = $xwiki.zipexplorer.getFileLink($doc, \"" + zipFilename + "\", \""
            + index + "\"))\n"
            + "<iframe src=\"$url\" frameborder=0 width=800px height=600px></iframe>";
    }
}
