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
 * Date: 6 juil. 2004
 * Time: 12:22:18
 */
package com.xpn.xwiki.render.groovy;

import groovy.text.Template;
import groovy.lang.Writable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.Writer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiVirtualMacro;
import com.xpn.xwiki.web.XWikiRequest;

public class XWikiGroovyRenderer implements XWikiRenderer {
    private static final Log log = LogFactory.getLog(com.xpn.xwiki.render.groovy.XWikiGroovyRenderer.class);
    private XWikiCache cache;

    public XWikiGroovyRenderer() {
    }


    public void flushCache() {
        if (cache!=null)
         cache.flushAll();
    }

    public Map prepareContext(XWikiContext context) {
            if (cache==null) {
                 cache = context.getWiki().getCacheService().newCache(100);
            }

            Map gcontext = (Map) context.get("gcontext");
            if (gcontext==null) {
                gcontext = new HashMap();
                gcontext.put("xwiki", new XWiki(context.getWiki(), context));
                gcontext.put("request", context.getRequest());
                gcontext.put("response", context.getResponse());
                gcontext.put("context", new Context(context));


                // Put the Grrovy Context in the context
                // so that includes can use it..
                context.put("gcontext", gcontext);
                //add XWikiMessageTool to the context
                if (context.get("msg") != null)
                	gcontext.put("msg", context.get("msg"));
                else
                	context.getWiki().prepareResources(context);                
            }
            return gcontext;
        }

    public String evaluate(String content, String name, Map gcontext) {
        GroovyTemplateEngine engine = new GroovyTemplateEngine();
        Template template = null;
        boolean refresh = false;
        try {
            XWikiRequest request = (XWikiRequest) gcontext.get("request");
            refresh = "1".equals(request.get("refresh"));
        } catch(Exception e) {
        }
        try {
            try {
                if (refresh) {
                    template = engine.createTemplate(content);
                    cache.putInCache(content, template);
                } else {
                    template = (Template) cache.getFromCache(content);
                }
            } catch (XWikiCacheNeedsRefreshException e) {
                template = engine.createTemplate(content);
                cache.putInCache(content, template);
            } finally {
                cache.cancelUpdate(content);
            }
            Writable writable = template.make(gcontext);
            String result = writable.toString();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Object[] args =  { name };

            String title;
            String text;

            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_RENDERING_GROOVY_EXCEPTION,
                                                        "Error while parsing groovy page {0}", e, args);
            title = xe.getMessage();
            text = com.xpn.xwiki.XWiki.getFormEncoded(xe.getFullMessage());

            return "<a href=\"\" onclick=\"document.getElementById('xwikierror').style.display='block'; return false;\">"
                    + title + "</a><div id=\"xwikierror\" style=\"display: none;\"><pre>\n"
                    + text + "</pre></div>";
        }
    }

    public String render(String content, XWikiDocument contentdoc, XWikiDocument contextdoc, XWikiContext context) {
        if (content.indexOf("<%")==-1)
            return content;

        if (!context.getWiki().getRightService().hasProgrammingRights(contentdoc, context))
             return content;

        Map gcontext = null;
        try {
            String name = contextdoc.getFullName();
            gcontext = prepareContext(context);
            Document previousdoc = (Document) gcontext.get("doc");
            Writer previouswriter  = (Writer) gcontext.get("out");

            try {
                gcontext.put("doc", new Document(contextdoc, context));
                return evaluate(content, name, gcontext);
            } finally {
                if (previousdoc!=null)
                    gcontext.put("doc", previousdoc);
                if (previouswriter!=null)
                    gcontext.put("out", previouswriter);    
            }

        } finally {
        }
    }

    private void generateFunction(StringBuffer result, String param, String data, XWikiVirtualMacro macro) {
        Map namedparams = new HashMap();
        List unnamedparams = new ArrayList();
        if ((param!=null)&&(!param.trim().equals(""))) {
            String[] params = StringUtils.split(param, "|");
            for (int i=0;i<params.length;i++) {
              String[] rparam = StringUtils.split(params[i], "=");
              if (rparam.length==1)
                  unnamedparams.add(params[i]);
              else
                  namedparams.put(rparam[0], rparam[1]);
            }
        }

        result.append("<% ");
        result.append(macro.getFunctionName());
        result.append("(");

        List macroparam = macro.getParams();
        int j = 0;
        for (int i=0;i<macroparam.size();i++) {
            String name = (String) macroparam.get(i);
            String value = (String) namedparams.get(name);
            if (value==null) {
                try {
                    value = (String) unnamedparams.get(j);
                    j++;
                } catch (Exception e) {
                    value = "";
                }
            }
            if (i>0)
             result.append(",");
            result.append("\"");
            result.append(value.replaceAll("\"","\\\\\""));
            result.append("\"");
        }

        if (data!=null) {
            result.append(",");
            result.append("\"");
            result.append(data.replaceAll("\"","\\\\\""));
            result.append("\"");
        }
        result.append(") %>");
    }

    private void addGroovyMacros(StringBuffer result, XWikiContext context) {
        Object macroAdded = context.get("groovyMacrosAdded");
        if (macroAdded==null) {
          context.put("groovyMacrosAdded", "1");
          String inclDocName = context.getWiki().getXWikiPreference("macros_groovy", context);
            try {
                XWikiDocument doc = context.getWiki().getDocument(inclDocName, context);
                result.append(doc.getContent());
            } catch (XWikiException e) {
                if (log.isErrorEnabled())
                log.error("Impossible to load groovy macros doc " + inclDocName);
            }
        }
    }

    public String convertSingleLine(String macroname, String param, String allcontent, XWikiVirtualMacro macro, XWikiContext context) {
        StringBuffer result = new StringBuffer();
        addGroovyMacros(result, context);
        generateFunction(result, param, null, macro);
        return result.toString();
    }

    public String convertMultiLine(String macroname, String param, String data, String allcontent, XWikiVirtualMacro macro, XWikiContext context) {
        StringBuffer result = new StringBuffer();
        addGroovyMacros(result, context);
        generateFunction(result, param, data, macro);
        return result.toString();
    }

}
