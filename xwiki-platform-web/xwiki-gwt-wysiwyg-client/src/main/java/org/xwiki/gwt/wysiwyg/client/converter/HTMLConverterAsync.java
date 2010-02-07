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
package org.xwiki.gwt.wysiwyg.client.converter;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Converter interface to be used on the client. It should have all the methods from {@link HTMLConverter} with an
 * additional {@link AsyncCallback} parameter. This is specific to GWT's architecture.
 * 
 * @version $Id$
 */
public interface HTMLConverterAsync
{
    /**
     * Makes a request to the server to convert the given source text from the specified syntax to HTML.
     * 
     * @param source the text to be converted
     * @param syntaxId the syntax identifier
     * @param callback the object used to notify the caller when the server response is received
     */
    void toHTML(String source, String syntaxId, AsyncCallback<String> callback);

    /**
     * Makes a request to the server to convert the given HTML fragment to the specified syntax.
     * 
     * @param html the HTML text to be converted
     * @param syntaxId the syntax identifier
     * @param callback the object used to notify the caller when the server response is received
     */
    void fromHTML(String html, String syntaxId, AsyncCallback<String> callback);

    /**
     * Makes a request to the server to parse the given HTML fragment and render the result in annotated XHTML syntax.
     * 
     * @param html the HTML fragment to be parsed and rendered
     * @param syntax the storage syntax
     * @param callback the object used to notify the caller when the server response is received
     */
    void parseAndRender(String html, String syntax, AsyncCallback<String> callback);
}
