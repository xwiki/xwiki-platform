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
package com.xpn.xwiki.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.image.ImageProcessor;
import com.xpn.xwiki.web.Utils;

/**
 * Utility APIs, available to scripting environments under the {@code util} variable.
 *
 * @version $Id$
 */
public class Util extends Api
{
    /** The internal object wrapped by this API. */
    private com.xpn.xwiki.XWiki xwiki;

    /**
     * Simple constructor, initializes a new utility API with the current {@link com.xpn.xwiki.XWikiContext context} and
     * the current global {@link com.xpn.xwiki.XWiki XWiki} object.
     *
     * @param xwiki the current global XWiki object
     * @param context the current context
     * @see Api#Api(com.xpn.xwiki.XWikiContext)
     */
    public Util(com.xpn.xwiki.XWiki xwiki, XWikiContext context)
    {
        super(context);
        this.xwiki = xwiki;
    }

    /**
     * Decodes a <code>application/x-www-form-urlencoded</code> string.
     *
     * @param text the encoded text
     * @return decoded text
     * @since 1.3 Milestone 2
     */
    public String decodeURI(String text)
    {
        return com.xpn.xwiki.util.Util.decodeURI(text, this.context);
    }

    /**
     * Creates a new {@link Date} object corresponding to the current time. This is useful from Velocity since new
     * objects cannot be created.
     *
     * @return the current date
     * @since 1.3 Milestone 2
     * @deprecated use <code>$datetool.date</code> instead
     */
    @Deprecated
    public Date getDate()
    {
        return new Date();
    }

    /**
     * Creates a new {@link Date} object corresponding to the specified time. This is useful from Velocity since new
     * objects cannot be created.
     *
     * @param time time in milliseconds since 1970, 00:00:00 GMT
     * @return Date a date from a time in milliseconds since 01/01/1970 as a Java {@link Date} Object
     * @since 1.3 Milestone 2
     * @deprecated use <code>$datetool.toDate(time)</code> instead
     */
    @Deprecated
    public Date getDate(long time)
    {
        return new Date(time);
    }

    /**
     * Compute the elapsed time, in milliseconds, since the specified unix-epoch timestamp. This is useful from Velocity
     * since new objects cannot be created.
     *
     * @param time the time in milliseconds
     * @return the time delta in milliseconds between the current date and the time passed as parameter
     * @since 1.3 Milestone 2
     */
    public int getTimeDelta(long time)
    {
        return this.xwiki.getTimeDelta(time);
    }

    /**
     * Get a stack trace as a String.
     *
     * @param e the exception to convert to a String
     * @return the exception stack trace as a String
     * @since 1.3 Milestone 2
     */
    public String printStrackTrace(Throwable e)
    {
        return this.xwiki.printStrackTrace(e);
    }

    /**
     * Generate a random string, containing only alpha-numeric characters.
     *
     * @param size the desired size of the string
     * @return the randomly generated string
     * @since 1.3 Milestone 2
     */
    public String generateRandomString(int size)
    {
        return this.xwiki.generateRandomString(size);
    }

    /**
     * Output a BufferedImage object into the response outputstream. Once this method has been called, no further action
     * is possible. Users should set {@code $xcontext.setFinished(true)} to avoid template output. The image is served
     * as image/jpeg.
     *
     * @param image the BufferedImage to output
     * @throws java.io.IOException if the output fails
     * @since 1.3 Milestone 2
     */
    public void outputImage(BufferedImage image) throws IOException
    {
        OutputStream ostream = getXWikiContext().getResponse().getOutputStream();
        Utils.getComponent(ImageProcessor.class).writeImage(image, "image/jpeg", (float) 0.8, ostream);
        ostream.flush();
    }

    /**
     * Get a Null value. This is useful in Velocity where there is no real {@code null} object for comparisons.
     *
     * @return a {@code null} Object
     * @since 1.3 Milestone 2
     */
    public Object getNull()
    {
        return null;
    }

    /**
     * Get a New Line character. This is useful in Velocity where there is no real new line character for inclusion in
     * texts.
     *
     * @return a new line character
     * @since 1.3 Milestone 2
     */
    public String getNewline()
    {
        return "\n";
    }

    /**
     * Convert a {@code String} to a {@code Boolean} object.
     *
     * @param str the String containing the boolean representation to be parsed
     * @return the boolean represented by the string argument, {@code false} if the string is not representing a boolean
     * @since 1.8 Milestone 2
     */
    public Boolean parseBoolean(String str)
    {
        return Boolean.parseBoolean(str);
    }

    /**
     * Replace all accented characters by their ASCII equivalent.
     *
     * @param text the text to parse
     * @return a string with accents replaced with their alpha equivalent
     * @since 1.3 Milestone 2
     */
    public String clearAccents(String text)
    {
        return com.xpn.xwiki.util.Util.noaccents(text);
    }

    /**
     * Cleans up the passed text by removing all accents and special characters to make it a valid page name.
     *
     * @param documentName the document name to normalize
     * @return the equivalent valid document name
     * @since 1.3 Milestone 2
     */
    public String clearName(String documentName)
    {
        return this.xwiki.clearName(documentName, getXWikiContext());
    }

    /**
     * Removes all non alpha numerical characters from the passed text. First tries to convert accented chars to their
     * ASCII representation. Then it removes all the remaining non-alphanumeric non-ASCII characters.
     *
     * @param text the text to convert
     * @return the alpha numeric equivalent
     * @since 1.3 Milestone 2
     */
    public String convertToAlphaNumeric(String text)
    {
        return com.xpn.xwiki.util.Util.convertToAlphaNumeric(text);
    }
}
