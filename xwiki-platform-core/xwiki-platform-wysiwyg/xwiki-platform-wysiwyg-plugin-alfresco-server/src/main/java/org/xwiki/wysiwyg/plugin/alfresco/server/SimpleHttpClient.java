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
package org.xwiki.wysiwyg.plugin.alfresco.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map.Entry;

import org.xwiki.component.annotation.ComponentRole;

/**
 * A simple HTTP Client.
 * 
 * @version $Id$
 */
@ComponentRole
public interface SimpleHttpClient
{
    /**
     * The interface used to handle the response.
     * 
     * @param <T> the type of object read from the response content
     */
    interface ResponseHandler<T>
    {
        /**
         * Read the response content stream.
         * 
         * @param content the response content stream
         * @return the object read from the response content
         */
        T read(InputStream content);
    }

    /**
     * Executes a GET HTTP request on the specified URL with the specified query string parameters.
     * 
     * @param <T> the type of object read from the response content
     * @param url the target of the GET request
     * @param queryStringParameters the list of query string parameters
     * @param handler the response handler
     * @return the object read from the response content
     * @throws IOException if this method fails to send the request or to read the response
     */
    <T> T doGet(String url, List<Entry<String, String>> queryStringParameters, ResponseHandler<T> handler)
        throws IOException;

    /**
     * Executes a POST HTTP request on the specified URL with the specified content.
     * 
     * @param <T> the type of object read from the response content
     * @param url the target of the POST request
     * @param content the posted content
     * @param contentType the media type of the posted content
     * @param handler the response handler
     * @return the object read from the response content
     * @throws IOException if this method fails to send the request or to read the response
     */
    <T> T doPost(String url, String content, String contentType, ResponseHandler<T> handler) throws IOException;
}
