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
package org.xwiki.livedata.internal.rest;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.namespace.NamespaceUtils;
import org.xwiki.model.ModelContext;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.model.reference.WikiReference;

/**
 * Internal component providing operations to initialize the context for the Live Data resources.
 *
 * @version $Id$
 * @since 12.10.9
 * @since 13.4.2
 * @since 13.5.1
 * @since 13.6RC1
 */
@Component(roles = LiveDataResourceContextInitializer.class)
@Singleton
public class LiveDataResourceContextInitializer
{
    @Inject
    private ModelContext modelContext;

    /**
     * Updates the wiki in the context if the provided namespace is not {@code null} and is of type wiki.
     *
     * @param namespace the requested namespace (for instance, {@code "wiki:s1"})
     */
    public void initialize(String namespace)
    {
        // Switch the wikiId of the context if a namespace is provided and is of type wiki (for instance "wiki:s1").
        Namespace namespaceObj = NamespaceUtils.toNamespace(namespace);
        if (namespaceObj != null && Objects.equals(namespaceObj.getType(), WikiNamespace.TYPE)) {
            this.modelContext.setCurrentEntityReference(new WikiReference(namespaceObj.getValue()));
        }
    }
}
