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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.tools.VelocityFormatter;
import org.apache.velocity.runtime.RuntimeConstants;
import org.xwiki.velocity.VelocityContextFactory;
import org.xwiki.velocity.VelocityFactory;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.XWikiVelocityException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class XWikiVelocityRenderer implements XWikiRenderer, XWikiInterpreter
{
    private static final Log LOG = LogFactory.getLog(XWikiVelocityRenderer.class);

    /**
     * {@inheritDoc}
     *
     * @see XWikiInterpreter#interpret(String,XWikiDocument,XWikiContext)
     */
    public String interpret(String content, XWikiDocument contextdoc, XWikiContext context)
    {
        return render(content, contextdoc, contextdoc, context);
    }

    /**
     * {@inheritDoc}
     *
     * @see XWikiRenderer#render(String,XWikiDocument,XWikiDocument,XWikiContext)
     */
    public String render(String content, XWikiDocument contentdoc, XWikiDocument contextdoc,
        XWikiContext context)
    {
        VelocityContext vcontext = prepareContext(context);
        Document previousdoc = (Document) vcontext.get("doc");

        content = context.getUtil().substitute("s/#include\\(/\\\\#include\\(/go", content);

        try {
            vcontext.put("doc", contextdoc.newDocument(context));
            try {
                // We need to do this in case there are any macros in the content
                List<String> macrolist =
                    context.getWiki().getIncludedMacros(contentdoc.getSpace(), content, context);
                if (macrolist != null) {
                    com.xpn.xwiki.XWiki xwiki = context.getWiki();
                    for (String docname: macrolist) {
                        LOG.debug("Pre-including macro topic " + docname);
                        xwiki.include(docname, true, context);
                    }
                }
            } catch (Exception e) {
                // Make sure we never fail
                LOG.warn("Exception while pre-including macro topics", e);
            }

            return evaluate(content, contextdoc.getFullName(), vcontext, context);
        } finally {
            if (previousdoc != null) {
                vcontext.put("doc", previousdoc);
            }
        }
    }

    public void flushCache()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * @todo Move this initialization code to a Skin Manager component.
     */
    public static VelocityEngine getVelocityEngine(XWikiContext context) throws XWikiVelocityException
    {
    	// Note: For improved performance we cache the Velocity Engines in order not to 
    	// recreate them all the time. The key we use is the location to the skin's macro.vm
    	// file since caching on the skin would create more Engines than needed (some skins
    	// don't have a macros.vm file and some skins inherit from others).
    	
    	// Create a Velocity context using the Velocity Engine associated to the current skin's
    	// macros.vm
    	
        // Get the location of the skin's macros.vm file
        String skin = context.getWiki().getSkin(context);
        // We need the path relative to the webapp's home folder so we need to remove all path before
        // the skins/ directory. This is a bit of a hack and should be improved with a proper api.
        String skinMacros = context.getWiki().getSkinFile("macros.vm", skin, context);
        String cacheKey;
        if (skinMacros != null) {
            // We're only using the path starting with the skin name since sometimes we'll
            // get /skins/skins/<skinname>/..., sometimes we get "/skins/<skinname>/..." 
            // and sometimes we get "skins/<skinname>/... 
        	cacheKey = skinMacros.substring(skinMacros.indexOf("skins/"));
        } else {
            // If no skin macros.vm file exists then use a "default" cache id
        	cacheKey = "default";
        }

        // Get the Velocity Engine to use
        VelocityFactory velocityFactory =
            (VelocityFactory) Utils.getComponent(VelocityFactory.ROLE, context);
        VelocityEngine velocityEngine;
        if (velocityFactory.hasVelocityEngine(cacheKey)) {
        	velocityEngine = velocityFactory.getVelocityEngine(cacheKey); 
        } else {
	        // Gather the global Velocity macros that we want to have. These are skin dependent. 
	        Properties properties = new Properties();
	        String macroList = "/templates/macros.vm" + ((skinMacros == null) ? "" : "," + cacheKey); 
	        properties.put(RuntimeConstants.VM_LIBRARY, macroList);
    		velocityEngine = velocityFactory.createVelocityEngine(cacheKey, properties);
        }    	

        return velocityEngine;
    }
    
    /**
     * @todo move this method to the VelocityManager component once we've moved to using the new 
     * Container component + once we have the new XWiki Model.
     */
    public static VelocityContext prepareContext(XWikiContext context)
    {
    	// Note: At each Request the XWiki Context is recreated and thus at each request we need to
    	// populate it with the Velocity context. During the same request (several Velocity
    	// renderings are done in the same request) we cache the Velocity context for better performance.
    	VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        if (vcontext == null) {
	        // Create the Velocity Context
            VelocityContextFactory contextFactory = 
                (VelocityContextFactory) Utils.getComponent(VelocityContextFactory.ROLE, context); 
	        vcontext = contextFactory.createContext();

	        // Initialize it with read only objects (for better performances)
	        
	        // TODO: Remove since it's been replaced by the Number and Date tools. We need to find
	        // all places in our VM and Documents where we might be using it.
	        vcontext.put("formatter", new VelocityFormatter(vcontext));

	        // Put the Util API in the Velocity context.
	        vcontext.put("util", new com.xpn.xwiki.api.Util(context.getWiki(), context));
        }
        
        // We put the com.xpn.xwiki.api.XWiki object into the context and not the
        // com.xpn.xwiki.XWiki one which is for internal use only. In this manner we control what
        // the user can access.
        vcontext.put("xwiki", new XWiki(context.getWiki(), context));

        vcontext.put("request", context.getRequest());
        vcontext.put("response", context.getResponse());

        // We put the com.xpn.xwiki.api.Context object into the context and not the
        // com.xpn.xwiki.XWikiContext one which is for internal use only. In this manner we control
        // what the user can access.
        vcontext.put("context", new Context(context));

        // Save the Velocity Context in the XWiki context so that users can access the objects
        // we've put in it (xwiki, request, response, etc).
        context.put("vcontext", vcontext);

        return vcontext;
    }

    public static String evaluate(String content, String name, VelocityContext vcontext,
        XWikiContext context)
    {
        StringWriter writer = new StringWriter();
        try {
            VelocityEngine velocityEngine = getVelocityEngine(context);
            velocityEngine.evaluate(vcontext, writer, name, content);
            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            Object[] args = {name};
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_RENDERING,
                XWikiException.ERROR_XWIKI_RENDERING_VELOCITY_EXCEPTION,
                "Error while parsing velocity page {0}", e, args);
            return Util.getHTMLExceptionMessage(xe, context);
        }
    }

    private void generateFunction(StringBuffer result, String param, String data,
        XWikiVirtualMacro macro)
    {
        Map namedparams = new HashMap();
        List unnamedparams = new ArrayList();
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

        List macroparam = macro.getParams();
        int j = 0;
        for (int i = 0; i < macroparam.size(); i++) {
            String name = (String) macroparam.get(i);
            String value = (String) namedparams.get(name);
            if (value == null) {
                try {
                    value = (String) unnamedparams.get(j);
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
            String velocityMacrosDocumentName =
                context.getWiki().getXWikiPreference("macros_velocity", context);
            if (velocityMacrosDocumentName.trim().length() > 0) {
                try {
                    XWikiDocument doc =
                        context.getWiki().getDocument(velocityMacrosDocumentName, context);
                    result.append(doc.getContent());
                } catch (XWikiException e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Impossible to load velocity macros doc "
                            + velocityMacrosDocumentName);
                    }
                }
            }
        }
    }

    public String convertSingleLine(String macroname, String param, String allcontent,
        XWikiVirtualMacro macro, XWikiContext context)
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

