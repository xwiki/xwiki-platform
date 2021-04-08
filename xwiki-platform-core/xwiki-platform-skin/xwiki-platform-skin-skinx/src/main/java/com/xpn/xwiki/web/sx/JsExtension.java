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
import java.util.Collections;

import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.skinx.SkinExtensionConfiguration;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

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

    /**
     * The JavaScript compressor returned by {@link JsExtension#getCompressor()}. Currently implemented using Closure
     * Compiler.
     */
    public static class JsCompressor implements SxCompressor
    {
        private String sourceMap;

        @Override
        public String compress(String source)
        {
            Compiler compiler = new Compiler();

            // Configure the Closure Compiler. We should use as much as possible the same configuration we use for
            // minifying JavaScript code at build time (with the corresponding Maven plugin).
            CompilerOptions options = new CompilerOptions();

            // Support the latest stable ECMAScript features (excludes drafts) as input.
            options.setLanguageIn(LanguageMode.STABLE);

            // The output language must match the highest ECMAScript version supported by all the browsers we support.
            // See https://dev.xwiki.org/xwiki/bin/view/Community/SupportStrategy/BrowserSupportStrategy
            // As long as we support IE11 we need to output a lower version of ECMAScript.
            options.setLanguageOut(LanguageMode.ECMASCRIPT5_STRICT);

            // Enable the JavaScript strict mode based on the configuration.
            options.setStrictModeInput(getConfig().shouldRunJavaScriptInStrictMode());
            options.setEmitUseStrict(options.expectStrictModeInput());

            // Add support for using the latest JavaScript APIs by including the necessary polyfills in the output. Note
            // that the polyfills won't be included if the JavaScript minification is disabled (e.g. from the debug
            // configuration) so running the unminified code on browsers that don't support the latest APIs (e.g. IE11)
            // won't work, if you use such APIs.
            options.setRewritePolyfills(true);

            // Generate the source map so that we have meaningful error messages. The specified path is not used. We
            // just need to set a path in order to enable source map generation.
            options.setSourceMapOutputPath(getSourceFileName() + ".map");

            // Do some simple optimizations, besides removing the whitespace and renaming local variables. This includes
            // removing dead code and generating warnings that can help us improve the JavaScript code.
            CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);

            // We don't have access to the name of the JavaScript extension that is being compressed so we use a fake
            // name that will be referenced in the generated warning and error messages.
            SourceFile input = SourceFile.fromCode(getSourceFileName(), source);

            // Build the syntax tree from the source JavaScript.
            Result result = compiler.compile(Collections.emptyList(), Collections.singletonList(input), options);

            // Log the warning and the errors that occurred.
            compiler.getWarnings().forEach(warning -> LOGGER.warn("Warning at line [{}], column [{}]: [{}]",
                warning.getLineNumber(), warning.getCharno(), warning.getDescription()));

            compiler.getErrors().forEach(error -> LOGGER.error("Error at line [{}], column [{}]: [{}]",
                error.getLineNumber(), error.getCharno(), error.getDescription()));

            if (result.success) {
                try {
                    // Generate the compressed JavaScript code and its source map.
                    String compressed = compiler.toSource();
                    // Store the source map to be used later. We have to do this after generating the compressed code
                    // because otherwise the source map is empty.
                    this.sourceMap = getSourceMap(compiler);
                    return compressed;
                } catch (Exception e) {
                    LOGGER.warn("Failed to compress JavaScript extension. Root cause is: [{}].",
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }

            // Fall-back on the original source if the compression failed.
            return source;
        }

        private String getSourceFileName()
        {
            // Return the reference of the current document on the XWiki context.
            Provider<XWikiContext> xcontextProvider = Utils.getComponent(XWikiContext.TYPE_PROVIDER);
            return xcontextProvider.get().getDoc().getDocumentReference().toString();
        }

        private String getSourceMap(Compiler compiler) throws IOException
        {
            if (compiler.getSourceMap() != null) {
                StringBuilder sourceMapping = new StringBuilder();
                compiler.getSourceMap().appendTo(sourceMapping, getSourceFileName());
                return sourceMapping.toString();
            } else {
                return null;
            }
        }

        /**
         * @return the last source map that was created by this compressor
         */
        public String getSourceMap()
        {
            return this.sourceMap;
        }

        private SkinExtensionConfiguration getConfig()
        {
            return Utils.getComponent(SkinExtensionConfiguration.class);
        }
    }
}
