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

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.input.AbstractBeanInputFilterStream;
import org.xwiki.filter.instance.input.InstanceInputEventGenerator;
import org.xwiki.filter.instance.input.InstanceInputProperties;
import org.xwiki.filter.instance.internal.InstanceFilter;
import org.xwiki.filter.instance.internal.InstanceModel;
import org.xwiki.filter.instance.internal.InstanceUtils;
import org.xwiki.logging.marker.TranslationMarker;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Named(InstanceUtils.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class InstanceInputFilterStream extends AbstractBeanInputFilterStream<InstanceInputProperties, InstanceFilter>
{
    private static final TranslationMarker LOG_DOCUMENT_SKIPPED = new TranslationMarker(
        "filter.instance.log.document.skipped", WikiDocumentFilter.LOG_DOCUMENT_SKIPPED);

    @Inject
    private InstanceModel instanceModel;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    private Logger logger;

    private List<InstanceInputEventGenerator> eventGenerators;

    @Override
    public void setProperties(InstanceInputProperties properties) throws FilterException
    {
        super.setProperties(properties);

        try {
            this.eventGenerators = this.componentManager.get().getInstanceList(InstanceInputEventGenerator.class);
        } catch (ComponentLookupException e) {
            throw new FilterException(
                "Failed to get regsitered instance of OutputInstanceFilterStreamFactory components", e);
        }

        for (InstanceInputEventGenerator eventGenerator : this.eventGenerators) {
            eventGenerator.setProperties(this.properties);
        }
    }

    private boolean isWikiEnabled(String wiki)
    {
        return this.properties.getEntities() == null || this.properties.getEntities().matches(new WikiReference(wiki));
    }

    private boolean isSpaceEnabled(String wiki, String space)
    {
        return this.properties.getEntities() == null
            || this.properties.getEntities().matches(new SpaceReference(space, new WikiReference(wiki)));
    }

    private boolean isDocumentEnabled(String wiki, String space, String document)
    {
        return this.properties.getEntities() == null
            || this.properties.getEntities().matches(new DocumentReference(wiki, space, document));
    }

    @Override
    public void close() throws IOException
    {
        // Nothing do close
    }

    @Override
    protected void read(Object filter, InstanceFilter proxyFilter) throws FilterException
    {
        FilterEventParameters parameters = new FilterEventParameters();

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.setWikiFarmParameters(parameters);
        }

        proxyFilter.beginFarm(parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.setFilter(filter);
            generator.beginFarm(parameters);
        }

        for (String wikiName : this.instanceModel.getWikis()) {
            if (isWikiEnabled(wikiName)) {
                writeWiki(wikiName, filter, proxyFilter);
            }
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endFarm(parameters);
        }

        proxyFilter.endFarm(parameters);
    }

    private void writeWiki(String wiki, Object filter, InstanceFilter proxyFilter) throws FilterException
    {
        FilterEventParameters parameters = new FilterEventParameters();

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.setWikiParameters(wiki, parameters);
        }

        proxyFilter.beginWiki(wiki, parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.beginWiki(wiki, parameters);
        }

        for (String spaceName : this.instanceModel.getSpaces(wiki)) {
            if (isSpaceEnabled(wiki, spaceName)) {
                writeSpace(wiki, spaceName, filter, proxyFilter);
            }
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWiki(wiki, parameters);
        }

        proxyFilter.endWiki(wiki, parameters);
    }

    private void writeSpace(String wiki, String space, Object filter, InstanceFilter proxyFilter)
        throws FilterException
    {
        FilterEventParameters parameters = new FilterEventParameters();

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.setWikiSpaceParameters(space, parameters);
        }

        proxyFilter.beginWikiSpace(space, parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.beginWikiSpace(space, parameters);
        }

        for (String documentName : this.instanceModel.getDocuments(wiki, space)) {
            if (isDocumentEnabled(wiki, space, documentName)) {
                writeDocument(documentName, filter, proxyFilter);
            } else {
                if (this.properties.isVerbose()) {
                    this.logger.info(LOG_DOCUMENT_SKIPPED, "Skipped document [{}]", new DocumentReference(wiki, space,
                        documentName));
                }
            }
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWikiSpace(space, parameters);
        }

        proxyFilter.endWikiSpace(space, parameters);
    }

    private void writeDocument(String document, Object filter, InstanceFilter proxyFilter) throws FilterException
    {
        FilterEventParameters parameters = new FilterEventParameters();

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.setWikiDocumentParameters(document, parameters);
        }

        proxyFilter.beginWikiDocument(document, parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.beginWikiDocument(document, parameters);
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWikiDocument(document, parameters);
        }

        proxyFilter.endWikiDocument(document, parameters);
    }
}
