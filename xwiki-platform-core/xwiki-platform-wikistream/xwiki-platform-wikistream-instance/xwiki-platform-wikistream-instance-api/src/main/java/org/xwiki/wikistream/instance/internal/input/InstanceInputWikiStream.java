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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.input.InstanceInputEventGenerator;
import org.xwiki.wikistream.instance.internal.InstanceFilter;
import org.xwiki.wikistream.instance.internal.InstanceModel;
import org.xwiki.wikistream.internal.input.AbstractBeanInputWikiStream;

/**
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Named("xwiki+instance")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class InstanceInputWikiStream extends AbstractBeanInputWikiStream<InstanceInputProperties, InstanceFilter>
{
    @Inject
    private InstanceModel instanceModel;

    @Inject
    private List<InstanceInputEventGenerator> eventGenerators;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.setProperties(this.properties);
        }
    }

    private boolean isWikiInContainers(String wiki)
    {
        return this.properties.getEntities() == null || this.properties.getEntities().matches(new WikiReference(wiki));
    }

    private boolean isSpaceInContainers(String wiki, String space)
    {
        return this.properties.getEntities() == null
            || this.properties.getEntities().matches(new SpaceReference(space, new WikiReference(wiki)));
    }

    @Override
    protected void read(Object filter, InstanceFilter internalFilter) throws WikiStreamException
    {
        FilterEventParameters parameters = FilterEventParameters.EMPTY;

        internalFilter.beginFarm(parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.setFilter(filter);
            generator.beginFarm(parameters);
        }

        for (String wikiName : this.instanceModel.getWikis()) {
            if (isWikiInContainers(wikiName)) {
                writeWiki(wikiName, filter, internalFilter);
            }
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endFarm(parameters);
        }

        internalFilter.endFarm(parameters);
    }

    private void writeWiki(String name, Object filter, InstanceFilter internalFilter) throws WikiStreamException
    {
        FilterEventParameters parameters = FilterEventParameters.EMPTY;

        internalFilter.beginWiki(name, parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.beginWiki(name, parameters);
        }

        for (String spaceName : this.instanceModel.getSpaces(name)) {
            if (isSpaceInContainers(name, spaceName)) {
                writeSpace(spaceName, filter, internalFilter);
            }
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWiki(name, parameters);
        }

        internalFilter.endWiki(name, parameters);
    }

    private void writeSpace(String name, Object filter, InstanceFilter internalFilter) throws WikiStreamException
    {
        FilterEventParameters parameters = FilterEventParameters.EMPTY;

        internalFilter.beginWikiSpace(name, parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.beginWikiSpace(name, parameters);
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWikiSpace(name, parameters);
        }

        internalFilter.endWikiSpace(name, parameters);
    }
}
