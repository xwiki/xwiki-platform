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

import org.xwiki.component.annotation.Role;

/**
 * Convert a LaTeX formula into an image.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Role
public interface FormulaRenderer
{
    /**
     * Encloses the supported formats for rendered mathematical formulae.
     * 
     * @version $Id$
     */
    public static enum Type
    {
        /**
         * Portable Network Graphics, a lossless, free image format. See RFC 2083 for details. Best-looking results, but
         * might not be supported by very old browsers.
         */
        PNG(".png", "image/png"),

        /** Graphics Interchange Format, a patent encumbered image format. */
        GIF(".gif", "image/gif"),

        /** Lossy image format created by Joint Photographic Experts Group. */
        JPEG(".jpg", "image/jpeg");

        /** The default rendered image format. */
        public static final Type DEFAULT = PNG;

        /** The file extension for this type. */
        private final String extension;

        /** The mimetype for this format. */
        private final String mimetype;

        /**
         * Creation of a type, specifying the corresponding file extension and mimetype.
         * 
         * @param extension the file extension for this format, starting with "."
         * @param mimetype the mimetype for this format
         */
        Type(String extension, String mimetype)
        {
            this.extension = extension;
            this.mimetype = mimetype;
        }

        /**
         * Access to the file extension corresponding to this format.
         * 
         * @return a string containing the extension, starting with ".".
         */
        public String getExtension()
        {
            return this.extension;
        }

        /**
         * Access to the mimetype corresponding to this format.
         * 
         * @return a string containing the mimetype, in the format defined in RFC 2045
         */
        public String getMimetype()
        {
            return this.mimetype;
        }
    }

    /**
     * Encloses the supported LaTeX font sizes used for displaying a mathematical formula.
     * 
     * @version $Id$
     */
    public static enum FontSize
    {
        // TODO: Check the correct pt sizes
        /** Script size. */
        TINY(10, "\\tiny"),

        /** Footnote size. */
        VERY_SMALL(12, "\\scriptsize"),

        /** Footnote size. */
        SMALLER(14, "\\footnotesize"),

        /** Small text. */
        SMALL(17, "\\small"),

        /** Normal font size. */
        NORMAL(20, "\\normalsize"),

        /** Slightly larger font size. */
        LARGE(24, "\\large"),

        /** Large font size. */
        LARGER(29, "\\Large"),

        /** Very large. */
        VERY_LARGE(35, "\\LARGE"),

        /** The largest defined font size. */
        HUGE(41, "\\huge"),

        /** The largest defined font size. */
        EXTREMELY_HUGE(50, "\\Huge");

        /** The default font size. */
        public static final FontSize DEFAULT = NORMAL;

        /** The font pt size. */
        private final int size;

        /** The corresponding LaTeX command. */
        private final String command;

        /**
         * Creation of a font size, specifying the corresponding size measured in "pt" and the LaTeX command.
         * 
         * @param size the font size in "pt"
         * @param command the LateX command that sets this font size
         */
        FontSize(int size, String command)
        {
            this.size = size;
            this.command = command;
        }

        /**
         * Access to the actual font size.
         * 
         * @return the value of the corresponding font size, in "pt"
         */
        public int getSize()
        {
            return this.size;
        }

        /**
         * Access to the LateX command that sets this font size.
         * 
         * @return a string representing the LateX command
         */
        public String getCommand()
        {
            return this.command;
        }
    }

    /**
     * Generates the image (if not already generated), stores it, and returns a key which can be used for retrieving it
     * in a subsequent request.
     * 
     * @param formula the mathematical formula to render, in LaTeX format, <em>without</em> the surrounding math-mode
     *            commands ($, begin{math}, etc.)
     * @param inline whether the formula appears inline inside the text, or as a standalone block
     * @param size the font size to use for the text
     * @param type The type of image to generate. See the {@link Type} enum for supported types. <em>Not all renderers
     *            support all types</em>.
     * @return an identifier which can be used for retrieving the image from the {@link ImageStorage storage}
     * @throws IllegalArgumentException if the LaTeX syntax of the formula is incorrect and the error is unrecoverable
     * @throws IOException in case of a renderer execution error
     */
    String process(String formula, boolean inline, FontSize size, Type type) throws IllegalArgumentException,
        IOException;

    /**
     * Retrieve the image data from the storage.
     * 
     * @param imageID the identifier for the image, returned by {@link #process(String, boolean, FontSize, Type)}
     * @return the generated {@link ImageData}, or {@code null} if no image exists with this identifier
     */
    ImageData getImage(String imageID);
}
