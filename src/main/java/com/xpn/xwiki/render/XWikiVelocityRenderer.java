/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ludovic
 * @author sdumitriu
 */

package com.xpn.xwiki.render;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.tools.VelocityFormatter;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XWikiVelocityRenderer implements XWikiRenderer {
    private static final Log log = LogFactory.getLog(com.xpn.xwiki.render.XWikiVelocityRenderer.class);
    public XWikiVelocityRenderer() {

        try {
            Velocity.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String render(String content, XWikiDocument contentdoc, XWikiDocument doc, XWikiContext context) {
        VelocityContext vcontext = null;
        try {
            String name = doc.getFullName();

            int index1 = content.lastIndexOf("\n");
            int index2 = content.lastIndexOf("##");
            if(index1<index2){
                content = content + "\n";
            }
            
            content = context.getUtil().substitute("s/#include\\(/\\\\#include\\(/go", content);
            vcontext = prepareContext(context);

            Document previousdoc = (Document) vcontext.get("doc");

            try {
                vcontext.put("doc", new Document(doc, context));

                try {
                    // We need to do this in case there are any macros in the content
                    List macrolist = context.getWiki().getIncludedMacros(contentdoc.getWeb(), content, context);
                    if (macrolist!=null) {
                        com.xpn.xwiki.XWiki xwiki = context.getWiki();
                        for (int i=0;i<macrolist.size();i++) {
                            String docname = (String) macrolist.get(i);
                            log.debug("Pre-including macro topic " + docname);
                            xwiki.include(docname, context, true);
                        }
                    }
                } catch (Throwable e) {
                    // Make sure we never fail
                    log.warn("Exception while pre-including macro topics", e);
                }

                return evaluate(content, name, vcontext, context);
            } finally {
                if (previousdoc!=null)
                    vcontext.put("doc", previousdoc);
            }

        } finally {
        }
    }

    public void flushCache() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public static VelocityContext prepareContext(XWikiContext context) {
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        if (vcontext==null)
            vcontext = new VelocityContext();
        vcontext.put("formatter", new VelocityFormatter(vcontext));
        vcontext.put("xwiki", new XWiki(context.getWiki(), context));
        vcontext.put("request", context.getRequest());
        vcontext.put("response", context.getResponse());
        vcontext.put("context", new Context(context));

        // Put the Velocity Context in the context
        // so that includes can use it..
        context.put("vcontext", vcontext);
        return vcontext;
    }

    public static String evaluate(String content, String name, VelocityContext vcontext) {
      return evaluate(content, name, vcontext, null);
    }

    public static String evaluate(String content, String name, VelocityContext vcontext, XWikiContext context) {
        StringWriter writer = new StringWriter();
        try {
            boolean result;
            result = XWikiVelocityRenderer.evaluate(vcontext, writer, name,
                                                    new StringReader(content));
            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            Object[] args =  { name };
            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_RENDERING_VELOCITY_EXCEPTION,
                                                        "Error while parsing velocity page {0}", e, args);
            return Util.getHTMLExceptionMessage(xe, context);
        }
    }

    public static boolean evaluate( org.apache.velocity.context.Context context, Writer writer,
                                    String logTag, InputStream instream )
        throws ParseErrorException, MethodInvocationException,
                ResourceNotFoundException, IOException
    {
        /*
         *  first, parse - convert ParseException if thrown
         */

        BufferedReader br  = null;
        String encoding = null;

        try
        {
            encoding = RuntimeSingleton.getString(Velocity.INPUT_ENCODING,Velocity.ENCODING_DEFAULT);
            br = new BufferedReader(  new InputStreamReader( instream, encoding));
        }
        catch( UnsupportedEncodingException  uce )
        {
            String msg = "Unsupported input encoding : " + encoding
                + " for template " + logTag;
            throw new ParseErrorException( msg );
        }

        return XWikiVelocityRenderer.evaluate( context, writer, logTag, br );
    }

    /**
     *  Renders the input reader using the context into the output writer.
     *  To be used when a template is dynamically constructed, or want to
     *  use Velocity as a token replacer.
     *
     *  @param context context to use in rendering input string
     *  @param writer  Writer in which to render the output
     *  @param logTag  string to be used as the template name for log messages
     *                 in case of error
     *  @param reader Reader containing the VTL to be rendered
     *
     *  @return true if successful, false otherwise.  If false, see
     *               Velocity runtime log
     *
     *  @since Velocity v1.1
     */
    public static boolean evaluate( org.apache.velocity.context.Context context, Writer writer,
                                    String logTag, Reader reader )
        throws ParseErrorException, MethodInvocationException,
        	ResourceNotFoundException,IOException
    {
        SimpleNode nodeTree = null;

        try
        {
            nodeTree = RuntimeSingleton.parse( reader, logTag, false);
        }
        catch ( ParseException pex )
        {
            throw  new ParseErrorException( pex.getMessage() );
        }

        /*
         * now we want to init and render
         */

        if (nodeTree != null)
        {
            InternalContextAdapterImpl ica =
                new InternalContextAdapterImpl( context );

            ica.pushCurrentTemplateName( logTag );

            try
            {
                try
                {
                    nodeTree.init( ica, RuntimeSingleton.getRuntimeServices() );
                }
                catch( Exception e )
                {
                    RuntimeSingleton.error("Velocity.evaluate() : init exception for tag = "
                                  + logTag + " : " + e );
                }

                /*
                 *  now render, and let any exceptions fly
                 */

                nodeTree.render( ica, writer );
            }
            finally
            {
                ica.popCurrentTemplateName();
            }

            return true;
        }

        return false;
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

        result.append("#");
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
             result.append(" ");
            result.append("\"");
            result.append(value.replaceAll("\"","\\\\\""));
            result.append("\"");
        }

        if (data!=null) {
            result.append(" ");
            result.append("\"");
            result.append(data.replaceAll("\"","\\\\\""));
            result.append("\"");
        }
        result.append(")");
    }

    private void addVelocityMacros(StringBuffer result, XWikiContext context) {
        Object macroAdded = context.get("velocityMacrosAdded");
        if (macroAdded==null) {
          context.put("velocityMacrosAdded", "1");
          String inclDocName = context.getWiki().getXWikiPreference("macros_velocity", context);
            try {
                XWikiDocument doc = context.getWiki().getDocument(inclDocName, context);
                result.append(doc.getContent());
            } catch (XWikiException e) {
                if (log.isErrorEnabled())
                log.error("Impossible to load velocity macros doc " + inclDocName);
            }
        }
    }

    public String convertSingleLine(String macroname, String param, String allcontent, XWikiVirtualMacro macro, XWikiContext context) {
        StringBuffer result = new StringBuffer();
        addVelocityMacros(result, context);
        generateFunction(result, param, null, macro);
        return result.toString();
    }

    public String convertMultiLine(String macroname, String param, String data, String allcontent, XWikiVirtualMacro macro, XWikiContext context) {
        StringBuffer result = new StringBuffer();
        addVelocityMacros(result, context);
        generateFunction(result, param, data, macro);
        return result.toString();
    }

}

