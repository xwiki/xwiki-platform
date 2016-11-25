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
package com.xpn.xwiki.internal.filter;

import java.io.IOException;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.input.BeanInputFilterStream;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.instance.input.BeanEntityEventGenerator;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.instance.input.EntityEventGenerator;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.filter.output.BeanOutputFilterStream;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.DefaultWriterOutputTarget;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.output.OutputTarget;
import org.xwiki.filter.output.WriterOutputTarget;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.internal.XARFilter;
import org.xwiki.filter.xar.internal.XARFilterUtils;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.filter.output.EntityOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.XWikiDocumentOutputFilterStream;

/**
 * Various XWikiDocument related helpers.
 * 
 * @version $Id$
 * @since 9.0RC1
 */
@Component(roles = XWikiDocumentFilterUtils.class)
@Singleton
public class XWikiDocumentFilterUtils
{
    @Inject
    private Provider<EntityOutputFilterStream<XWikiDocument>> streamProvider;

    @Inject
    @Named(XARFilterUtils.ROLEHINT_CURRENT)
    private InputFilterStreamFactory xarInputFilterStreamFactory;

    @Inject
    @Named(XARFilterUtils.ROLEHINT_CURRENT)
    private OutputFilterStreamFactory xarOutputFilterStreamFactory;

    @Inject
    private ComponentManager componentManager;

    // Import

    /**
     * @param document the document to write
     * @param source the stream to read
     * @param history import or skip the history in the XML
     * @throws FilterException when failing to import
     * @throws IOException when failing to import
     */
    public void importDocument(XWikiDocument document, InputSource source, boolean history)
        throws FilterException, IOException
    {
        // Output
        DocumentInstanceOutputProperties documentProperties = new DocumentInstanceOutputProperties();
        documentProperties.setVersionPreserved(history);

        // Input
        XARInputProperties xarProperties = new XARInputProperties();
        xarProperties.setWithHistory(history);

        importDocument(document, source, xarProperties, documentProperties);
    }

    /**
     * @param source the stream to read
     * @param xarProperties the configuration of the input filter
     * @param documentProperties the configuration of the output filter
     * @return the imported document, same as {@code document} if not null
     * @throws FilterException when failing to import
     * @throws IOException when failing to import
     */
    public XWikiDocument importDocument(InputSource source, XARInputProperties xarProperties,
        DocumentInstanceOutputProperties documentProperties) throws FilterException, IOException
    {
        return importDocument(null, source, xarProperties, documentProperties);
    }

    /**
     * @param document the document to write to or null to create a new XWikiDocument instance
     * @param source the stream to read
     * @param xarProperties the configuration of the input filter
     * @param documentProperties the configuration of the output filter
     * @return the imported document, same as {@code document} if not null
     * @throws FilterException when failing to import
     * @throws IOException when failing to import
     */
    public XWikiDocument importDocument(XWikiDocument document, InputSource source, XARInputProperties xarProperties,
        DocumentInstanceOutputProperties documentProperties) throws FilterException, IOException
    {
        // Output
        XWikiDocumentOutputFilterStream documentFilter = (XWikiDocumentOutputFilterStream) this.streamProvider.get();
        documentFilter.setProperties(documentProperties);
        documentFilter.setEntity(document);
        documentFilter.disableRenderingEvents();

        // Input
        xarProperties.setForceDocument(true);
        xarProperties.setSource(source);
        BeanInputFilterStream<XARInputProperties> xarReader =
            ((BeanInputFilterStreamFactory<XARInputProperties>) this.xarInputFilterStreamFactory)
                .createInputFilterStream(xarProperties);

        // Convert
        xarReader.read(documentFilter.getFilter());

        xarReader.close();

        return documentFilter.getEntity();
    }

    // Export

    /**
     * @param entity the entity to read
     * @return the XML as a String
     * @throws ComponentLookupException failed to find an event generator for passed entity
     * @throws FilterException when failing to generate export the passed entity
     */
    public String exportEntity(Object entity) throws ComponentLookupException, FilterException
    {
        return exportEntity(entity, new XAROutputProperties(), new DocumentInstanceInputProperties());
    }

    /**
     * @param entity the entity to read
     * @param target the target where to write the result
     * @throws ComponentLookupException failed to find an event generator for passed entity
     * @throws FilterException when failing to generate export the passed entity
     */
    public void exportEntity(Object entity, OutputTarget target) throws ComponentLookupException, FilterException
    {
        exportEntity(entity, target, new XAROutputProperties(), new DocumentInstanceInputProperties());
    }

    /**
     * @param entity the entity to read
     * @param xarProperties the configuration of the output filter
     * @param documentProperties the configuration of the input filter
     * @return the XML as a String
     * @throws ComponentLookupException failed to find an event generator for passed entity
     * @throws FilterException when failing to generate export the passed entity
     */
    public String exportEntity(Object entity, XAROutputProperties xarProperties,
        DocumentInstanceInputProperties documentProperties) throws ComponentLookupException, FilterException
    {
        WriterOutputTarget target = new DefaultWriterOutputTarget(new StringWriter());

        exportEntity(entity, target, xarProperties, documentProperties);

        return target.getWriter().toString();
    }

    /**
     * @param entity the entity to read
     * @param target the target where to write the result
     * @param xarProperties the configuration of the output filter
     * @param documentProperties the configuration of the input filter
     * @throws ComponentLookupException failed to find an event generator for passed entity
     * @throws FilterException when failing to generate export the passed entity
     */
    public void exportEntity(Object entity, OutputTarget target, XAROutputProperties xarProperties,
        DocumentInstanceInputProperties documentProperties) throws ComponentLookupException, FilterException
    {
        // Input
        documentProperties.setVerbose(false);

        // Output
        xarProperties.setForceDocument(true);
        if (target != null) {
            xarProperties.setTarget(target);
        }
        xarProperties.setVerbose(false);
        BeanOutputFilterStream<XAROutputProperties> xarFilter =
            ((BeanOutputFilterStreamFactory<XAROutputProperties>) this.xarOutputFilterStreamFactory)
                .createOutputFilterStream(xarProperties);
        XARFilter filter = (XARFilter) xarFilter.getFilter();

        BeanEntityEventGenerator<Object, DocumentInstanceInputProperties> generator = this.componentManager
            .getInstance(new DefaultParameterizedType(null, EntityEventGenerator.class, entity.getClass()));

        // Spaces and document events
        FilterEventParameters documentParameters = null;
        DocumentReference documentReference = null;
        if (entity instanceof XWikiDocument) {
            documentReference = ((XWikiDocument) entity).getDocumentReference();
            for (SpaceReference spaceReference : documentReference.getSpaceReferences()) {
                filter.beginWikiSpace(spaceReference.getName(), FilterEventParameters.EMPTY);
            }

            documentParameters = new FilterEventParameters();
            documentParameters.put(WikiDocumentFilter.PARAMETER_LOCALE, ((XWikiDocument) entity).getDefaultLocale());
            filter.beginWikiDocument(documentReference.getName(), documentParameters);
        }

        // Document Locale events
        generator.write(entity, xarFilter, documentProperties);

        // Document and spaces events
        if (documentParameters != null) {
            filter.endWikiDocument(documentReference.getName(), documentParameters);

            documentReference = ((XWikiDocument) entity).getDocumentReference();
            for (EntityReference reference =
                documentReference.getParent(); reference instanceof SpaceReference; reference = reference.getParent()) {
                filter.beginWikiSpace(reference.getName(), FilterEventParameters.EMPTY);
            }
        }
    }
}
