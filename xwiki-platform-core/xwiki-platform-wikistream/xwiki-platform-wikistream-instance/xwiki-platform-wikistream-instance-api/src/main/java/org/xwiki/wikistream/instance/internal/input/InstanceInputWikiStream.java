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
import org.xwiki.model.reference.DocumentReference;
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

    private boolean isWikiEnaled(String wiki)
    {
        return this.properties.getEntities() == null || this.properties.getEntities().matches(new WikiReference(wiki));
    }

    private boolean isSpaceEnabled(String wiki, String space)
    {
        return this.properties.getEntities() == null
            || this.properties.getEntities().matches(new SpaceReference(space, new WikiReference(wiki)));
    }

    private boolean isDocumentEnaled(String wiki, String space, String document)
    {
        return this.properties.getEntities() == null
            || this.properties.getEntities().matches(new DocumentReference(wiki, space, document));
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
            if (isWikiEnaled(wikiName)) {
                writeWiki(wikiName, filter, internalFilter);
            }
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endFarm(parameters);
        }

        internalFilter.endFarm(parameters);
    }

    private void writeWiki(String wiki, Object filter, InstanceFilter internalFilter) throws WikiStreamException
    {
        FilterEventParameters parameters = FilterEventParameters.EMPTY;

        internalFilter.beginWiki(wiki, parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.beginWiki(wiki, parameters);
        }

        for (String spaceName : this.instanceModel.getSpaces(wiki)) {
            if (isSpaceEnabled(wiki, spaceName)) {
                writeSpace(wiki, spaceName, filter, internalFilter);
            }
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWiki(wiki, parameters);
        }

        internalFilter.endWiki(wiki, parameters);
    }

    private void writeSpace(String wiki, String space, Object filter, InstanceFilter internalFilter)
        throws WikiStreamException
    {
        FilterEventParameters parameters = FilterEventParameters.EMPTY;

        internalFilter.beginWikiSpace(space, parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.beginWikiSpace(space, parameters);
        }

        for (String documentName : this.instanceModel.getDocuments(wiki, space)) {
            if (isDocumentEnaled(wiki, space, documentName)) {
                writeDocument(documentName, filter, internalFilter);
            }
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWikiSpace(space, parameters);
        }

        internalFilter.endWikiSpace(space, parameters);
    }

    private void writeDocument(String document, Object filter, InstanceFilter internalFilter)
        throws WikiStreamException
    {
        FilterEventParameters parameters = FilterEventParameters.EMPTY;

        internalFilter.beginWikiDocument(document, parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.beginWikiDocument(document, parameters);
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWikiDocument(document, parameters);
        }

        internalFilter.endWikiSpace(document, parameters);
    }
}
