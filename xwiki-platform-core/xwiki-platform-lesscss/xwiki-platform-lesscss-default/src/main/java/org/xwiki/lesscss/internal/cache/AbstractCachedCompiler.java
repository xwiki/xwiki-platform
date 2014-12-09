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
package org.xwiki.lesscss.internal.cache;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.lesscss.LESSCache;
import org.xwiki.lesscss.LESSCompilerException;
import org.xwiki.lesscss.LESSResourceReference;
import org.xwiki.lesscss.internal.colortheme.CurrentColorThemeGetter;

import com.xpn.xwiki.XWikiContext;

/**
 * Implements a cache system to prevent the compiler to be called too often.
 *
 * @param <T> class of the expected results
 *
 * @since 6.4M2
 * @version $Id$
 */
public abstract class AbstractCachedCompiler<T>
{
    protected LESSCache<T> cache;

    protected CachedCompilerInterface<T> compiler;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    private CurrentColorThemeGetter currentColorThemeGetter;

    @Inject
    private CacheKeyFactory cacheKeyFactory;

    private Map<CacheKey, Object> mutexList = new HashMap<>();

    /**
     * Get the result of the compilation.
     * @param lessResourceReference reference to the LESS content
     * @param includeSkinStyle include the main LESS file of the skin in order to have variables and mix-ins
     * defined there
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the desired object
     * @throws LESSCompilerException if problems occur
     */
    public T getResult(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean force)
        throws LESSCompilerException
    {
        XWikiContext context = xcontextProvider.get();
        String skin = context.getWiki().getSkin(context);
        return getResult(lessResourceReference, includeSkinStyle, skin, force);
    }

    /**
     * Get the result of the compilation.
     * @param lessResourceReference reference to the LESS content
     * @param includeSkinStyle include the main LESS file of the skin in order to have variables and mix-ins
     * defined there
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @param skin name of the skin used for the context
     * @return the desired object
     * @throws LESSCompilerException if problems occur
     */
    public T getResult(LESSResourceReference lessResourceReference, boolean includeSkinStyle, String skin,
        boolean force) throws LESSCompilerException
    {
        T result;

        String colorTheme = currentColorThemeGetter.getCurrentColorTheme("default");

        // Only one computation is allowed in the same time per color theme, then the waiting threads will be able to
        // use the last result stored in the cache
        synchronized (getMutex(skin, colorTheme, lessResourceReference)) {

            // Check if the result is in the cache
            if (!force) {
                result = cache.get(lessResourceReference, skin, colorTheme);
                if (result != null) {
                    return result;
                }
            }

            // Either the result was in the cache or the force flag is set to true, we need to getResult
            result = compiler.compute(lessResourceReference, includeSkinStyle, skin);
            cache.set(lessResourceReference, skin, colorTheme, result);
        }

        return result;
    }

    private synchronized Object getMutex(String skin, String colorTheme, LESSResourceReference lessResourceReference)
    {
        CacheKey cacheKey = cacheKeyFactory.getCacheKey(skin, colorTheme, lessResourceReference);
        Object mutex = mutexList.get(cacheKey);
        if (mutex == null) {
            mutex = new Object();
            mutexList.put(cacheKey, mutex);
        }
        return mutex;
    }
}
