/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 26 nov. 2003
 * Time: 20:48:17
 */
package com.xpn.xwiki.render;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;

import java.util.Vector;

public class XWikiRenderingEngine {

    private Vector renderers = new Vector();

    public XWikiRenderingEngine(XWiki xwiki) throws XWikiException {
        addRenderer(new XWikiJSPRenderer());
        addRenderer(new XWikiVelocityRenderer());
        addRenderer(new XWikiPluginRenderer());
        if (xwiki.Param("xwiki.perl.active", "1").equals("1")) {
            boolean hasPerl = true;
            String pluginspath = xwiki.ParamAsRealPathVerified("xwiki.perl.pluginspath");
            if (pluginspath!=null) {

                String classespath = xwiki.ParamAsRealPathVerified("xwiki.perl.classespath");
                if (classespath==null)
                    classespath = "../classes";

                addRenderer(new XWikiPerlPluginRenderer(xwiki.Param("xwiki.perl.perlpath"),
                        pluginspath, classespath,
                        xwiki.Param("xwiki.perl.javaserverport", "7890"), 0));
            }
        }

        // The first should not removePre
        // The last one should removePre
        renderers.add(new XWikiRadeoxRenderer(false));
        renderers.add(new XWikiWikiBaseRenderer(true));
    }

    public void addRenderer(XWikiRenderer renderer) {
        renderers.add(renderer);
    }

    public XWikiRenderer getRenderer(String name) {
        for (int i=0;i<renderers.size();i++) {
            XWikiRenderer renderer = (XWikiRenderer) renderers.elementAt(i);
            if (renderer.getClass().getName().equals(name))
                return renderer;
        }
        return null;
    }

    public String renderDocument(XWikiDocInterface doc, XWikiContext context) {
        String content = doc.getContent();
        for (int i=0;i<renderers.size();i++)
            content = ((XWikiRenderer)renderers.elementAt(i)).render(content, doc, context);
        return content;
    }

    public String renderDocument(XWikiDocInterface doc, XWikiDocInterface includingdoc, XWikiContext context) {
        String content = doc.getContent();
        for (int i=0;i<renderers.size();i++)
            content = ((XWikiRenderer)renderers.elementAt(i)).render(content, includingdoc, context);
        return content;
    }

}
