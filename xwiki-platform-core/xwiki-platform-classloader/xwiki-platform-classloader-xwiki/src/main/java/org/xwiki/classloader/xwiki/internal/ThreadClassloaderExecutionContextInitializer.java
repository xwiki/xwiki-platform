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
package org.xwiki.classloader.xwiki.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.classloader.NamespaceURLClassLoader;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceValueProvider;

/**
 * Take care of setting the proper Thread classloader when new request starts.
 * 
 * @version $Id$
 */
@Component
@Singleton
@Named("threadclassloader")
public class ThreadClassloaderExecutionContextInitializer implements ExecutionContextInitializer
{
    /**
     * Used to get the classloader corresponding to the current wiki.
     */
    @Inject
    private ClassLoaderManager classLoaderManager;

    /**
     * Used to get the current wiki.
     */
    @Inject
    @Named("current")
    private EntityReferenceValueProvider provider;

    @Override
    public void initialize(ExecutionContext context) throws ExecutionContextException
    {
        String currentWikiId = this.provider.getDefaultValue(EntityType.WIKI);

        NamespaceURLClassLoader extensionClassLoader =
            this.classLoaderManager.getURLClassLoader(currentWikiId != null ? "wiki:" + currentWikiId : null, false);

        if (extensionClassLoader != null) {
            // TODO: This overwrite the current context classloader. We should instead save the current classloader in
            // a stack and add a restore() method to put it back (and ensure that at the end of a request the stack is
            // always empty!).
            Thread.currentThread().setContextClassLoader(extensionClassLoader);
        }
    }
}
