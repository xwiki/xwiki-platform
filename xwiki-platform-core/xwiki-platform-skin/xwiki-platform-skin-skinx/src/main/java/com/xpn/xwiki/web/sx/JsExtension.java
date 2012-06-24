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
package com.xpn.xwiki.web.sx;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.MessageFormat;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * JavaScript extension.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class JsExtension implements Extension
{
    /** Logging helper. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsExtension.class);

    @Override
    public String getClassName()
    {
        return "XWiki.JavaScriptExtension";
    }

    @Override
    public String getContentType()
    {
        return "text/javascript; charset=UTF-8";
    }

    @Override
    public SxCompressor getCompressor()
    {
        return new JsCompressor();
    }

    /** The JavaScript compressor which is returned by getCompressor. Currently implemented using YUI Compressor. */
    private static class JsCompressor implements SxCompressor
    {
        @Override
        public String compress(String source)
        {
            try {
                ErrorReporter reporter = new CustomErrorReporter();
                JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(source), reporter);
                StringWriter out = new StringWriter();
                compressor.compress(out, -1, true, false, false, false);
                return out.toString();
            } catch (IOException ex) {
                LOGGER.info("Failed to write the compressed output: " + ex.getMessage());
            } catch (EvaluatorException ex) {
                LOGGER.info("Failed to parse the JS extension: " + ex.getMessage());
            } catch (Exception ex) {
                LOGGER.warn("Failed to compress JS extension: " + ex.getMessage());
            }
            return source;
        }

        /** A Javascript error reporter which logs errors with the XWiki logging system. */
        private static class CustomErrorReporter implements ErrorReporter
        {
            @Override
            public void error(String message, String filename, int lineNumber, String context, int column)
            {
                LOGGER.warn(MessageFormat.format("Error at line {2}, column {3}: {0}. Caused by: [{1}]",
                    message, context, lineNumber, column));
            }

            @Override
            public EvaluatorException runtimeError(String message, String filename, int lineNumber,
                String context, int column)
            {
                LOGGER.error(MessageFormat.format("Runtime error minimizing JSX object: {0}", message));
                return null;
            }

            @Override
            public void warning(String message, String filename, int lineNumber, String context, int column)
            {
                LOGGER.info(MessageFormat.format("Warning at line {2}, column {3}: {0}. Caused by: [{1}]",
                    message, context, lineNumber, column));
            }
        }
    }
}
