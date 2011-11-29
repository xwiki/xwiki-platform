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
package org.xwiki.extension.jar.internal.handler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.component.annotation.Component;

@Component
public class DefaultJarExtensionClassLoader implements JarExtensionClassLoader
{
    private ExtensionURLClassLoader rootClassLoader;

    private Map<String, ExtensionURLClassLoader> wikiClassLoaderMap = new HashMap<String, ExtensionURLClassLoader>();

    /**
     * Allow overriding the system classloader during tests.
     * @return a ClassLoader to be used as the system parent
     */
    protected ClassLoader getSystemClassLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public ExtensionURLClassLoader getURLClassLoader(String namespace, boolean create)
    {
        if (this.rootClassLoader == null && create) {
            this.rootClassLoader = new ExtensionURLClassLoader(new URI[] {}, getSystemClassLoader(), null);
        }

        ExtensionURLClassLoader wikiClassLoader = this.rootClassLoader;

        if (namespace != null) {
            wikiClassLoader = this.wikiClassLoaderMap.get(namespace);

            if (wikiClassLoader == null) {
                if (create) {
                    wikiClassLoader = new ExtensionURLClassLoader(new URI[] {}, this.rootClassLoader, namespace);
                    this.wikiClassLoaderMap.put(namespace, wikiClassLoader);
                } else {
                    wikiClassLoader = this.rootClassLoader;
                }
            }
        }

        return wikiClassLoader;
    }

    @Override
    public void dropURLClassLoaders()
    {
        if (this.rootClassLoader != null) {
            for (String namespace : wikiClassLoaderMap.keySet()) {
                dropURLClassLoader(namespace);
            }
            this.rootClassLoader = null;
        }
    }

    @Override
    public void dropURLClassLoader(String namespace)
    {
        if (this.rootClassLoader != null) { 
           if (this.wikiClassLoaderMap.get(namespace) != null) {
               this.wikiClassLoaderMap.put(namespace, null);
           }
        }
    }
}
