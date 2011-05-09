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
package org.xwiki.formula;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Requirement;

/**
 * Base class for all implementations of the {@link FormulaRenderer} component. Provides all the common functionalities
 * (caching, storage, and retrieval), so that the only responsibility of each implementation remains just to actually
 * transform the formula into an image.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public abstract class AbstractFormulaRenderer implements FormulaRenderer
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFormulaRenderer.class);

    /** A storage system for rendered images, for reuse in subsequent requests. */
    @Requirement
    private ImageStorage storage;

    /**
     * {@inheritDoc}
     * 
     * @see FormulaRenderer#process(String, boolean, FontSize, Type)
     */
    public String process(String formula, boolean inline, FontSize size, Type type) throws IllegalArgumentException,
        IOException
    {
        // Only render the image if it is not already in the cache
        String cacheKey = computeImageID(formula, inline, size, type);
        if (this.storage.get(cacheKey) == null) {
            ImageData image = renderImage(formula, inline, size, type);
            this.storage.put(cacheKey, image);
        }
        return cacheKey;
    }

    /**
     * {@inheritDoc}
     * 
     * @see FormulaRenderer#getImage(String)
     */
    public ImageData getImage(String imageID)
    {
        return this.storage.get(imageID);
    }

    /**
     * Renders a mathematical formula into an image.
     * 
     * @param formula a string representation of the formula, in LaTeX syntax, without any commands that specify the
     *            environment (such as $$ .. $$, \begin{math} ... \end{math}, etc)
     * @param inline specifies if the rendered formula will be displayed inline in the text, or as a separate block
     * @param size the font size used for displaying the formula
     * @param type the format in which the formula is rendered
     * @return the rendered image, as an {@link ImageData} instance
     * @throws IllegalArgumentException if the LaTeX syntax of the formula is incorrect and the error is unrecoverable
     * @throws IOException in case of a renderer execution error
     */
    protected abstract ImageData renderImage(String formula, boolean inline, FontSize size, Type type)
        throws IllegalArgumentException, IOException;

    /**
     * Computes the identifier under which the rendered formula will be stored for later reuse.
     * 
     * @param formula a string representation of the formula, in LaTeX syntax, without any commands that specify the
     *            environment (such as $$ .. $$, \begin{math} ... \end{math}, etc)
     * @param inline specifies if the rendered formula will be displayed inline in the text, or as a separate block
     * @param size the font size used for displaying the formula
     * @param type the format in which the formula is rendered
     * @return a string representation of the hash code for the four information items
     */
    protected String computeImageID(String formula, boolean inline, FontSize size, Type type)
    {
        // Try computing a long hash
        try {
            MessageDigest hashAlgorithm = MessageDigest.getInstance("SHA-256");
            hashAlgorithm.update(inline ? (byte) 't' : (byte) 'f');
            hashAlgorithm.update((byte) size.ordinal());
            hashAlgorithm.update((byte) type.ordinal());
            hashAlgorithm.update(formula.getBytes());
            return new String(org.apache.commons.codec.binary.Hex.encodeHex(hashAlgorithm.digest()));
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.error("No MD5 hash algorithm implementation", ex);
        } catch (NullPointerException ex) {
            LOGGER.error("Error hashing image name", ex);
        }
        // Fallback to a simple hashcode
        final int prime = 37;
        int result = 1;
        result = prime * result + formula.hashCode();
        result = prime * result + (inline ? 0 : 1);
        result = prime * result + size.hashCode();
        result = prime * result + type.hashCode();
        result = prime * result + this.getClass().getCanonicalName().hashCode();
        return result + "";
    }

    /**
     * Prepares the mathematical formula for rendering by wrapping it in the proper math environment.
     * 
     * @param formula the mathematical formula that needs to be rendered
     * @param inline a boolean that specifies if the rendered formula will be displayed inline in the text, or as a
     *            separate block
     * @return the formula, surrounded by "\begin{math}" / "\end{math}" if it is supposed to be displayed inline, and by
     *         "\begin{displaymath}" / \end{displaymath} if it should be displayed as a block.
     */
    protected String wrapFormula(String formula, boolean inline)
    {
        return (inline ? "\\begin{math}" : "\\begin{displaymath}") + "\n{ " + formula + " }\n"
            + (inline ? "\\end{math}" : "\\end{displaymath}");
    }
}
