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
package com.xpn.xwiki.render;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import java.util.List;

public interface XWikiRenderingEngine
{
    void virtualInit(XWikiContext context);

    void addRenderer(String name, XWikiRenderer renderer);

    XWikiRenderer getRenderer(String name);

    List<XWikiRenderer> getRendererList();

    List<String> getRendererNames();

    String renderDocument(XWikiDocument doc, XWikiContext context) throws XWikiException;

    String renderDocument(XWikiDocument doc, XWikiDocument includingdoc, XWikiContext context) throws XWikiException;

    String renderText(String text, XWikiDocument includingdoc, XWikiContext context);

    String interpretText(String text, XWikiDocument includingdoc, XWikiContext context);

    String renderText(String text, XWikiDocument contentdoc, XWikiDocument includingdoc, XWikiContext context);

    void flushCache();

    String convertMultiLine(String macroname, String params, String data, String allcontent, XWikiVirtualMacro macro,
        XWikiContext context);

    String convertSingleLine(String macroname, String params, String allcontent, XWikiVirtualMacro macro,
        XWikiContext context);
}
