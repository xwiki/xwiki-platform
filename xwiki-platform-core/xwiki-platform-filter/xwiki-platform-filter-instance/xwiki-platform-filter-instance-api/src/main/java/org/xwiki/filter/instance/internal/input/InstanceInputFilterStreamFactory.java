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
package org.xwiki.filter.instance.internal.input;

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
import org.xwiki.component.phase.InitializationException;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.descriptor.CompositeFilterStreamDescriptor;
import org.xwiki.filter.descriptor.FilterStreamDescriptor;
import org.xwiki.filter.input.AbstractBeanInputFilterStreamFactory;
import org.xwiki.filter.instance.input.InstanceInputEventGenerator;
import org.xwiki.filter.instance.input.InstanceInputProperties;
import org.xwiki.filter.instance.internal.InstanceFilter;
import org.xwiki.filter.instance.internal.InstanceUtils;
import org.xwiki.filter.type.FilterStreamType;

/**
 * A generic xml output filter implementation. This class can be used as a test bench to validate various
 * XMLInputStream wiki parsers.
 * 
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Named(InstanceUtils.ROLEHINT)
@Singleton
public class InstanceInputFilterStreamFactory extends
    AbstractBeanInputFilterStreamFactory<InstanceInputProperties, InstanceFilter>
{
    public static final String ROLEHINT = "xwiki+instance";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    public InstanceInputFilterStreamFactory()
    {
        super(FilterStreamType.XWIKI_INSTANCE);

        setName("XWiki instance input stream");
        setDescription("Generates wiki events from XWiki instance.");
    }

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        List<InstanceInputEventGenerator> eventGenerators;
        try {
            eventGenerators = this.componentManagerProvider.get().getInstanceList(InstanceInputEventGenerator.class);
        } catch (ComponentLookupException e) {
            throw new InitializationException(
                "Failed to get registered instance of InstanceInputEventGenerator components", e);
        }

        FilterStreamDescriptor[] descriptors = new FilterStreamDescriptor[eventGenerators.size() + 1];

        descriptors[0] = this.descriptor;
        for (int i = 0; i < eventGenerators.size(); ++i) {
            descriptors[i + 1] = eventGenerators.get(i).getDescriptor();
        }

        setDescriptor(new CompositeFilterStreamDescriptor(this.descriptor.getName(), this.descriptor.getDescription(),
            descriptors));
    }

    @Override
    public Collection<Class< ? >> getFilterInterfaces() throws FilterException
    {
        List<InstanceInputEventGenerator> eventGenerators;
        try {
            eventGenerators = this.componentManagerProvider.get().getInstanceList(InstanceInputEventGenerator.class);
        } catch (ComponentLookupException e) {
            throw new FilterException("Failed to get registered instance of InstanceInputEventGenerator components", e);
        }

        Set<Class< ? >> filters = new HashSet<Class< ? >>();
        filters.addAll(super.getFilterInterfaces());
        for (InstanceInputEventGenerator generator : eventGenerators) {
            filters.addAll(generator.getFilterInterfaces());
        }

        return filters;
    }
}
