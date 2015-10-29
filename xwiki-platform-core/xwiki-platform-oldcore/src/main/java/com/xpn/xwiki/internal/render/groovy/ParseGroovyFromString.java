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
package com.xpn.xwiki.internal.render.groovy;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.DisposableCacheValue;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import groovy.lang.GroovyClassLoader;

/**
 * Helper used to implement {@link com.xpn.xwiki.XWiki#parseGroovyFromString(String, XWikiContext)}.
 * 
 * @version $Id$
 * @since 7.1M1
 */
@Component(roles = ParseGroovyFromString.class)
@Singleton
public class ParseGroovyFromString
{
    @Inject
    private CacheManager cacheManager;

    private Cache<CachedGroovyClass> classCache;

    public void flushCache()
    {
        if (this.classCache != null) {
            this.classCache.dispose();
        }

        this.classCache = null;
    }

    private void initCache(XWikiContext xcontext) throws XWikiException
    {
        int classCacheSize = 100;
        try {
            String capacity = xcontext.getWiki().Param("xwiki.render.groovy.classcache.capacity");
            if (capacity != null) {
                classCacheSize = Integer.parseInt(capacity);
            }
        } catch (Exception e) {
        }

        initCache(classCacheSize, xcontext);
    }

    private void initCache(int iClassCapacity, XWikiContext context) throws XWikiException
    {
        try {
            CacheConfiguration configuration = new LRUCacheConfiguration("xwiki.groovy.class", iClassCapacity);

            this.classCache = this.cacheManager.createNewLocalCache(configuration);
        } catch (CacheException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CACHE, XWikiException.ERROR_CACHE_INITIALIZING,
                "Failed to initilize caches", e);
        }
    }

    private void prepareCache(XWikiContext context)
    {
        try {
            if (this.classCache == null) {
                initCache(context);
            }
        } catch (Exception e) {
        }
    }

    public Object parseGroovyFromString(String script, XWikiContext context) throws XWikiException
    {
        prepareCache(context);

        ClassLoader parentClassLoader = (ClassLoader) context.get("parentclassloader");
        try {
            CachedGroovyClass cgc = this.classCache.get(script);
            Class<?> gc;

            if (cgc == null) {
                GroovyClassLoader gcl =
                    (parentClassLoader == null) ? new GroovyClassLoader() : new GroovyClassLoader(parentClassLoader);
                gc = gcl.parseClass(script);
                cgc = new CachedGroovyClass(gc);
                this.classCache.set(script, cgc);
            } else {
                gc = cgc.getGroovyClass();
            }

            return gc.newInstance();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_GROOVY,
                XWikiException.ERROR_XWIKI_GROOVY_COMPILE_FAILED, "Failed compiling groovy script", e);
        }
    }

    private class CachedGroovyClass implements DisposableCacheValue
    {
        protected Class<?> cl;

        CachedGroovyClass(Class<?> cl)
        {
            this.cl = cl;
        }

        public Class<?> getGroovyClass()
        {
            return this.cl;
        }

        @Override
        public void dispose() throws Exception
        {
            if (this.cl != null) {
                InvokerHelper.removeClass(this.cl);
            }
        }
    }
}
