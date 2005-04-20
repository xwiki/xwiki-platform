/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 26 nov. 2003
 * Time: 21:00:05
 */
package com.xpn.xwiki.render;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.tools.VelocityFormatter;

import java.io.*;

public class XWikiVelocityRenderer implements XWikiRenderer {

    public XWikiVelocityRenderer() {
        try {
            Velocity.init();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }
    }

    public String render(String content, XWikiDocument contentdoc, XWikiDocument doc, XWikiContext context) {
        VelocityContext vcontext = null;
        try {
            String name = doc.getFullName();
            content = context.getUtil().substitute("s/#include\\(/\\\\#include\\(/go", content);
            vcontext = prepareContext(context);

            Document previousdoc = (Document) vcontext.get("doc");

            try {
                vcontext.put("doc", new Document(doc, context));
                return evaluate(content, name, vcontext);
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
            return Util.getHTMLExceptionMessage(xe, ((Context)vcontext.get("context")).getContext());
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
     *  @param out  Writer in which to render the output
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


}

