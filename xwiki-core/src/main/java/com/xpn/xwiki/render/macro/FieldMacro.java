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

package com.xpn.xwiki.render.macro;

import java.io.IOException;
import java.io.Writer;

import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;

/**
 * Radeox macro for the xwiki 1.0 syntax, which can display a property from an object attached to the current document.
 * Syntax: <tt>{field:text|url|image}</tt>
 * <dl>
 * <dt>text</dt>
 * <dd>The name of the field to display. It can be prefixed with the short name of a class in the "XWiki" space,
 * otherwise the name of the current document is assumed to be the target class.</dd>
 * <dt>mode</dt>
 * <dd>The display mode, one of the modes supported by the {@link com.xpn.xwiki.api.Document#display(String, String)}
 * method: <tt>view</tt>, <tt>rendered</tt>, <tt>edit</tt>, <tt>search</tt>, <tt>hidden</tt>. If missing, the current
 * default mode is used.</dd>
 * <dt>id</dt>
 * <dd>The index of the object of the specified type, from the current document.</dd>
 * </dl>
 * Example:
 * <ul>
 * <li><tt>{field:title}</tt> displays the {@code title} field of the first object of the type defined in the current
 * document.</li>
 * <li><tt>{field:XWikiUsers.email}</tt> displays the {@code email} field of the first object of the {@code
 * XWiki.XWikiUsers} type.</li>
 * <li><tt>{field:XWikiComment.content|edit|3}</tt> displays the {@code content} field of the third {@code
 * XWiki.XWikiComment} type in edit mode.</li>
 * </ul>
 * 
 * @deprecated It provides limited functionalities compared to the {@code doc.display} scripting API.
 * @version $Id$
 */
@Deprecated
public class FieldMacro extends BaseLocaleMacro
{
    /**
     * The name of the macro.
     * 
     * @see org.radeox.macro.BaseLocaleMacro#getLocaleKey()
     */
    public String getLocaleKey()
    {
        return "macro.field";
    }

    /**
     * Main macro execution method, replaces the macro instance with the generated output.
     * 
     * @param writer the place where to write the output
     * @param params the parameters this macro is called with
     * @throws IllegalArgumentException if the mandatory argument ({@code text}) is missing
     * @throws IOException if the output cannot be written
     * @see org.radeox.macro.BaseMacro#execute(Writer, MacroParameter)
     */
    @Override
    public void execute(Writer writer, MacroParameter params) throws IllegalArgumentException, IOException
    {
        RenderContext context = params.getContext();
        RenderEngine engine = context.getRenderEngine();
        XWikiContext xcontext = ((XWikiRadeoxRenderEngine) engine).getXWikiContext();
        XWikiDocument doc = xcontext.getDoc();

        String fieldname = params.get("text", 0);
        String mode = params.get("mode", 1);
        String sobjid = params.get("id", 2);
        String className = doc.getFullName();

        int position = fieldname.indexOf(".");
        if (position != -1) {
            className = fieldname.substring(0, position);
            fieldname = fieldname.substring(position + 1);
        }

        BaseObject object;
        if (sobjid == null) {
            object = doc.getObject(className);
        } else {
            int objid = Integer.parseInt(sobjid);
            object = doc.getObject(className, objid);
        }

        String result;
        if (mode == null) {
            result = doc.display(fieldname, object, xcontext);
        } else {
            result = doc.display(fieldname, mode, object, xcontext);
        }

        writer.write(result);
    }
}
