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

import java.io.IOException;
import java.io.Writer;

public class ImageMacro  extends BaseLocaleMacro {
  public String getLocaleKey() {
    return "macro.image";
  }

  public void execute(Writer writer, MacroParameter params)
      throws IllegalArgumentException, IOException {

    RenderContext context = params.getContext();
    RenderEngine engine = context.getRenderEngine();

    String img = params.get("text", 0);
    String height = params.get("height", 1);
    String width = params.get("width", 2);
    String align = params.get("align", 3);
    XWikiContext xcontext = ((XWikiRadeoxRenderEngine)engine).getContext();
    XWikiDocument doc = xcontext.getDoc();

    StringBuffer str = new StringBuffer();
    str.append("<img src=\"");
    str.append(doc.getAttachmentURL(img, "download", xcontext));
    str.append("\" ");
    if ((!"none".equals(height))&&(height!=null)&&(!"".equals(height.trim()))){
        str.append("height=\"" + height.trim() + "\" ");
    }
    if ((!"none".equals(width))&&(width!=null)&&(!"".equals(width.trim()))){
    	str.append("width=\"" + width.trim() + "\" ");
    }
    if ((!"none".equals(align))&&(align!=null)&&(!"".equals(align.trim()))){
    	str.append("align=\"" + align.trim() + "\" ");
    }
    str.append("alt=\"");
    str.append(img);
    str.append("\" />");
    writer.write(str.toString());
  }
}