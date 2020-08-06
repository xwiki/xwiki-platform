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

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

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
            Compiler compiler = new Compiler();

            CompilerOptions options = new CompilerOptions();
            options.setLanguageIn(LanguageMode.STABLE);
            options.setLanguageOut(LanguageMode.ECMASCRIPT5_STRICT);
            options.setRewritePolyfills(true);
            CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);

            // The dummy input name "input.js" is used here so that any warnings or
            // errors will cite line numbers in terms of input.js.
            SourceFile input = SourceFile.fromCode("input.js", source);

            // compile() returns a Result, but it is not needed here.
            Result result = compiler.compile(Collections.emptyList(), Collections.singletonList(input), options);

            compiler.getWarnings().forEach(warning -> LOGGER.warn("Warning at line {}, column {}: [{}]",
                warning.getLineNumber(), warning.getCharno(), warning.getDescription()));

            compiler.getErrors().forEach(error -> LOGGER.error("Error at line {}, column {}: [{}]",
                error.getLineNumber(), error.getCharno(), error.getDescription()));

            if (result.success) {
                try {
                    // The compiler is responsible for generating the compiled code; it is not
                    // accessible via the Result.
                    return compiler.toSource();
                } catch (Exception e) {
                    LOGGER.warn("Failed to compress JS extension: " + e.getMessage());
                }
            }

            return source;
        }
    }
}
