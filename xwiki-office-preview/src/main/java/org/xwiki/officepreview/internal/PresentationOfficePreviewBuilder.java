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
package org.xwiki.officepreview.internal;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;

/**
 * Implementation of {@link org.xwiki.officepreview.OfficePreviewBuilder} which is responsible for building previews of
 * office presentations.
 * 
 * @since 2.5M2
 * @version $Id$
 */
@Component("presentation")
public class PresentationOfficePreviewBuilder extends AbstractOfficePreviewBuilder
{
    /**
     * Name of the office converter output file.
     */
    private static final String OUTPUT_FILE_NAME = "output.html";

    /**
     * Used for converting presentation files.
     */
    @Requirement
    private OpenOfficeManager officeManager;

    /**
     * Used to build the presentation XDOM.
     */
    @Requirement("xwiki/2.0")
    private Parser xwiki20Parser;

    /**
     * Used to transform the XDOM.
     */
    @Requirement
    private TransformationManager transformationManager;

    /**
     * {@inheritDoc}
     */
    protected OfficeDocumentPreview build(AttachmentReference attachmentReference, String version, InputStream data,
        Map<String, String> parameters) throws Exception
    {
        Map<String, InputStream> inputs = Collections.singletonMap(attachmentReference.getName(), data);
        Map<String, byte[]> artifacts =
            officeManager.getConverter().convert(inputs, attachmentReference.getName(), OUTPUT_FILE_NAME);
        Set<File> temporaryFiles = new HashSet<File>();
        for (Map.Entry<String, byte[]> entry : artifacts.entrySet()) {
            try {
                temporaryFiles.add(saveTemporaryFile(attachmentReference, entry.getKey(), entry.getValue()));
            } catch (Exception ex) {
                String message = "Error while saving temporary file [%s] for presentation preview [%s].";
                getLogger().error(String.format(message, entry.getKey()), attachmentReference.getName(), ex);
            }
        }
        String firstSlideURL = buildURL(attachmentReference, OUTPUT_FILE_NAME);
        XDOM presentationXDOM = buildPresentationXDOM(firstSlideURL);
        return new OfficeDocumentPreview(attachmentReference, version, presentationXDOM, temporaryFiles);
    }

    /**
     * Builds a presentation XDOM which simply contains an {@code html} macro that outputs an in-line frame that points
     * to the specified slide.
     * 
     * @param firstSlideURL URL of the first slide to which this presentation should link to
     * @return XDOM containing a {@code html} macro block which outputs an in-line frame displaying the presentation
     * @throws Exception if an error occurs while transforming the XDOM
     */
    private XDOM buildPresentationXDOM(String firstSlideURL) throws Exception
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{{html wiki=\"false\" clean=\"false\"}}");
        buffer.append(String.format("<iframe src=\"%s\" frameborder=0 width=800px height=600px></iframe>",
            firstSlideURL));
        buffer.append("{{/html}}");
        XDOM xdom = this.xwiki20Parser.parse(new StringReader(buffer.toString()));

        // Transform XDOM
        TransformationContext context = new TransformationContext();
        context.setXDOM(xdom);
        context.setSyntax(Syntax.XWIKI_2_0);
        transformationManager.performTransformations(xdom, context);
        return xdom;
    }
}
