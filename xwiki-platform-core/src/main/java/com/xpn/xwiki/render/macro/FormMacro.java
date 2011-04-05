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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;
import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import java.io.IOException;
import java.io.Writer;

public class FormMacro  extends BaseLocaleMacro {
  public String getLocaleKey() {
    return "macro.form";
  }

  public void execute(Writer writer, MacroParameter params)
      throws IllegalArgumentException, IOException {

    RenderContext context = params.getContext();
    RenderEngine engine = context.getRenderEngine();
    XWikiContext xcontext = ((XWikiRadeoxRenderEngine)engine).getXWikiContext();
    XWikiDocument doc = xcontext.getDoc();

    String className = params.get("text", 0);

    String result;
    if (className==null)
        result = doc.displayForm(doc.getFullName(), xcontext);
    else
        result = doc.displayForm(className, xcontext);
    
    writer.write(result);
  }
}

