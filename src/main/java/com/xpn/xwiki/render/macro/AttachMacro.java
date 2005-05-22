/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 24 août 2004
 * Time: 20:34:21
 */
package com.xpn.xwiki.render.macro;

import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.api.engine.RenderEngine;
import org.radeox.util.Encoder;

import java.io.Writer;
import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;

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
