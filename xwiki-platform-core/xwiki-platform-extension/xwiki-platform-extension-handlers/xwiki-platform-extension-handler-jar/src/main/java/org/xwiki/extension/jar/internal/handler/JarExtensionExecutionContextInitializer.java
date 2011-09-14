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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;

@Component
@Singleton
@Named("jarextension")
public class JarExtensionExecutionContextInitializer implements ExecutionContextInitializer
{
    @Inject
    private JarExtensionClassLoader jarExtensionClassLoader;

    @Inject
    private ModelContext modelContext;

    @Override
    public void initialize(ExecutionContext context) throws ExecutionContextException
    {
        String currentWikiId = null;

        EntityReference currentEntityReference = this.modelContext.getCurrentEntityReference();
        if (currentEntityReference != null) {
            EntityReference currentWikiReference =
                this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI);

            if (currentWikiReference != null) {
                currentWikiId = currentWikiReference.getName();
            }
        }

        ExtensionURLClassLoader extensionClassLoader =
            this.jarExtensionClassLoader.getURLClassLoader(currentWikiId, false);

        if (extensionClassLoader != null) {
            Thread.currentThread().setContextClassLoader(extensionClassLoader);
        }
    }
}
