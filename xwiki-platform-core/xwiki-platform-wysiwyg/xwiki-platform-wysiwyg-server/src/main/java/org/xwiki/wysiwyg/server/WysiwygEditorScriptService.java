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
package org.xwiki.wysiwyg.server;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * The WYSIWYG editor API exposed to server-side scripts like Velocity.
 * 
 * @version $Id$
 */
@Role
public interface WysiwygEditorScriptService extends ScriptService
{
    /**
     * Checks if there is a parser and a renderer available for the specified syntax.
     * <p>
     * This method should be called before attempting to load the WYSIWYG editor.
     * 
     * @param syntaxId the syntax identifier, like {@code xwiki/2.0}
     * @return {@code true} if the specified syntax is currently supported by the editor, {@code false} otherwise
     */
    boolean isSyntaxSupported(String syntaxId);

    /**
     * Parses the given HTML fragment and renders the result in annotated XHTML syntax.
     * <p>
     * This method is currently used in {@code wysiwyginput.vm} and its purpose is to refresh the content of the WYSIWYG
     * editor. This method is called for instance when a macro is inserted or edited.
     * 
     * @param html the HTML fragment to be rendered
     * @param syntaxId the storage syntax identifier
     * @return the XHTML result of rendering the given HTML fragment
     */
    String parseAndRender(String html, String syntaxId);

    /**
     * Produces the input for the editor by rendering the specified content template as a full HTML page, making sure
     * the skin extension hooks are resolved. The template is rendered in the context of the current document and the
     * Velocity context is not isolated so you can put the data needed by the template in the Velocity context before
     * calling this method. The advantage of using this method to obtain the editor input is that the editor doesn't
     * have to make an additional HTTP request for the content template.
     * 
     * @param templateReference specifies the document that serves as the template for the editor content
     * @return the result of rendering the specified content template
     * @since 7.4.1
     * @since 8.0M1
     */
    @Unstable
    String render(DocumentReference templateReference);

    /**
     * Converts the given source text from the specified syntax to annotated XHTML, which can be used as input for the
     * WYSIWYG editor.
     * 
     * @param source the text to be converted
     * @param syntaxId the syntax identifier
     * @return the annotated XHTML result of the conversion
     */
    String toAnnotatedXHTML(String source, String syntaxId);

    /**
     * @return the WYSIWYG editor configuration object
     */
    WysiwygEditorConfiguration getConfig();
}
