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
package com.xpn.xwiki.internal.namespace;

import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation for {@link NamespaceContextExecutor}.
 *
 * @version $Id$
 * @since 10.6RC1
 * @since 10.5
 * @since 9.11.6
 */
@Component
@Singleton
public class DefaultNamespaceContextExecutor implements NamespaceContextExecutor
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public <V> V execute(Namespace namespace, Callable<V> callable) throws Exception
    {
        if (!WikiNamespace.TYPE.equals(namespace.getType())) {
            throw new UnsupportedOperationException("The namespace type [%s] is not supported yet.");
        }

        XWikiContext context = xcontextProvider.get();
        WikiReference currentWiki = context.getWikiReference();

        try {
            context.setWikiReference(new WikiReference(namespace.getValue()));
            return callable.call();
        } finally {
            context.setWikiReference(currentWiki);
        }
    }
}
