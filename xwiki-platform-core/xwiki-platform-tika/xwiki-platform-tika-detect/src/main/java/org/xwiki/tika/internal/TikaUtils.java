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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
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

    /**
     * Detects the media type of the given file. The type detection is based on the document content and a potential
     * known file extension.
     * <p>
     * Use the {@link #detect(String)} method when you want to detect the type of the document without actually
     * accessing the file.
     *
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
     * Detects the media type of the file at the given path. The type detection is based on the document content and a
     * potential known file extension.
     * <p>
     * Use the {@link #detect(String)} method when you want to detect the type of the document without actually
     * accessing the file.
     *
     * @param path the path of the file
     * @return detected media type
     * @throws IOException if the file can not be read
     */
    public static String detect(Path path) throws IOException
    {
        return tika.detect(path);
    }

    /**
     * Detects the media type of the given document. The type detection is based on the content of the given document
     * stream and the name of the document.
     * <p>
     * If the document stream supports the {@link InputStream#markSupported() mark feature}, then the stream is marked
     * and reset to the original position before this method returns. Only a limited number of bytes are read from the
     * stream.
     * <p>
     * The given document stream is <em>not</em> closed by this method.
     *
     * @param stream the document stream
     * @param name document name
     * @return detected media type
     * @throws IOException if the stream can not be read
     */
    public static String detect(InputStream stream, String name) throws IOException
    {
        return tika.detect(stream, name);
    }

    /**
     * Detects the media type of the given document. The type detection is based on the content of the given document
     * stream.
     * <p>
     * If the document stream supports the {@link InputStream#markSupported() mark feature}, then the stream is marked
     * and reset to the original position before this method returns. Only a limited number of bytes are read from the
     * stream.
     * <p>
     * The given document stream is <em>not</em> closed by this method.
     *
     * @param stream the document stream
     * @return detected media type
     * @throws IOException if the stream can not be read
     */
    public static String detect(InputStream stream) throws IOException
    {
        return tika.detect(stream);
    }

    /**
     * Detects the media type of a document with the given file name. The type detection is based on known file name
     * extensions.
     * <p>
     * The given name can also be a URL or a full file path. In such cases only the file name part of the string is used
     * for type detection.
     *
     * @param name the file name of the document
     * @return detected media type
     */
    public static String detect(String name)
    {
        return tika.detect(name);
    }

    /**
     * Parses the given document and returns the extracted text content. The given input stream is closed by this
     * method.
     * <p>
     * To avoid unpredictable excess memory use, the returned string contains only up to {@link #getMaxStringLength()}
     * first characters extracted from the input document. Use the {@link #setMaxStringLength(int)} method to adjust
     * this limitation.
     * <p>
     * <strong>NOTE:</strong> Unlike most other Tika methods that take an {@link InputStream}, this method will close
     * the given stream for you as a convenience. With other methods you are still responsible for closing the stream or
     * a wrapper instance returned by Tika.
     *
     * @param stream the document to be parsed
     * @param metadata document metadata
     * @return extracted text content
     * @throws IOException if the document can not be read
     * @throws TikaException if the document can not be parsed
     */
    public static String parseToString(InputStream stream, Metadata metadata) throws IOException, TikaException
    {
        return tika.parseToString(stream, metadata);
    }

    /**
     * Parses the given document and returns the extracted text content. The given input stream is closed by this
     * method.
     * <p>
     * To avoid unpredictable excess memory use, the returned string contains only up to {@link #getMaxStringLength()}
     * first characters extracted from the input document. Use the {@link #setMaxStringLength(int)} method to adjust
     * this limitation.
     * <p>
     * <strong>NOTE:</strong> Unlike most other Tika methods that take an {@link InputStream}, this method will close
     * the given stream for you as a convenience. With other methods you are still responsible for closing the stream or
     * a wrapper instance returned by Tika.
     *
     * @param stream the document to be parsed
     * @return extracted text content
     * @throws IOException if the document can not be read
     * @throws TikaException if the document can not be parsed
     */
    public static String parseToString(InputStream stream) throws IOException, TikaException
    {
        return tika.parseToString(stream);
    }

    /**
     * Parses the file at the given path and returns the extracted text content.
     * <p>
     * To avoid unpredictable excess memory use, the returned string contains only up to {@link #getMaxStringLength()}
     * first characters extracted from the input document. Use the {@link #setMaxStringLength(int)} method to adjust
     * this limitation.
     *
     * @param path the path of the file to be parsed
     * @return extracted text content
     * @throws IOException if the file can not be read
     * @throws TikaException if the file can not be parsed
     */
    public static String parseToString(Path path) throws IOException, TikaException
    {
        return tika.parseToString(path);
    }

    /**
     * Parses the given file and returns the extracted text content.
     * <p>
     * To avoid unpredictable excess memory use, the returned string contains only up to {@link #getMaxStringLength()}
     * first characters extracted from the input document. Use the {@link #setMaxStringLength(int)} method to adjust
     * this limitation.
     *
     * @param file the file to be parsed
     * @return extracted text content
     * @throws IOException if the file can not be read
     * @throws TikaException if the file can not be parsed
     * @see #parseToString(Path)
     */
    public static String parseToString(File file) throws IOException, TikaException
    {
        return tika.parseToString(file);
    }

    /**
     * Parses the resource at the given URL and returns the extracted text content.
     * <p>
     * To avoid unpredictable excess memory use, the returned string contains only up to {@link #getMaxStringLength()}
     * first characters extracted from the input document. Use the {@link #setMaxStringLength(int)} method to adjust
     * this limitation.
     *
     * @param url the URL of the resource to be parsed
     * @return extracted text content
     * @throws IOException if the resource can not be read
     * @throws TikaException if the resource can not be parsed
     */
    public static String parseToString(URL url) throws IOException, TikaException
    {
        return tika.parseToString(url);
    }
}
