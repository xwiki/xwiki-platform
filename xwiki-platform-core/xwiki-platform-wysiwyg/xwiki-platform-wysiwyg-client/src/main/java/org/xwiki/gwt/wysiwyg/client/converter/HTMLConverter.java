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

import org.xwiki.component.annotation.Role;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Converts HTML to/from a specified syntax.
 * 
 * @version $Id$
 */
@Role
@RemoteServiceRelativePath("HTMLConverter.gwtrpc")
public interface HTMLConverter extends RemoteService
{
    /**
     * Converts the given source text from the specified syntax to HTML.
     * 
     * @param source the text to be converted
     * @param syntaxId the syntax identifier
     * @return the HTML result of the conversion
     */
    String toHTML(String source, String syntaxId);

    /**
     * Cleans and converts the given HTML fragment to the specified syntax.
     * 
     * @param html the HTML text to be converted
     * @param syntaxId the syntax identifier
     * @return the result on the conversion
     */
    String fromHTML(String html, String syntaxId);

    /**
     * Parses the given HTML fragment and renders the result in annotated XHTML syntax.
     * 
     * @param html the HTML fragment to be parsed and rendered
     * @param syntax the storage syntax
     * @return the XHTML result of rendering the given HTML fragment
     */
    String parseAndRender(String html, String syntax);
}
