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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import org.xwiki.gwt.dom.client.Document;

import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Specifies whether the target document should be styled using the style attributes in markup or using formatting
 * elements. We added this class because there are two predefined commands that control this flag:
 * {@link Command#STYLE_WITH_CSS} and {@link StyleWithCssExecutable#USE_CSS}. Since some browsers support the first
 * while other support the second, we execute both commands.
 * 
 * @version $Id$
 */
public class StyleWithCssExecutable implements Executable
{
    /**
     * The deprecated command for switching between styling with the style attribute and styling with formatting tags.
     */
    public static final Command USE_CSS = new Command("useCSS");

    /**
     * {@inheritDoc}
     * 
     * @see Executable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String parameter)
    {
        boolean styleWithCSS = Boolean.valueOf(parameter);
        boolean success =
            rta.getDocument().execCommand(Command.STYLE_WITH_CSS.toString(), String.valueOf(styleWithCSS));
        // useCSS command is deprecated and has opposite meaning
        success = success || rta.getDocument().execCommand(USE_CSS.toString(), String.valueOf(!styleWithCSS));
        return success;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#getParameter(RichTextArea)
     */
    public String getParameter(RichTextArea rta)
    {
        String parameter = rta.getDocument().queryCommandValue(Command.STYLE_WITH_CSS.toString());
        if (parameter == null) {
            parameter = rta.getDocument().queryCommandValue(USE_CSS.toString());
            if (parameter != null) {
                parameter = String.valueOf(!Boolean.valueOf(parameter));
            }
        }
        return parameter;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        return rta.getDocument().queryCommandEnabled(Command.STYLE_WITH_CSS.toString())
            || rta.getDocument().queryCommandEnabled(USE_CSS.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        return rta.getDocument().queryCommandState(Command.STYLE_WITH_CSS.toString())
            || rta.getDocument().queryCommandState(USE_CSS.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isSupported(RichTextArea)
     */
    public boolean isSupported(RichTextArea rta)
    {
        Document doc = rta.getDocument();
        return doc != null
            && (doc.queryCommandSupported(Command.STYLE_WITH_CSS.toString()) || doc.queryCommandSupported(USE_CSS
                .toString()));
    }
}
