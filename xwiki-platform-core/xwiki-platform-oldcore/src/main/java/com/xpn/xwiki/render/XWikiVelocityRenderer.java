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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

public class XWikiVelocityRenderer implements XWikiRenderer, XWikiInterpreter
{
    /** Anything which doesn't contain any of these characters cannot be velocity code */
    private static final String VELOCITY_CHARACTERS = "$#";

    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiVelocityRenderer.class);

    @Override
    public String interpret(String content, XWikiDocument contextdoc, XWikiContext context)
    {
        return render(content, contextdoc, contextdoc, context);
    }

    @Override
    public String render(String content, XWikiDocument contentdoc, XWikiDocument contextdoc, XWikiContext context)
    {
        // If there are no # or $ characters than the content doesn't contain any velocity code
        // see: http://velocity.apache.org/engine/releases/velocity-1.5/vtl-reference-guide.html
        if (StringUtils.containsNone(content, VELOCITY_CHARACTERS)) {
            return content;
        }
        VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
        VelocityContext vcontext = velocityManager.getVelocityContext();
        Document previousdoc = (Document) vcontext.get("doc");

        content = context.getUtil().substitute("s/#include\\(/\\\\#include\\(/go", content);

        try {
            vcontext.put("doc", contextdoc.newDocument(context));
            try {
                // We need to do this in case there are any macros in the content
                List<String> macrolist = context.getWiki().getIncludedMacros(contentdoc.getSpace(), content, context);
                if (macrolist != null) {
                    com.xpn.xwiki.XWiki xwiki = context.getWiki();
                    for (String docname : macrolist) {
                        LOGGER
                            .debug("Pre-including macro topic " + docname + " in context " + contextdoc.getFullName());
                        xwiki.include(docname, true, context);
                    }
                }
            } catch (Exception e) {
                // Make sure we never fail
                LOGGER.warn("Exception while pre-including macro topics", e);
            }

            return evaluate(content, contextdoc.getPrefixedFullName(), vcontext, context);
        } finally {
            if (previousdoc != null) {
                vcontext.put("doc", previousdoc);
            }
        }
    }

    @Override
    public void flushCache()
    {
        // To change body of implemented methods use File | Settings | File Templates.
    }

    public static String evaluate(String content, String name, VelocityContext vcontext, XWikiContext context)
    {
        StringWriter writer = new StringWriter();
        try {
            VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
            velocityManager.getVelocityEngine().evaluate(vcontext, writer, name, content);
            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            Object[] args = {name};
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_RENDERING,
                    XWikiException.ERROR_XWIKI_RENDERING_VELOCITY_EXCEPTION, "Error while parsing velocity page {0}",
                    e, args);
            return Util.getHTMLExceptionMessage(xe, context);
        }
    }

    private void generateFunction(StringBuffer result, String param, String data, XWikiVirtualMacro macro)
    {
        Map<String, String> namedparams = new HashMap<String, String>();
        List<String> unnamedparams = new ArrayList<String>();
        if ((param != null) && (!param.trim().equals(""))) {
            String[] params = StringUtils.split(param, "|");
            for (int i = 0; i < params.length; i++) {
                String[] rparam = StringUtils.split(params[i], "=");
                if (rparam.length == 1) {
                    unnamedparams.add(params[i]);
                } else {
                    namedparams.put(rparam[0], rparam[1]);
                }
            }
        }

        result.append("#");
        result.append(macro.getFunctionName());
        result.append("(");

        List<String> macroparam = macro.getParams();
        int j = 0;
        for (int i = 0; i < macroparam.size(); i++) {
            String name = macroparam.get(i);
            String value = namedparams.get(name);
            if (value == null) {
                try {
                    value = unnamedparams.get(j);
                    j++;
                } catch (Exception e) {
                    value = "";
                }
            }
            if (i > 0) {
                result.append(" ");
            }
            result.append("\"");
            result.append(value.replaceAll("\"", "\\\\\""));
            result.append("\"");
        }

        if (data != null) {
            result.append(" ");
            result.append("\"");
            result.append(data.replaceAll("\"", "\\\\\""));
            result.append("\"");
        }
        result.append(")");
    }

    private void addVelocityMacros(StringBuffer result, XWikiContext context)
    {
        Object macroAdded = context.get("velocityMacrosAdded");
        if (macroAdded == null) {
            context.put("velocityMacrosAdded", "1");
            String velocityMacrosDocumentName = context.getWiki().getXWikiPreference("macros_velocity", context);
            if (velocityMacrosDocumentName.trim().length() > 0) {
                try {
                    XWikiDocument doc = context.getWiki().getDocument(velocityMacrosDocumentName, context);
                    result.append(doc.getContent());
                } catch (XWikiException e) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("Impossible to load velocity macros doc " + velocityMacrosDocumentName);
                    }
                }
            }
        }
    }

    public String convertSingleLine(String macroname, String param, String allcontent, XWikiVirtualMacro macro,
        XWikiContext context)
    {
        StringBuffer result = new StringBuffer();
        addVelocityMacros(result, context);
        generateFunction(result, param, null, macro);
        return result.toString();
    }

    public String convertMultiLine(String macroname, String param, String data, String allcontent,
        XWikiVirtualMacro macro, XWikiContext context)
    {
        StringBuffer result = new StringBuffer();
        addVelocityMacros(result, context);
        generateFunction(result, param, data, macro);
        return result.toString();
    }
}
