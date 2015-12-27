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
package org.xwiki.platform.svg.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.svg.SVGUtils;
import org.xwiki.resource.temporary.TemporaryResourceReference;

/**
 * The straight-forward implementation of the {@link SVGUtils} role.
 *
 * @version $Id$
 * @since 8.0M1
 */
@Component
@Singleton
public class DefaultSVGUtils implements SVGUtils
{
    private static final String TEMP_DIR_NAME = "svg";

    private static final String RASTER_FILE_EXTENSION = ".png";

    @Inject
    private Logger logger;

    /**
     * Used to get the temporary directory.
     */
    @Inject
    private Environment environment;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentResolver;

    @Inject
    private Container container;

    @Override
    public File rasterizeToTemporaryFile(String content, int width, int height) throws IOException
    {
        File out = getTempFile(content.hashCode());
        rasterizeToFile(content, out, width, height);
        return out;
    }

    @Override
    public TemporaryResourceReference rasterizeToTemporaryResource(String content, int width, int height)
        throws IOException
    {
        return rasterizeToTemporaryResource(content, width, height, getCurrentDocument());
    }

    @Override
    public TemporaryResourceReference rasterizeToTemporaryResource(String content, int width, int height,
        DocumentReference targetContext) throws IOException
    {
        File out = getContextTempFile(content.hashCode(), targetContext);
        rasterizeToFile(content, out, width, height);
        return new TemporaryResourceReference(TEMP_DIR_NAME, out.getName(), targetContext);
    }

    @Override
    public void rasterizeToResponse(String content, int width, int height) throws IOException
    {
        if (!(this.container.getResponse() instanceof ServletResponse)) {
            return;
        }
        HttpServletResponse response = ((ServletResponse) this.container.getResponse()).getHttpServletResponse();
        File result = rasterizeToTemporaryFile(content, width, height);
        response.setContentLength((int) result.length());
        response.setContentType("image/png");
        OutputStream os = response.getOutputStream();
        FileUtils.copyFile(result, os);
        os.flush();
    }

    private void rasterizeToFile(String content, File out, int width, int height) throws IOException
    {
        if (out.exists()) {
            this.logger.debug("Reusing existing temporary raster image: {}", out.getAbsolutePath());
            return;
        }
        try (OutputStream fout = new FileOutputStream(out)) {
            this.logger.debug("Rasterizing to temp file: {}", out.getAbsolutePath());
            TranscoderInput input = new TranscoderInput(new StringReader(content));
            TranscoderOutput output = new TranscoderOutput(fout);
            rasterize(input, output, width, height);
        }
    }

    private void rasterize(TranscoderInput input, TranscoderOutput output, int width, int height)
    {
        PNGTranscoder transcoder = new PNGTranscoder();

        if (width > 0) {
            transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, new Float(width));
        }
        if (height > 0) {
            transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, new Float(height));
        }

        // Set maximum width and height to 8k to avoid DoS attacks
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_MAX_WIDTH, new Float(8192));
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_MAX_HEIGHT, new Float(8192));

        try {
            transcoder.transcode(input, output);
        } catch (TranscoderException ex) {
            this.logger.warn("Failed to rasterize SVG image: {}", ExceptionUtils.getRootCauseMessage(ex));
        }
    }

    private File getTempFile(int hashcode)
    {
        return new File(getBaseTempDir(), Math.abs(hashcode) + RASTER_FILE_EXTENSION);
    }

    private File getContextTempFile(int hashcode, DocumentReference targetContext)
    {
        return new File(getTempDir(targetContext), Math.abs(hashcode) + RASTER_FILE_EXTENSION);
    }

    private File getTempDir(DocumentReference targetContext)
    {
        File tempDir = getBaseTempDir();
        for (EntityReference ref : targetContext.getReversedReferenceChain()) {
            tempDir = new File(tempDir, ref.getName());
        }

        try {
            tempDir.mkdirs();
        } catch (Exception ex) {
            this.logger.warn("Cannot create temporary files: {}", ExceptionUtils.getRootCauseMessage(ex));
        }

        return tempDir;
    }

    private File getBaseTempDir()
    {
        File tempDir = new File(new File(this.environment.getTemporaryDirectory(), "temp"), TEMP_DIR_NAME);

        return tempDir;
    }

    private DocumentReference getCurrentDocument()
    {
        return this.currentDocumentResolver.resolve("");
    }
}
