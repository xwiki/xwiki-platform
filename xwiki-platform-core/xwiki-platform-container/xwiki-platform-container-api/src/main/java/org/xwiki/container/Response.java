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
package org.xwiki.container;

import java.io.IOException;
import java.io.OutputStream;

import org.xwiki.stability.Unstable;

/**
 * Represents a server response.
 * 
 * @version $Id$
 * @since 1.2M1
 */
public interface Response
{
    /**
     * Returns an OutputStream suitable for writing binary data in the response.
     * 
     * @return the binary OutputStream for the response, or {@code null} if the response does not allow writing data
     * @throws IOException if an output exception occurred
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * Sets the length of the content body in the response. If this length is not relevant to the type of connection, it
     * will simply be ignored.
     * 
     * @param length an integer specifying the length of the content being returned to the client
     */
    void setContentLength(int length);

    /**
     * Sets the content type of the response being sent to the client, as a MIME type string. For example, {@code
     * text/html}. If the MIME type is not relevant to the type of connection, it will simply be ignored.
     * 
     * @param mimeType the MIME type for this response, according to the RFC 2045.
     */
    void setContentType(String mimeType);

    // HTTP

    /**
     * Sends a temporary redirect response to the client using the specified redirect location URL.
     *
     * @param location the redirect URL
     * @throws IOException if an error happens
     * @since 42.0.0
     */
    @Unstable
    default void sendRedirect(String location) throws IOException
    {
        
    }

    /**
     * Sets the status code for this response.
     * <p>
     * Valid status codes are those in the 2XX, 3XX, 4XX, and 5XX ranges. Other status codes are treated as container
     * specific.
     *
     * @param sc the status code
     * @see #sendError
     * @since 42.0.0
     */
    @Unstable
    default void setStatus(int sc)
    {
        
    }

    /**
     * Sends an error response to the client using the specified status and clears the buffer.
     * <p>
     * If the response has already been committed, this method throws an IllegalStateException. After using this method,
     * the response should be considered to be committed and should not be written to.
     *
     * @param sc the error status code
     * @param msg the descriptive message
     * @exception IOException If an input or output exception occurs
     * @exception IllegalStateException If the response was committed
     * @since 42.0.0
     */
    @Unstable
    default void sendError(int sc, String msg) throws IOException
    {
        
    }

    /**
     * Sends an error response to the client using the specified status code and clears the buffer.
     * <p>
     * If the response has already been committed, this method throws an IllegalStateException. After using this method,
     * the response should be considered to be committed and should not be written to.
     *
     * @param sc the error status code
     * @exception IOException If an input or output exception occurs
     * @exception IllegalStateException If the response was committed before this method call
     * @since 42.0.0
     */
    @Unstable
    default void sendError(int sc) throws IOException
    {
        
    }

    /**
     * Returns a boolean indicating whether the named response header has already been set.
     * 
     * @param name the header name
     * @return <code>true</code> if the named response header has already been set; <code>false</code> otherwise
     * @since 42.0.0
     */
    @Unstable
    default boolean containsHeader(String name)
    {
        return false;
    }

    /**
     * Sets a response header with the given name and value. If the header had already been set, the new value
     * overwrites the previous one. The <code>containsHeader</code> method can be used to test for the presence of a
     * header before setting its value.
     * 
     * @param name the name of the header
     * @param value the header value If it contains octet string, it should be encoded according to RFC 2047
     *            (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #containsHeader
     * @see #addHeader
     * @since 42.0.0
     */
    @Unstable
    default void setHeader(String name, String value)
    {
        
    }

    /**
     * Adds a response header with the given name and value. This method allows response headers to have multiple
     * values.
     * 
     * @param name the name of the header
     * @param value the additional header value If it contains octet string, it should be encoded according to RFC 2047
     *            (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #setHeader
     * @since 42.0.0
     */
    @Unstable
    default void addHeader(String name, String value)
    {
        
    }
}
