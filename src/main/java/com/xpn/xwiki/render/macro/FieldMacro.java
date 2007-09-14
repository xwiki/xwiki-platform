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
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;
import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import java.io.IOException;
import java.io.Writer;

public class FieldMacro extends BaseLocaleMacro {
  public String getLocaleKey() {
    return "macro.field";
  }

  public void execute(Writer writer, MacroParameter params)
      throws IllegalArgumentException, IOException {

    RenderContext context = params.getContext();
    RenderEngine engine = context.getRenderEngine();
    XWikiContext xcontext = ((XWikiRadeoxRenderEngine)engine).getContext();
    XWikiDocument doc = xcontext.getDoc();

    String fieldname = params.get("text", 0);
    String mode = params.get("mode", 1);
    String sobjid = params.get("id", 2);
    String className = doc.getFullName();

    int position = fieldname.indexOf(".");
    if (position!=-1) {
        className = fieldname.substring(0, position);
        fieldname = fieldname.substring(position + 1);
    }

    BaseObject object;
    if (sobjid==null)
        object = doc.getObject(className);
    else {
        int objid = Integer.parseInt(sobjid);
        object = doc.getObject(className, objid);
    }

    String result;
    if (mode==null)
        result = doc.display(fieldname, object, xcontext);
    else
        result = doc.display(fieldname, mode, object, xcontext);

    writer.write(result);
  }
}
