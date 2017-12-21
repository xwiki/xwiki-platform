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
package org.xwiki.edit.script;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.edit.EditException;
import org.xwiki.edit.Editor;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxContent;

/**
 * Edit script service specialized in {@link SyntaxContent} {@link Editor}s.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Singleton
@Named(EditScriptService.ROLE_HINT + ".syntaxContent")
public class SyntaxContentEditScriptService extends AbstractTypedEditScriptService<SyntaxContent>
{
    /**
     * @return the default {@link SyntaxContent} editor in the "Text" category
     */
    public Editor<SyntaxContent> getDefaultTextEditor()
    {
        return getDefaultEditor("text");
    }

    /**
     * @return the default {@link SyntaxContent} editor in the "WYSIWYG" category
     */
    public Editor<SyntaxContent> getDefaultWysiwygEditor()
    {
        return getDefaultEditor("wysiwyg");
    }

    /**
     * Generates the HTML code needed to edit the given data.
     * 
     * @param content the text content to edit
     * @param syntax the syntax of the given content
     * @param parameters the edit parameters
     * @return the HTML code that displays the default text editor for {@link SyntaxContent}
     * @throws EditException if rendering the editor fails
     */
    public String text(String content, Syntax syntax, Map<String, Object> parameters) throws EditException
    {
        Editor<SyntaxContent> editor = getDefaultTextEditor();
        return editor == null ? null : editor.render(new SyntaxContent(content, syntax), parameters);
    }

    /**
     * Generates the HTML code needed to edit the given data.
     * 
     * @param content the text content to edit
     * @param syntax the syntax of the given content
     * @param parameters the edit parameters
     * @return the HTML code that displays the default WYSIWYG editor for {@link SyntaxContent}
     * @throws EditException if rendering the editor fails
     */
    public String wysiwyg(String content, Syntax syntax, Map<String, Object> parameters) throws EditException
    {
        Editor<SyntaxContent> editor = getDefaultWysiwygEditor();
        return editor == null ? null : editor.render(new SyntaxContent(content, syntax), parameters);
    }
}
