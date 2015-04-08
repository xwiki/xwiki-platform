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
package com.xpn.xwiki.plugin.svg;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang3.StringUtils;
import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;

/**
 * Radeox macro for the xwiki 1.0 syntax, converting SVG code into bitmap images. Syntax:
 * <tt>{svg:alternate text|height|width} SVG content here {svg}</tt>
 * <dl>
 * <dt>text</dt>
 * <dd>The alternate text for the image.</dd>
 * <dt>height</dt>
 * <dd>The height of the generated image. If missing or invalid, the default is 400.</dd>
 * <dt>width</dt>
 * <dd>The width of the generated image. If missing or invalid, the default is 400.</dd>
 * </dl>
 * <p>
 * You can get the content from an attachment using Velocity, as in:
 * </p>
 *
 * <pre>
 * {svg}
 * $doc.getAttachment('image.svg').getContentAsString()
 * {svg}
 * </pre>
 * <p>
 * The macro relies on the {@link SVGPlugin} to actually transform the SVG content into an image.
 * </p>
 *
 * @deprecated The Radeox macros are deprecated in favor of the new wiki macros.
 * @version $Id$
 */
@Deprecated
public class SVGMacro extends BaseLocaleMacro
{
    /**
     * The name of the macro.
     *
     * @see org.radeox.macro.BaseLocaleMacro#getLocaleKey()
     */
    @Override
    public String getLocaleKey()
    {
        return "macro.svg";
    }

    /**
     * Main macro execution method, replaces the macro instance with the generated output.
     *
     * @param writer the place where to write the output
     * @param params the parameters this macro is called with
     * @throws IllegalArgumentException if the mandatory argument ({@code text}) is missing
     * @throws IOException if the output cannot be written
     * @see org.radeox.macro.BaseMacro#execute(Writer, MacroParameter)
     */
    @Override
    public void execute(Writer writer, MacroParameter params) throws IllegalArgumentException, IOException
    {
        RenderContext context = params.getContext();
        RenderEngine engine = context.getRenderEngine();

        XWikiContext xcontext = ((XWikiRadeoxRenderEngine) engine).getXWikiContext();
        XWiki xwiki = xcontext.getWiki();

        SVGPlugin plugin = (SVGPlugin) xwiki.getPlugin("svg", xcontext);
        // If the SVG plugin is not loaded, exit.
        if (plugin == null) {
            writer.write("Plugin not loaded");
            return;
        }
        // {svg:alternate text|height|width}
        StringBuilder str = new StringBuilder();
        String text = params.get("text", 0);
        String height = params.get("height", 1);
        if (StringUtils.isBlank(height) || "none".equals(height) || !StringUtils.isNumeric(height.trim())) {
            height = "400";
        }
        String width = params.get("width", 2);
        if (StringUtils.isBlank(width) || "none".equals(width) || !StringUtils.isNumeric(width.trim())) {
            width = "400";
        }
        try {
            int intHeight = Integer.parseInt(height.trim());
            int intWidth = Integer.parseInt(width.trim());
            String svgtext = StringUtils.trimToEmpty(params.getContent());
            str.append("<img src=\"");
            // The SVG plugin generates the image and returns an URL for accessing it.
            str.append(plugin.getSVGImageURL(svgtext, intHeight, intWidth, xcontext));
            str.append("\" ");
            str.append("height=\"" + height + "\" ");
            str.append("width=\"" + width + "\" ");
            str.append("alt=\"");
            str.append(text);
            str.append("\" />");
            writer.write(str.toString());
        } catch (Throwable t) {
            XWikiException e =
                new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "SVG Issue", t);
            writer.write("Exception converting SVG: " + e.getFullMessage());
        }
    }
}
