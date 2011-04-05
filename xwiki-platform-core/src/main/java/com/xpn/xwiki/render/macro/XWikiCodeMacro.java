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
/*
 * This file is part of "SnipSnap Radeox Rendering Engine".
 *
 * Copyright (c) 2002 Stephan J. Schmidt, Matthias L. Jugel
 * All Rights Reserved.
 *
 * Please visit http://radeox.org/ for updates and contact.
 *
 * --LICENSE NOTICE--
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * --LICENSE NOTICE--
 */
package com.xpn.xwiki.render.macro;

import org.radeox.macro.CodeMacro;
import org.radeox.macro.parameter.MacroParameter;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Writer;

/**
 * The Code Macro renders its content as is but pretty printed according to the language passed as parameter. Supporting
 * escaping Radeox, Velocity and Groovy wasn't trivial. Here's how it's currently implemented (waiting for a better
 * implementation):
 * <ol>
 * <li>The first renderer to execute is the MacroMappingRenderer. That's because it's registered first in
 *     {@link com.xpn.xwiki.render.DefaultXWikiRenderingEngine#DefaultXWikiRenderingEngine(com.xpn.xwiki.XWiki, com.xpn.xwiki.XWikiContext)}}.
 *     The code macro is registered in the macros.txt file used by the MacroMappingRenderer and thus it is processed
 *     by the macro mapping feature. Since it recognizes it's a radeox macro, it calls the Radeox Renderer's
 *     {@link com.xpn.xwiki.render.XWikiRadeoxRenderer#convertMultiLine(String, String, String, String, com.xpn.xwiki.render.XWikiVirtualMacro, com.xpn.xwiki.XWikiContext)}
 *     method. This method has a hack and verifies if the macro to render is the code macro. If so it escapes Velocity
 *     and Groovy special characters ($, #, <% and %>) so that when the Velocity and Radeox renderers execute they do
 *     not process its content.</li>
 * <li>We also don't want the Radeox renderer to render the code macro's content. We do this by having a special Radeox
 *     Filter ({@link com.xpn.xwiki.render.filter.CodeRemoveFilter} that is defined before all other filters in
 *     the <code>META-INF/services/com.xpn.xwiki.render.filter.XWikiFilter</code> file and that removes the content of
 *     the code macro. The other filters then execute. The last but one defined filter is
 *     {@link com.xpn.xwiki.render.filter.CodeRestoreFilter} which puts back the content of the code macro and the last
 *     filter is the {@link com.xpn.xwiki.render.filter.CodeFilter} filter which calls this macro.</li>
 * </ol>
 */
public class XWikiCodeMacro extends CodeMacro
{
    public XWikiCodeMacro()
    {
        super();
    }

    public String getLocaleKey()
    {
        return "macro.code";
    }

    public void execute(Writer writer, MacroParameter params)
        throws IllegalArgumentException, IOException
    {
        // We need to escape any HTML tag before we execute the macro. This is because the macro
        // generates HTML itself and we must only escape the HTML that was there before the
        // generation.
        String content = params.getContent();
        content = StringUtils.replace(content, "<", "&#60;");
        content = StringUtils.replace(content, ">", "&#62;");
        params.setContent(content);

        super.execute(writer, params);
        return;
    }
}
