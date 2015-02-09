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
package org.xwiki.lesscss.internal;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.lesscss.LESSCache;
import org.xwiki.lesscss.LESSCompilerException;

import com.xpn.xwiki.XWikiContext;

/**
 * Implements a cache system to prevent the compiler to be called too often.
 *
 * @param <T> class of the expected results
 *
 * @since 6.1M2
 * @version $Id$
 */
public abstract class AbstractCachedCompiler<T>
{
    protected LESSCache<T> cache;

    /**
     * Whether or not the cache should handle the current XWikiContext object (true by default).
     * @since 6.2.6
     */
    protected boolean isContextHandled = true;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected CurrentColorThemeGetter currentColorThemeGetter;

    @Inject
    protected LESSContext lessContext;
    
    @Inject
    protected XWikiContextCacheKeyFactory xwikiContextCacheKeyFactory;
    
    @Inject
    protected Logger logger;

    private Map<String, Object> mutexList = new HashMap<>();

    /**
     * Compile an output corresponding to a LESS filename, located on the current skin.
     * @param fileName name of the file
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the desired object
     * @throws LESSCompilerException if problems occur
     */
    public T compileFromSkinFile(String fileName, boolean force) throws LESSCompilerException
    {
        XWikiContext context = xcontextProvider.get();
        String skin = context.getWiki().getSkin(context);
        return compileFromSkinFile(fileName, skin, force);
    }

    /**
     * Compile an output corresponding to a LESS filename.
     * @param fileName name of the file
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @param skin name of the skin where the LESS file is located
     * @return the desired object
     * @throws LESSCompilerException if problems occur
     */
    public T compileFromSkinFile(String fileName, String skin, boolean force) throws LESSCompilerException
    {
        // If the cache is disabled, we just compile
        if (lessContext.isCacheDisabled()) {
            return compile(fileName, skin, force);
        }

        T result;

        String colorTheme = currentColorThemeGetter.getCurrentColorTheme("default");

        // Only one computation is allowed in the same time per color theme, then the waiting threads will be able to
        // use the last result stored in the cache
        synchronized (getMutex(colorTheme)) {

            // Check if the result is in the cache
            if (!force) {
                result = cache.get(fileName, skin, colorTheme);
                if (result != null) {

                    // The LESS file contains Velocity code that call resources (ie: $xwiki.getSkinFile), and the HTML
                    // exporter listens these calls to know which resources must be exported.
                    // If we only use the cache, we would have a correct CSS file but some resources will be missing.
                    // So we need to execute the velocity again, even if the LESS file is cached.
                    // To perform this quickly, we do not recompile the LESS code (which would be useless anyway), but
                    // we only do the Velocity Execution step.
                    // (quick backport of http://jira.xwiki.org/browse/XWIKI-11731)
                    if (lessContext.isHtmlExport() && this instanceof DefaultLESSSkinFileCompiler) {
                        compile(fileName, skin, force);
                    }
                    
                    return result;
                }
            }

            // Either the result was in the cache or the force flag is set to true, we need to compile
            result = compile(fileName, skin, force);
            cache.set(fileName, skin, colorTheme, result);

        }

        return result;
    }

    protected abstract T compile(String fileName, String skin, boolean force) throws LESSCompilerException;

    private synchronized Object getMutex(String colorThemeFullName)
    {
        String cacheKey = colorThemeFullName;
        
        if (this.isContextHandled) {
            try {
                String xcontext = xwikiContextCacheKeyFactory.getCacheKey();
                cacheKey += "_" + xcontext;
            } catch (LESSCompilerException e) {
                logger.warn("Failed to generate a cache key handling the XWikiContext", e);
            }
        }
        
        Object mutex = mutexList.get(cacheKey);
        if (mutex == null) {
            mutex = new Object();
            mutexList.put(colorThemeFullName, mutex);
        }
        return mutex;
    }
}
