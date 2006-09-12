/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author sdumitriu
 */

package com.xpn.xwiki.render.macro;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;
import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.util.Encoder;

import java.io.IOException;
import java.io.Writer;

public class AttachMacro   extends BaseLocaleMacro {
  public String getLocaleKey() {
    return "macro.attach";
  }

  public void execute(Writer writer, MacroParameter params)
      throws IllegalArgumentException, IOException {

    RenderContext context = params.getContext();
    RenderEngine engine = context.getRenderEngine();

    String text = params.get("text", 0);
    String filename = params.get("file", 1);

    if(params.getLength() == 1) {
        filename = text;
        text = Encoder.toEntity(text.charAt(0)) + Encoder.escape(text.substring(1));
    }


    XWikiContext xcontext = ((XWikiRadeoxRenderEngine)engine).getContext();
    XWikiDocument doc = xcontext.getDoc();

    StringBuffer str = new StringBuffer();
    str.append("<a href=\"");
    str.append(doc.getAttachmentURL(filename, "download", xcontext));
    str.append("\" />");
    str.append(text);
    str.append("</a>");
    writer.write(str.toString());

    // if (!"none".equals(img) && engine instanceof ImageRenderEngine) {
    //    writer.write(((ImageRenderEngine) engine).getExternalImageLink());
    // }

  }
}
