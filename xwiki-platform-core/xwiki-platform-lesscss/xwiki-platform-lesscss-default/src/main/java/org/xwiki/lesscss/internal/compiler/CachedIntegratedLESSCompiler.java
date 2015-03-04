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
package org.xwiki.lesscss.internal.compiler;

import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.internal.cache.CachedCompilerInterface;
import org.xwiki.lesscss.internal.compiler.less4j.Less4jCompiler;
import org.xwiki.lesscss.resources.LESSResourceReader;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;

import com.github.sommeri.less4j.Less4jException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Compile a LESS resource in a particular context (@see org.xwiki.lesscss.compiler.IntegratedLESSCompiler}.
 * To be used with AbstractCachedCompiler.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component(roles = CachedIntegratedLESSCompiler.class)
@Singleton
public class CachedIntegratedLESSCompiler implements CachedCompilerInterface<String>
{
    /**
     * The name of the file holding the main skin style (on which Velocity is always executed).
     */
    public static final String MAIN_SKIN_STYLE_FILENAME = "style.less.vm";
    
    private static final String SKIN_CONTEXT_KEY = "skin";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private LESSResourceReader lessResourceReader;
    
    @Inject
    private Less4jCompiler less4JCompiler;

    @Override
    public String compute(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean useVelocity,
        boolean useLESS, String skin) throws LESSCompilerException
    {
        StringWriter source = new StringWriter();

        try {
            if (lessResourceReference instanceof LESSSkinFileResourceReference || includeSkinStyle) {
                
                if (includeSkinStyle) {
                    // Add the import line to the LESS resource.
                    // We import this file to be able to use variables and mix-ins defined in it.
                    // But we don't want it in the output.
                    source.write(String.format("@import (reference) \"%s\";%s", MAIN_SKIN_STYLE_FILENAME,
                        System.lineSeparator()));
                }

                // Get the content of the LESS resource
                source.write(lessResourceReader.getContent(lessResourceReference, skin));
            }

            // Parse the LESS content with Velocity
            String lessCode = source.toString();
            if (useVelocity) {
                lessCode = executeVelocity(lessCode, skin);
            }
            
            // Compile the LESS code
            if (useLESS) {
                return less4JCompiler.compile(lessCode, skin);
            }
            
            // Otherwise return the raw LESS code
            return lessCode;
        } catch (Less4jException e) {
            throw new LESSCompilerException(String.format("Failed to compile the resource [%s] with LESS.",
                    lessResourceReference), e);
        }
    }

    private String executeVelocity(String source, String skin)
    {
        // Get the XWiki object
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        String currentSkin = xwiki.getSkin(xcontext);

        try {
            // Trick: change the current skin in order to compile the LESS file as if the specified skin
            // was the current skin
            if (!currentSkin.equals(skin)) {
                xcontext.put(SKIN_CONTEXT_KEY, skin);
            }
            return xwiki.parseContent(source, xcontext);

        } finally {
            // Reset the current skin to the old value
            if (!currentSkin.equals(skin)) {
                xcontext.put(SKIN_CONTEXT_KEY, currentSkin);
            }
        }
    }
}
