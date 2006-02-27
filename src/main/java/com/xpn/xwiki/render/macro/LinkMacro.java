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

import java.io.IOException;
import java.io.Writer;

import org.radeox.api.engine.ImageRenderEngine;
import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.util.Encoder;

public class LinkMacro extends BaseLocaleMacro {
  public String getLocaleKey() {
    return "macro.link";
  }

  public void execute(Writer writer, MacroParameter params)
      throws IllegalArgumentException, IOException {

    RenderContext context = params.getContext();
    RenderEngine engine = context.getRenderEngine();

    String text = params.get("text", 0);
    String url = params.get("url", 1);
    String img = params.get("img", 2);

    // check for single url argument (text == url)
    if(params.getLength() == 1) {
      url = text;
      text = Encoder.toEntity(text.charAt(0)) + Encoder.escape(text.substring(1));
    }

    if (url != null && text != null) {
      writer.write("<span class=\"nobr\">");
      if (!"none".equals(img) && engine instanceof ImageRenderEngine) {
        writer.write(((ImageRenderEngine) engine).getExternalImageLink());
      }

      // A URL should not have starting and trailing white spaces
      url = url.trim();

      writer.write("<a href=\"");
      writer.write(Encoder.escape(url));
      writer.write("\">");
      writer.write(text);
      writer.write("</a></span>");
    } else {
      throw new IllegalArgumentException("link needs a name and a url as argument");
    }
    return;
  }
}
