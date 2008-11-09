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
 *
 */
package com.xpn.xwiki.web.sx;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * JavaScript extension.
 * 
 * @version $Id: $
 * @since 1.7M2
 */
public class JsExtension implements Extension
{
    /** Logging helper. */
    private static final Log LOG = LogFactory.getLog(JsExtension.class);

    /**
     * {@inheritDoc}
     * 
     * @see Extension#getClassName()
     */
    public String getClassName()
    {
        return "XWiki.JavaScriptExtension";
    }

    /**
     * {@inheritDoc}
     * 
     * @see SxSource.Extension#getContentType()()
     */
    public String getContentType()
    {
        return "text/javascript; charset=UTF-8";
    }

    /**
     * {@inheritDoc}
     * 
     * @see Extension#getCompressor()
     */
    public SxCompressor getCompressor()
    {
        return new SxCompressor()
        {
            public String compress(String source)
            {
                try {
                    JavaScriptCompressor compressor =
                        new JavaScriptCompressor(new StringReader(source), new ErrorReporter()
                        {
                            public void error(String arg0, String arg1, int arg2, String arg3, int arg4)
                            {
                                LOG.warn("Error minimizing JSX object");
                            }

                            public EvaluatorException runtimeError(String arg0, String arg1, int arg2, String arg3,
                                int arg4)
                            {
                                return null;
                            }

                            public void warning(String arg0, String arg1, int arg2, String arg3, int arg4)
                            {
                            }

                        });
                    StringWriter out = new StringWriter();
                    compressor.compress(out, -1, true, false, false, false);
                    return out.toString();
                } catch (IOException e) {
                } catch (EvaluatorException e) {
                }
                return source;
            }
        };
    }
}
