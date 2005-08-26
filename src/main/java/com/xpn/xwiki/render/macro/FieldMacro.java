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
 * Date: 25 août 2004
 * Time: 13:43:19
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
