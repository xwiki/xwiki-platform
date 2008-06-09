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
 * Date: 25 august 2004
 * Time: 13:43:19
 */
package com.xpn.xwiki.render.macro;

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.VelocityContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import com.xpn.xwiki.api.Document;

public class UseMacro extends BaseLocaleMacro {
  public String getLocaleKey() {
    return "macro.use";
  }

  public void execute(Writer writer, MacroParameter params)
      throws IllegalArgumentException, IOException {

    RenderContext context = params.getContext();
    VelocityContext vcontext = (VelocityContext) context.get("vcontext");
    Document doc = (Document) vcontext.get("doc");
    com.xpn.xwiki.api.Object obj;

      // We lookup the className for this object
    String classname = params.get("classname", 0);

    // Optionnaly we see if it was asked for an object number
    String snb = params.get("nb", 1);

    // We find the corresponding object
    if (snb!=null)
     obj = doc.getObject(classname, Integer.parseInt(snb));
    else
     obj = doc.getObject(classname);

    // We assign this object as the used object
    // Future calls to doc.display() or {field} will make use of this object
    doc.use(obj);
  }
}
