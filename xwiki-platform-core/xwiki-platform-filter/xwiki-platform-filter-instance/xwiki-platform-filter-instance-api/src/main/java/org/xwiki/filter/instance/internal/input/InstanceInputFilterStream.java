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
import org.xwiki.model.reference.EntityReferenceTreeNode;
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

    private boolean isWikiEnabled(WikiReference wikiReference)
    {
        return this.properties.getEntities() == null || this.properties.getEntities().matches(wikiReference);
    }

    private boolean isSpaceEnabled(SpaceReference spaceReference)
    {
        return this.properties.getEntities() == null || this.properties.getEntities().matches(spaceReference);
    }

    private boolean isDocumentEnabled(DocumentReference documentReference)
    {
        return this.properties.getEntities() == null || this.properties.getEntities().matches(documentReference);
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

        proxyFilter.beginWikiFarm(parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.setFilter(filter);
            generator.beginWikiFarm(parameters);
        }

        for (WikiReference wikiReference : this.instanceModel.getWikiReferences()) {
            if (isWikiEnabled(wikiReference)) {
                writeWiki(wikiReference, filter, proxyFilter);
            }
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWikiFarm(parameters);
        }

        proxyFilter.endWikiFarm(parameters);
    }

    private void writeWiki(WikiReference wikiReference, Object filter, InstanceFilter proxyFilter)
        throws FilterException
    {
        FilterEventParameters parameters = new FilterEventParameters();

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.setWikiParameters(wikiReference.getName(), parameters);
        }

        proxyFilter.beginWiki(wikiReference.getName(), parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.beginWiki(wikiReference.getName(), parameters);
        }

        // TODO: improve with a new space related API to get space level by space level instead of all of them at the
        // same time
        EntityReferenceTreeNode spaces = this.instanceModel.getSpaceReferences(wikiReference);
        for (EntityReferenceTreeNode node : spaces.getChildren()) {
            if (isSpaceEnabled((SpaceReference) node.getReference())) {
                writeSpace(node, filter, proxyFilter);
            }
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWiki(wikiReference.getName(), parameters);
        }

        proxyFilter.endWiki(wikiReference.getName(), parameters);
    }

    private void writeSpace(EntityReferenceTreeNode node, Object filter, InstanceFilter proxyFilter)
        throws FilterException
    {
        SpaceReference spaceReference = (SpaceReference) node.getReference();

        FilterEventParameters parameters = new FilterEventParameters();

        // Get begin/end space parameters
        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.setWikiSpaceParameters(spaceReference.getName(), parameters);
        }

        // Begin space
        proxyFilter.beginWikiSpace(spaceReference.getName(), parameters);

        // Extend begin space
        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.beginWikiSpace(spaceReference.getName(), parameters);
        }

        // Write documents
        for (DocumentReference documentReference : this.instanceModel.getDocumentReferences(spaceReference)) {
            if (isDocumentEnabled(documentReference)) {
                writeDocument(documentReference, filter, proxyFilter);
            } else {
                if (this.properties.isVerbose()) {
                    this.logger.info(LOG_DOCUMENT_SKIPPED, "Skipped document [{}]", documentReference);
                }
            }
        }

        // Write nested spaces
        for (EntityReferenceTreeNode child : node.getChildren()) {
            if (isSpaceEnabled((SpaceReference) child.getReference())) {
                writeSpace(child, filter, proxyFilter);
            }
        }

        // Extend end space
        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWikiSpace(spaceReference.getName(), parameters);
        }

        // End space
        proxyFilter.endWikiSpace(spaceReference.getName(), parameters);
    }

    private void writeDocument(DocumentReference documentReference, Object filter, InstanceFilter proxyFilter)
        throws FilterException
    {
        FilterEventParameters parameters = new FilterEventParameters();

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.setWikiDocumentParameters(documentReference.getName(), parameters);
        }

        proxyFilter.beginWikiDocument(documentReference.getName(), parameters);

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.beginWikiDocument(documentReference.getName(), parameters);
        }

        for (InstanceInputEventGenerator generator : this.eventGenerators) {
            generator.endWikiDocument(documentReference.getName(), parameters);
        }

        proxyFilter.endWikiDocument(documentReference.getName(), parameters);
    }
}
