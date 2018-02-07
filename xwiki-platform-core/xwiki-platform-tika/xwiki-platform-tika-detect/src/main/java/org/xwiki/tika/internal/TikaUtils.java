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
package org.xwiki.tika.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.ZeroByteFileException;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide a pre-configured {@link Tika} instance.
 * 
 * @version $Id$
 * @since 10.1RC1
 */
public final class TikaUtils
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(TikaUtils.class);

    private static Tika tika;

    static {
        try {
            tika = new Tika(new TikaConfig(TikaUtils.class.getResource("/tika-config.xml")));
        } catch (Exception e) {
            LOGGER.warn("Failed to load tika configuration (default configuration will be used): {}",
                ExceptionUtils.getRootCauseMessage(e));

            tika = new Tika();
        }
    }

    private TikaUtils()
    {
        // Utility class
    }

    /**
     * @return the shared {@link Tika} instance
     */
    public static Tika getTika()
    {
        return tika;
    }

    // TODO: Remove when https://issues.apache.org/jira/browse/IO-568 is fixed (AutoCloseInputStream does not properly
    // support mark/reset)
    private static InputStream safeInputStream(InputStream stream)
    {
        if (stream instanceof AutoCloseInputStream) {
            return new BufferedInputStream(stream);
        }

        return stream;
    }

    /**
     * @see Tika#detect(File)
     * @param file the file
     * @return detected media type
     * @throws IOException if the file can not be read
     * @see #detect(Path)
     */
    public static String detect(File file) throws IOException
    {
        return tika.detect(file);
    }

    /**
     * @see Tika#detect(Path)
     * @param path the path of the file
     * @return detected media type
     * @throws IOException if the file can not be read
     */
    public static String detect(Path path) throws IOException
    {
        return tika.detect(path);
    }

    /**
     * @see Tika#detect(InputStream, String)
     * @param stream the document stream
     * @param name document name
     * @return detected media type
     * @throws IOException if the stream can not be read
     */
    public static String detect(InputStream stream, String name) throws IOException
    {
        return tika.detect(safeInputStream(stream), name);
    }

    /**
     * @see Tika#detect(InputStream)
     * @param stream the document stream
     * @return detected media type
     * @throws IOException if the stream can not be read
     */
    public static String detect(InputStream stream) throws IOException
    {
        return tika.detect(stream);
    }

    /**
     * @see Tika#detect(String)
     * @param name the file name of the document
     * @return detected media type
     */
    public static String detect(String name)
    {
        return tika.detect(name);
    }

    /**
     * @see Tika#parseToString(InputStream, Metadata)
     * @param stream the document to be parsed
     * @param metadata document metadata
     * @return extracted text content
     * @throws IOException if the document can not be read
     * @throws TikaException if the document can not be parsed
     */
    public static String parseToString(InputStream stream, Metadata metadata) throws IOException, TikaException
    {
        try {
            return tika.parseToString(safeInputStream(stream), metadata);
        } catch (ZeroByteFileException e) {
            // How is empty file an issue ?
            return "";
        }
    }

    /**
     * @see Tika#parseToString(InputStream)
     * @param stream the document to be parsed
     * @return extracted text content
     * @throws IOException if the document can not be read
     * @throws TikaException if the document can not be parsed
     */
    public static String parseToString(InputStream stream) throws IOException, TikaException
    {
        try {
            return tika.parseToString(safeInputStream(stream));
        } catch (ZeroByteFileException e) {
            // How is empty file an issue ?
            return "";
        }
    }

    /**
     * @see Tika#parseToString(Path)
     * @param path the path of the file to be parsed
     * @return extracted text content
     * @throws IOException if the file can not be read
     * @throws TikaException if the file can not be parsed
     */
    public static String parseToString(Path path) throws IOException, TikaException
    {
        try {
            return tika.parseToString(path);
        } catch (ZeroByteFileException e) {
            // How is empty file an issue ?
            return "";
        }
    }

    /**
     * @see Tika#parseToString(File)
     * @param file the file to be parsed
     * @return extracted text content
     * @throws IOException if the file can not be read
     * @throws TikaException if the file can not be parsed
     * @see #parseToString(Path)
     */
    public static String parseToString(File file) throws IOException, TikaException
    {
        try {
            return tika.parseToString(file);
        } catch (ZeroByteFileException e) {
            // How is empty file an issue ?
            return "";
        }
    }

    /**
     * @see Tika#parseToString(URL)
     * @param url the URL of the resource to be parsed
     * @return extracted text content
     * @throws IOException if the resource can not be read
     * @throws TikaException if the resource can not be parsed
     */
    public static String parseToString(URL url) throws IOException, TikaException
    {
        try {
            return tika.parseToString(url);
        } catch (ZeroByteFileException e) {
            // How is empty file an issue ?
            return "";
        }
    }
}
