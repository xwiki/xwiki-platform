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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

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
    protected WikiDescriptorManager wikiDescriptorManager;

    @Inject
    protected DocumentReferenceResolver<String> referenceResolver;

    @Inject
    protected EntityReferenceSerializer<String> referenceSerializer;

    private Map<String, Object> mutexList = new HashMap<>();

    /**
     * Compile an output corresponding to a LESS filename.
     * @param fileName name of the file
     * @param force force the computation, even if the output is already in the cache (not recommended)
     * @return the desired object
     * @throws LESSCompilerException if problems occur
     */
    public T compileFromSkinFile(String fileName, boolean force) throws LESSCompilerException
    {
        T result;

        // Get information about the context
        String wikiId = wikiDescriptorManager.getCurrentWikiId();
        XWikiContext context = xcontextProvider.get();
        String skin = context.getWiki().getBaseSkin(context);
        XWikiRequest request = context.getRequest();

        // Getting the full name representation of colorTheme
        DocumentReference colorThemeReference = referenceResolver.resolve(request.getParameter("colorTheme"));
        String colorTheme = referenceSerializer.serialize(colorThemeReference);

        // Check that the color theme exists, to avoid a DOS if some user tries to compile a skin file
        // with random colorTheme names
        if (!context.getWiki().exists(colorThemeReference, context)) {
            colorTheme = "default";
        }

        // Only one computation is allowed in the same time on a wiki, then the waiting threads will be able to use
        // the last result stored in the cache
        synchronized (getMutex(wikiId)) {

            // Check if the result is in the cache
            if (!force) {
                result = cache.get(fileName, wikiId, skin, colorTheme);
                if (result != null) {
                    return result;
                }
            }

            // Either the result was in the cache or the force flag is set to true, we need to compile
            result = compile(fileName, force);
            cache.set(fileName, wikiId, skin, colorTheme, result);

        }

        return result;
    }

    protected abstract T compile(String fileName, boolean force) throws LESSCompilerException;

    private synchronized Object getMutex(String wikiId)
    {
        Object mutex = mutexList.get(wikiId);
        if (mutex == null) {
            mutex = new Object();
            mutexList.put(wikiId, mutex);
        }
        return mutex;
    }
}
