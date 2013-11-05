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
package org.xwiki.wikistream.instance.internal.output;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.internal.InstanceUtils;
import org.xwiki.wikistream.instance.output.OutputInstanceWikiStreamFactory;
import org.xwiki.wikistream.internal.output.AbstractBeanOutputWikiStream;

/**
 * @version $Id$
 * @since 5.2
 */
@Component
@Named(InstanceUtils.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class InstanceOutputWikiStream extends AbstractBeanOutputWikiStream<InstanceOutputProperties>
{
    @Inject
    private FilterDescriptorManager filterManager;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Override
    public void setProperties(InstanceOutputProperties properties) throws WikiStreamException
    {
        super.setProperties(properties);

        List<OutputInstanceWikiStreamFactory> factories;
        try {
            factories = this.componentManager.get().getInstanceList(OutputInstanceWikiStreamFactory.class);
        } catch (ComponentLookupException e) {
            throw new WikiStreamException(
                "Failed to get regsitered instance of OutputInstanceWikiStreamFactory components", e);
        }

        Object[] filters = new Object[factories.size()];
        int i = 0;
        for (OutputInstanceWikiStreamFactory factory : factories) {
            filters[i++] = factory.createOutputWikiStream(properties).getFilter();
        }

        this.filter = this.filterManager.createCompositeFilter(filters);
    }

    @Override
    public Object getFilter() throws WikiStreamException
    {
        return this.filter;
    }

    @Override
    public void close() throws IOException
    {
        // Nothing to close
    }
}
