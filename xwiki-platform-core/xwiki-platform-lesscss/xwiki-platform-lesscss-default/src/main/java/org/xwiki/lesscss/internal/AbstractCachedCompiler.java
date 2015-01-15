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

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    private CurrentColorThemeGetter currentColorThemeGetter;

    @Inject
    private LESSContext lessContext;

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
        Object mutex = mutexList.get(colorThemeFullName);
        if (mutex == null) {
            mutex = new Object();
            mutexList.put(colorThemeFullName, mutex);
        }
        return mutex;
    }
}
