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
package org.xwiki.formula.internal;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.formula.AbstractFormulaRenderer;
import org.xwiki.formula.FormulaRenderer;
import org.xwiki.formula.ImageData;

import uk.ac.ed.ph.snuggletex.SnuggleEngine;
import uk.ac.ed.ph.snuggletex.SnuggleInput;
import uk.ac.ed.ph.snuggletex.SnuggleSession;
import uk.ac.ed.ph.snuggletex.WebPageOutputOptions;
import uk.ac.ed.ph.snuggletex.jeuclid.JEuclidUtilities;
import uk.ac.ed.ph.snuggletex.jeuclid.SimpleMathMLImageSavingCallback;

/**
 * Implementation of the {@link FormulaRenderer} component, which uses <a
 * href="http://snuggletex.sf.net/">SnuggleTeX</a> for generating the images corresponding to the rendered mathematical
 * formulae. The results are not as eye-pleasing as those obtained from the native TeX system, but this is a pure-java
 * solution, which doesn't depend on any external services or native commands. It could probably be tweaked to obtain
 * better results, by selecting a different font.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component("snuggletex")
public final class SnuggleTexFormulaRenderer extends AbstractFormulaRenderer
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SnuggleTexFormulaRenderer.class);

    /** The SnuggleTeX engine responsible for rendering the formulae. */
    private SnuggleEngine engine = new SnuggleEngine();

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImageData renderImage(String formula, boolean inline, FormulaRenderer.FontSize size,
        FormulaRenderer.Type type) throws IllegalArgumentException, IOException
    {
        SnuggleSession session = this.engine.createSession();

        SnuggleInput input = new SnuggleInput(wrapFormula(formula, inline));
        session.parseInput(input);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CustomMathMLImageSavingCallback callback = new CustomMathMLImageSavingCallback(output, size.getSize());

        WebPageOutputOptions options = JEuclidUtilities.createWebPageOptions(false, callback);
        session.writeWebPage(options, new NullOutputStream());

        return new ImageData(output.toByteArray(), type);
    }

    /**
     * Callback which writes the first generated image into a provided output stream.
     * 
     * @version $Id$
     */
    private class CustomMathMLImageSavingCallback extends SimpleMathMLImageSavingCallback
    {
        /** The target output. The first generated image will be pushed into this stream. */
        private OutputStream target;

        /**
         * Simple constructor which receives the {@link #target} output stream, and the default font size for the image.
         * 
         * @param target the target output, see {@link #target}
         * @param size the desired font size
         */
        public CustomMathMLImageSavingCallback(OutputStream target, int size)
        {
            this.target = target;
            setFontSize(String.valueOf(size));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public File getImageOutputFile(int mathmlCounter)
        {
            // Not used here)
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OutputStream getImageOutputStream(int mathmlCounter)
        {
            // Valid input only produces 1 image, so ignore all others
            return mathmlCounter == 0 ? this.target : null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getImageURL(int mathmlCounter)
        {
            // Not needed here as we're throwing the resulting XML away
            return "";
        }

        /**
         * {@inheritDoc}
         */
        public void imageSavingFailed(Object imageFileOrOutputStream, int mathmlCounter, String contentType,
            Throwable exception)
        {
            // Shouldn't really happen
            LOGGER.error("Can't save image", exception);
        }
    }
}
