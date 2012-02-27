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
package org.xwiki.gwt.user.client.ui.rta.cmd.internal;

import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;

/**
 * Specifies whether the target document should be styled using the style attributes in markup or using formatting
 * elements. We added this class because there are two predefined commands that control this flag:
 * {@link Command#STYLE_WITH_CSS} and {@link StyleWithCssExecutable#USE_CSS}. Since some browsers support the first
 * while other support the second, we execute both commands.
 * 
 * @version $Id$
 */
public class StyleWithCssExecutable extends AbstractRichTextAreaExecutable
{
    /**
     * The deprecated command for switching between styling with the style attribute and styling with formatting tags.
     */
    public static final Command USE_CSS = new Command("useCSS");

    /**
     * Creates a new executable to be executed on the specified rich text area.
     * 
     * @param rta the execution target
     */
    public StyleWithCssExecutable(RichTextArea rta)
    {
        super(rta);
    }

    @Override
    public boolean execute(String parameter)
    {
        boolean styleWithCSS = Boolean.valueOf(parameter);
        boolean success =
            rta.getDocument().execCommand(Command.STYLE_WITH_CSS.toString(), String.valueOf(styleWithCSS));
        // useCSS command is deprecated and has opposite meaning
        success = success || rta.getDocument().execCommand(USE_CSS.toString(), String.valueOf(!styleWithCSS));
        return success;
    }

    @Override
    public String getParameter()
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

    @Override
    public boolean isEnabled()
    {
        return super.isEnabled() && rta.getDocument().queryCommandEnabled(Command.STYLE_WITH_CSS.toString())
            || rta.getDocument().queryCommandEnabled(USE_CSS.toString());
    }

    @Override
    public boolean isExecuted()
    {
        return rta.getDocument().queryCommandState(Command.STYLE_WITH_CSS.toString())
            || rta.getDocument().queryCommandState(USE_CSS.toString());
    }

    @Override
    public boolean isSupported()
    {
        return super.isSupported()
            && (rta.getDocument().queryCommandSupported(Command.STYLE_WITH_CSS.toString()) || rta.getDocument()
                .queryCommandSupported(USE_CSS.toString()));
    }
}
