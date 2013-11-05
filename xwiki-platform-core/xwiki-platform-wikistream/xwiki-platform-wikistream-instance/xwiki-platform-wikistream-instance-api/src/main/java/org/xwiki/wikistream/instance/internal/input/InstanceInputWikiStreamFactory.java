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
package org.xwiki.wikistream.instance.internal.input;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.internal.InstanceFilter;
import org.xwiki.wikistream.instance.internal.InstanceUtils;
import org.xwiki.wikistream.instance.output.OutputInstanceWikiStreamFactory;
import org.xwiki.wikistream.internal.input.AbstractBeanInputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;

/**
 * A generic xml output wikistream implementation. This class can be used as a test bench to validate various
 * XMLInputStream wiki parsers.
 * 
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Named(InstanceUtils.ROLEHINT)
@Singleton
public class InstanceInputWikiStreamFactory extends
    AbstractBeanInputWikiStreamFactory<InstanceInputProperties, InstanceFilter>
{
    public static final String ROLEHINT = "xwiki+instance";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    public InstanceInputWikiStreamFactory()
    {
        super(WikiStreamType.XWIKI_INSTANCE);

        setName("XWiki instance output stream");
        setDescription("Generates wiki events from XWiki instance.");
    }

    @Override
    public Collection<Class< ? >> getFilterInterfaces() throws WikiStreamException
    {
        List<OutputInstanceWikiStreamFactory> factories;
        try {
            factories = this.componentManagerProvider.get().getInstanceList(OutputInstanceWikiStreamFactory.class);
        } catch (ComponentLookupException e) {
            throw new WikiStreamException(
                "Failed to get regsitered instance of OutputInstanceWikiStreamFactory components", e);
        }

        Set<Class< ? >> filters = new HashSet<Class< ? >>();
        for (OutputInstanceWikiStreamFactory factory : factories) {
            filters.addAll(factory.getFilterInterfaces());
        }

        return filters;
    }
}
