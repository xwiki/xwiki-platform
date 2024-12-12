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
package org.xwiki.internal.filter;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.input.BeanInputFilterStream;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.instance.internal.InstanceUtils;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.filter.instance.output.InstanceOutputProperties;
import org.xwiki.filter.output.BeanOutputFilterStream;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.internal.XARFilterUtils;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.LoggerManager;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.logging.event.LoggerListener;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.event.XARImportedEvent;
import com.xpn.xwiki.internal.event.XARImportingEvent;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.Package;
import com.xpn.xwiki.web.Utils;

/**
 * A helper to import a XAR package.
 * 
 * @version $Id$
 * @since 16.10.2
 * @since 16.4.6
 * @since 15.10.16
 */
@Component(roles = Importer.class)
@Singleton
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class Importer
{
    @Inject
    @Named(XARFilterUtils.ROLEHINT_CURRENT)
    private InputFilterStreamFactory xarFilterStreamFactory;

    @Inject
    @Named(InstanceUtils.ROLEHINT)
    private OutputFilterStreamFactory instanceFilterStreamFactory;

    @Inject
    private ObservationManager observation;

    @Inject
    private Logger logger;

    /**
     * @param source the XAR package to import
     * @param entities the entities to import from the package or null for everything
     * @param historyStrategy the history strategy to apply
     * @param backup true if the package should be handled as a backup
     * @param context the XWiki context
     * @throws IOException when failing to import the package
     * @throws FilterException when failing to import the package
     */
    public void importXAR(InputSource source, EntityReferenceSet entities, String historyStrategy, boolean backup,
        XWikiContext context) throws IOException, FilterException
    {
        XARInputProperties xarProperties = new XARInputProperties();
        xarProperties.setSource(source);

        DocumentInstanceOutputProperties instanceProperties = new DocumentInstanceOutputProperties();
        instanceProperties.setSaveComment("Imported from XAR");

        if (entities != null) {
            xarProperties.setEntities(entities);
        }

        // Set the appropriate strategy to handle versions
        setHistoryStrategy(historyStrategy, xarProperties, instanceProperties);

        // Set the backup pack option
        instanceProperties.setAuthorPreserved(backup);

        // Setup log
        xarProperties.setVerbose(true);
        instanceProperties.setVerbose(true);
        instanceProperties.setStoppedWhenSaveFail(false);
        LoggerManager loggerManager = Utils.getComponent(LoggerManager.class);
        LogQueue importLogger = new LogQueue();
        if (loggerManager != null) {
            // Isolate log
            loggerManager.pushLogListener(new LoggerListener(UUID.randomUUID().toString(), importLogger));
        }

        // Create the streams
        BeanInputFilterStream<XARInputProperties> xarFilterStream = createXARFilterStream(xarProperties);
        BeanOutputFilterStream<InstanceOutputProperties> instanceFilterStream =
            createInstanceFilterStream(instanceProperties);

        // Notify listeners about import beginning
        this.observation.notify(new XARImportingEvent(), null, context);

        try {
            xarFilterStream.read(instanceFilterStream.getFilter());

            xarFilterStream.close();
            instanceFilterStream.close();
        } finally {
            if (loggerManager != null) {
                // Stop isolating log
                loggerManager.popLogListener();
            }

            // Print the import log
            if (this.logger.isDebugEnabled()) {
                importLogger.log(this.logger);
            } else {
                // TODO: remove when the UI show the log properly
                for (LogEvent logEvent : importLogger.getLogsFrom(LogLevel.ERROR)) {
                    logEvent.log(this.logger);
                }
            }

            // Make sure to free any resource use by the input source in case the input filter does not do it
            source.close();

            // Notify listeners about import end
            this.observation.notify(new XARImportedEvent(), null, context);
        }

        // Generate import report
        generateReport(importLogger, context);
    }

    private BeanInputFilterStream<XARInputProperties> createXARFilterStream(XARInputProperties xarProperties)
        throws FilterException
    {
        BeanInputFilterStreamFactory<XARInputProperties> xarInputfactory =
            (BeanInputFilterStreamFactory<XARInputProperties>) this.xarFilterStreamFactory;
        return xarInputfactory.createInputFilterStream(xarProperties);
    }

    private BeanOutputFilterStream<InstanceOutputProperties> createInstanceFilterStream(
        DocumentInstanceOutputProperties instanceProperties) throws FilterException
    {
        BeanOutputFilterStreamFactory<InstanceOutputProperties> instanceOutputFactory =
            (BeanOutputFilterStreamFactory<InstanceOutputProperties>) this.instanceFilterStreamFactory;
        return instanceOutputFactory.createOutputFilterStream(instanceProperties);
    }

    private void setHistoryStrategy(String historyStrategy, XARInputProperties xarProperties,
        DocumentInstanceOutputProperties instanceProperties)
    {
        if (StringUtils.equals(historyStrategy, "reset")) {
            instanceProperties.setPreviousDeleted(true);
            instanceProperties.setVersionPreserved(false);
            xarProperties.setWithHistory(false);
        } else if (StringUtils.equals(historyStrategy, "replace")) {
            instanceProperties.setPreviousDeleted(true);
            instanceProperties.setVersionPreserved(true);
            xarProperties.setWithHistory(true);
        } else {
            instanceProperties.setPreviousDeleted(false);
            instanceProperties.setVersionPreserved(false);
            xarProperties.setWithHistory(false);
        }
    }

    private void generateReport(LogQueue importLogger, XWikiContext context)
    {
        // Emulate old packager report (for retro compatibility)
        Package oldImporter = new Package();
        context.put("install_status",
            importLogger.containLogsFrom(LogLevel.ERROR) ? DocumentInfo.INSTALL_ERROR : DocumentInfo.INSTALL_OK);
        EntityReferenceSerializer<String> serializer =
            Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        for (LogEvent log : importLogger) {
            Marker marker = log.getMarker();
            if (marker != null) {
                if (marker.contains(WikiDocumentFilter.LOG_DOCUMENT_CREATED.getName())
                    || marker.contains(WikiDocumentFilter.LOG_DOCUMENT_UPDATED.getName())) {
                    oldImporter.getInstalled(context)
                        .add(serializer.serialize((EntityReference) log.getArgumentArray()[0]));
                } else if (marker.contains(WikiDocumentFilter.LOG_DOCUMENT_SKIPPED.getName())) {
                    oldImporter.getSkipped(context)
                        .add(serializer.serialize((EntityReference) log.getArgumentArray()[0]));
                } else if (marker.contains(WikiDocumentFilter.LOG_DOCUMENT_ERROR.getName())) {
                    Object entity = log.getArgumentArray()[0];
                    if (entity != null) {
                        oldImporter.getErrors(context).add(entity instanceof EntityReference
                            ? serializer.serialize((EntityReference) log.getArgumentArray()[0]) : entity.toString());
                    }
                }
            }
        }
    }
}
