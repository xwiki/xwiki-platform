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

import javax.inject.Inject;
import javax.inject.Named;
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
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.output.OutputTarget;
import org.xwiki.filter.output.StringWriterOutputTarget;
import org.xwiki.filter.output.WriterOutputTarget;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.input.XARInputProperties.SourceType;
import org.xwiki.filter.xar.internal.XARFilter;
import org.xwiki.filter.xar.internal.XARFilterUtils;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.filter.output.EntityOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.XWikiDocumentOutputFilterStream;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

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
    @Named(XARFilterUtils.ROLEHINT_CURRENT)
    private InputFilterStreamFactory xarInputFilterStreamFactory;

    @Inject
    @Named(XARFilterUtils.ROLEHINT_CURRENT)
    private OutputFilterStreamFactory xarOutputFilterStreamFactory;

    @Inject
    private ComponentManager componentManager;

    // Import

    private <T> Class<T> getClass(Object entity)
    {
        Class<T> entityClass;

        if (entity instanceof Class) {
            entityClass = (Class<T>) entity;
        } else if (entity instanceof XWikiDocument) {
            entityClass = (Class<T>) XWikiDocument.class;
        } else if (entity instanceof XWikiAttachment) {
            entityClass = (Class<T>) XWikiAttachment.class;
        } else if (entity instanceof BaseClass) {
            entityClass = (Class<T>) BaseClass.class;
        } else if (entity instanceof BaseObject) {
            entityClass = (Class<T>) BaseObject.class;
        } else if (entity instanceof BaseProperty) {
            entityClass = (Class<T>) BaseProperty.class;
        } else if (entity instanceof PropertyClass) {
            entityClass = (Class<T>) PropertyClass.class;
        } else {
            entityClass = (Class<T>) entity.getClass();
        }

        return entityClass;
    }

    private SourceType getSourceType(Class<?> entityClass) throws FilterException
    {
        SourceType sourceType;

        if (entityClass == XWikiDocument.class) {
            sourceType = SourceType.DOCUMENT;
        } else if (entityClass == XWikiAttachment.class) {
            sourceType = SourceType.ATTACHMENT;
        } else if (entityClass == BaseClass.class) {
            sourceType = SourceType.CLASS;
        } else if (entityClass == BaseObject.class) {
            sourceType = SourceType.OBJECT;
        } else if (entityClass == BaseProperty.class) {
            sourceType = SourceType.OBJECTPROPERTY;
        } else if (entityClass == PropertyClass.class) {
            sourceType = SourceType.CLASSPROPERTY;
        } else {
            throw new FilterException("Unsupported type [" + entityClass + "]");
        }

        return sourceType;
    }

    /**
     * @param entity the entity to write to or its class to create a new one
     * @param source the stream to read
     * @return the imported entity, same as {@code entity} if not null
     * @throws FilterException when failing to import
     * @throws IOException when failing to import
     * @throws ComponentLookupException when failing to find a EntityOutputFilterStream corresponding to passed class
     */
    public <T> T importEntity(Object entity, InputSource source)
        throws FilterException, IOException, ComponentLookupException
    {
        // Output
        DocumentInstanceOutputProperties documentProperties = new DocumentInstanceOutputProperties();

        // Input
        XARInputProperties xarProperties = new XARInputProperties();

        return importEntity(getClass(entity), entity instanceof Class ? null : (T) entity, source, xarProperties,
            documentProperties);
    }

    /**
     * @param entityClass to class used to find the {@link EntityOutputFilterStream} component
     * @param entity the entity to write to or null to create a new entity of the passed class
     * @param source the stream to read
     * @param xarProperties the configuration of the input filter
     * @param documentProperties the configuration of the output filter
     * @return the imported entity, same as {@code entity} if not null
     * @throws FilterException when failing to import
     * @throws IOException when failing to import
     * @throws ComponentLookupException when failing to find a EntityOutputFilterStream corresponding to passed class
     */
    public <T> T importEntity(Class<T> entityClass, T entity, InputSource source, XARInputProperties xarProperties,
        DocumentInstanceOutputProperties documentProperties)
        throws FilterException, IOException, ComponentLookupException
    {
        // Output
        EntityOutputFilterStream<T> filterStream = this.componentManager
            .getInstance(new DefaultParameterizedType(null, EntityOutputFilterStream.class, entityClass));
        filterStream.setProperties(documentProperties);
        filterStream.setEntity(entity);
        if (filterStream instanceof XWikiDocumentOutputFilterStream) {
            ((XWikiDocumentOutputFilterStream) filterStream).disableRenderingEvents();
        }

        // Input
        xarProperties.setSourceType(getSourceType(entityClass));
        xarProperties.setSource(source);
        BeanInputFilterStream<XARInputProperties> xarReader =
            ((BeanInputFilterStreamFactory<XARInputProperties>) this.xarInputFilterStreamFactory)
                .createInputFilterStream(xarProperties);

        // Convert
        xarReader.read(filterStream.getFilter());

        xarReader.close();

        return filterStream.getEntity();
    }

    /**
     * @param source the stream to read
     * @param xarProperties the configuration of the input filter
     * @param documentProperties the configuration of the output filter
     * @return the imported document
     * @throws FilterException when failing to import
     * @throws IOException when failing to import
     * @throws ComponentLookupException when failing to find a EntityOutputFilterStream corresponding to passed class
     */
    public XWikiDocument importDocument(InputSource source, XARInputProperties xarProperties,
        DocumentInstanceOutputProperties documentProperties)
        throws FilterException, IOException, ComponentLookupException
    {
        return importEntity(XWikiDocument.class, null, source, xarProperties, documentProperties);
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
        WriterOutputTarget target = new StringWriterOutputTarget();

        exportEntity(entity, target, xarProperties, documentProperties);

        return target.toString();
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
