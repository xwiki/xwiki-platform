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
 *
 */
package org.xwiki.container;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents a server response.
 * 
 * @version $Id$
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
}
