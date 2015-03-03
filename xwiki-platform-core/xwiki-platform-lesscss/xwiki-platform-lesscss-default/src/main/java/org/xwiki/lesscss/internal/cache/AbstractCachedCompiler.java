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

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.lesscss.cache.LESSCache;
import org.xwiki.lesscss.colortheme.ColorThemeReference;
import org.xwiki.lesscss.colortheme.ColorThemeReferenceFactory;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.internal.LESSContext;
import org.xwiki.lesscss.internal.colortheme.CurrentColorThemeGetter;
import org.xwiki.lesscss.internal.compiler.DefaultIntegratedLESSCompiler;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.skin.SkinReference;
import org.xwiki.lesscss.skin.SkinReferenceFactory;

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
    protected CurrentColorThemeGetter currentColorThemeGetter;

    @Inject
    protected SkinReferenceFactory skinReferenceFactory;

    @Inject
    protected ColorThemeReferenceFactory colorThemeReferenceFactory;

    @Inject
    protected LESSContext lessContext;

    /**
     * Get the result of the compilation.
     * @param lessResourceReference reference to the LESS content
     * @param includeSkinStyle include the main LESS file of the skin in order to have variables and mix-ins
     * defined there
     * @param useVelocity either or not the resource be parsed by Velocity before compiling it
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the desired object
     * @throws LESSCompilerException if problems occur
     */
    public T getResult(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean useVelocity,
        boolean force) throws LESSCompilerException
    {
        XWikiContext context = xcontextProvider.get();
        String skin = context.getWiki().getSkin(context);
        return getResult(lessResourceReference, includeSkinStyle, useVelocity, skin, force);
    }

    /**
     * Get the result of the compilation.
     * @param lessResourceReference reference to the LESS content
     * @param includeSkinStyle include the main LESS file of the skin in order to have variables and mix-ins
     * defined there
     * @param useVelocity either or not the resource be parsed by Velocity before compiling it
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @param skin name of the skin used for the context
     * @return the desired object
     * @throws LESSCompilerException if problems occur
     */
    public T getResult(LESSResourceReference lessResourceReference, boolean includeSkinStyle, boolean useVelocity,
        String skin, boolean force) throws LESSCompilerException
    {
        // If the cache is disabled, we just compile
        if (lessContext.isCacheDisabled()) {
            return compiler.compute(lessResourceReference, includeSkinStyle, useVelocity, true, skin);
        }

        T result = null;

        SkinReference skinReference = skinReferenceFactory.createReference(skin);
        ColorThemeReference colorThemeReference = colorThemeReferenceFactory.createReference(
                currentColorThemeGetter.getCurrentColorTheme(true, "default"));

        // Only one computation is allowed in the same time per color theme, then the waiting threads will be able to
        // use the last result stored in the cache.
        Object mutex = cache.getMutex(lessResourceReference, skinReference, colorThemeReference);
        synchronized (mutex) {

            // Check if the result is in the cache
            if (!force) {
                result = cache.get(lessResourceReference, skinReference, colorThemeReference);
                if (result != null) {
                    // The LESS file contains Velocity code that call resources (ie: $xwiki.getSkinFile), and the HTML
                    // exporter listens these calls to know which resources must be exported.
                    // If we only use the cache, we would have a correct CSS file but some resources will be missing.
                    // So we need to execute the velocity again, even if the LESS file is cached.
                    // To perform this quickly, we do not recompile the LESS code (which would be useless anyway), but
                    // we only do the Velocity Execution step.
                    if (lessContext.isHtmlExport() && useVelocity && this instanceof DefaultIntegratedLESSCompiler) {
                        compiler.compute(lessResourceReference, includeSkinStyle, true, false, skin);
                    }
                    return cloneResult(result);
                }
            }

            // Either the result was in the cache or the force flag is set to true, we need to compile
            try {
                result = compiler.compute(lessResourceReference, includeSkinStyle, useVelocity, true, skin);
            } finally {
                // We must cache the result, even if the compilation have failed, to prevent re-compiling again and
                // again (the compilation will still fail until the LESS resource is updated so it useless to retry).
                // FixMe: it actually does not work, since setting null in the cache will not prevent an other
                // computation.
                cache.set(lessResourceReference, skinReference, colorThemeReference, result);
            }
        }

        return cloneResult(result);
    }

    /**
     * Returns a clone of the result to avoid returning the instance stored in the cache. Need to be implemented by
     * subclasses.
     * @param toClone result to clone
     * @return a clone of the result
     *
     * @since 6.4M3
     */
    protected abstract T cloneResult(T toClone);
}
