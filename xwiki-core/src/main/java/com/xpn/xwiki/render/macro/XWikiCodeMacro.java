/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
