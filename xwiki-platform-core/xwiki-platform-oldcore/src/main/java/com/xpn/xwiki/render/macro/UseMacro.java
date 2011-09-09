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
 */
package com.xpn.xwiki.render.macro;

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.VelocityContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import com.xpn.xwiki.api.Document;

public class UseMacro extends BaseLocaleMacro
{
    @Override
    public String getLocaleKey()
    {
        return "macro.use";
    }

    @Override
    public void execute(Writer writer, MacroParameter params) throws IllegalArgumentException, IOException
    {

        RenderContext context = params.getContext();
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        Document doc = (Document) vcontext.get("doc");
        com.xpn.xwiki.api.Object obj;

        // We lookup the className for this object
        String classname = params.get("classname", 0);

        // Optionnaly we see if it was asked for an object number
        String snb = params.get("nb", 1);

        // We find the corresponding object
        if (snb != null)
            obj = doc.getObject(classname, Integer.parseInt(snb));
        else
            obj = doc.getObject(classname);

        // We assign this object as the used object
        // Future calls to doc.display() or {field} will make use of this object
        doc.use(obj);
    }
}
