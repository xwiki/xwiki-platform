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

import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.util.Encoder;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;
import com.xpn.xwiki.web.Utils;

/**
 * Macro that outputs links to document attachments. Syntax: {attach:text|file|document|title|rel|id|fromIncludingDoc}
 * <ul>
 * <li>text: The link text. If missing, the file name is used.</li>
 * <li>file: The target filename. If missing, the link text is used.</li>
 * <li>document: The document to which the file is attached. If missing, the current document is used.</li>
 * <li>title: The title attribute of the link. If missing, no title.</li>
 * <li>rel: The rel attribute of the link. If missing, no rel.</li>
 * <li>id: The id attribute of the link. If missing, no id.</li>
 * <li>fromIncludingDoc: If present, when the current document is included using #include*, use the top level document
 * instead of the included one. This is useful for templates, for example.</li>
 * </ul>
 */
public class AttachMacro extends BaseLocaleMacro
{
    @Override
    public String getLocaleKey()
    {
        return "macro.attach";
    }

    @Override
    public void execute(Writer writer, MacroParameter params) throws IllegalArgumentException, IOException
    {

        RenderContext context = params.getContext();
        RenderEngine engine = context.getRenderEngine();

        // Read the parameters
        String text = params.get("text", 0);
        if (null == text || text.indexOf("=") != -1) {
            text = null;
        }
        String filename = params.get("file", 1);
        if (null == filename || filename.indexOf("=") != -1) {
            filename = null;
        }
        String document = params.get("document", 2);
        if (null == document || document.indexOf("=") != -1) {
            document = null;
        }
        String title = params.get("title", 3);
        if (null == title || title.indexOf("=") != -1 || title.equals("")) {
            title = null;
        }
        String rel = params.get("rel", 4);
        if (null == rel || rel.indexOf("=") != -1 || rel.equals("")) {
            rel = null;
        }
        String id = params.get("id", 5);
        if (null == id || id.indexOf("=") != -1 || id.equals("")) {
            id = null;
        }
        String useIncluder = params.get("fromIncludingDoc", 6);
        if (null == useIncluder || useIncluder.indexOf("=") != -1 || useIncluder.toLowerCase().startsWith("f")) {
            useIncluder = null;
        }

        // Fix missing text or filename parameters
        if (text == null && filename != null) {
            text = filename;
        } else if (text != null && filename == null) {
            filename = text;
        } else if (text == null && filename == null) {
            return;
        }
        text = Encoder.escape(text);

        XWikiContext xcontext = ((XWikiRadeoxRenderEngine) engine).getXWikiContext();

        // Get the target document
        XWikiDocument doc = null;
        try {
            if (document == null || "".equals(document)) {
                if (useIncluder == null) {
                    doc = (XWikiDocument) xcontext.get("sdoc");
                }
            } else {
                doc = xcontext.getWiki().getDocument(xcontext.getDoc().getSpace(), document, xcontext);
            }
        } catch (Exception ex) {
            // NullPointer or ClassCast
        }
        if (doc == null) {
            doc = xcontext.getDoc();
        }

        // Create the link code
        StringBuilder str = new StringBuilder();
        String url;
        str.append("<a href=\"");
        if (filename.indexOf("tp://") != -1) {
            url = filename;
        } else {
            url = doc.getAttachmentURL(filename, "download", xcontext);
        }
        str.append(Utils.createPlaceholder(url, xcontext));
        str.append("\"");
        if (rel != null) {
            str.append(" rel=\"" + Utils.createPlaceholder(rel, xcontext) + "\"");
        }
        if (title != null) {
            str.append(" title=\"" + Utils.createPlaceholder(title, xcontext) + "\"");
        }
        if (id != null) {
            str.append(" id=\"" + Utils.createPlaceholder(id, xcontext) + "\"");
        }
        str.append(">");
        str.append(text);
        str.append("</a>");

        // All done, flush the StringBufer
        writer.write(str.toString());

        // if (!"none".equals(img) && engine instanceof ImageRenderEngine) {
        // writer.write(((ImageRenderEngine) engine).getExternalImageLink());
        // }

    }
}
