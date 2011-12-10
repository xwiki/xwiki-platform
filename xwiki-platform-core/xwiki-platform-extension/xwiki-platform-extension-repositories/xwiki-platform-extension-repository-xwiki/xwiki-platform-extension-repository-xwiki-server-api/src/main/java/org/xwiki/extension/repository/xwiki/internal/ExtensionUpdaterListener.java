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
package org.xwiki.extension.repository.xwiki.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.internal.VersionManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component("ExtensionUpdaterListener")
@Singleton
public class ExtensionUpdaterListener implements EventListener
{
    /**
     * Listened events.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new DocumentCreatedEvent(),
        new DocumentUpdatedEvent());

    /**
     * Used to find last version.
     */
    @Inject
    private VersionManager versionManager;

    /**
     * Get the reference of the class in the current wiki.
     */
    @Inject
    @Named("default/reference")
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to validate download reference.
     */
    @Inject
    @Named("link")
    private ResourceReferenceParser resourceReferenceParser;

    /**
     * Used to validate download reference.
     */
    @Inject
    private AttachmentReferenceResolver<String> attachmentResolver;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return "ExtensionUpdaterListener";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        // TODO: improve this to do only one save

        BaseObject extensionObject = document.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);

        if (extensionObject != null) {
            XWikiDocument modifiedDocument = updateLastVersion(document, extensionObject, context);
            validateExtension(modifiedDocument, extensionObject, context);
        }
    }

    private void validateExtension(XWikiDocument document, BaseObject extensionObject, XWikiContext context)
    {
        String extensionId = extensionObject.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_ID);
        boolean valid = !StringUtils.isBlank(extensionId);
        if (valid) {
            int nbVersions = 0;
            List<BaseObject> extensionVersions =
                document.getXObjects(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE);
            if (extensionVersions != null) {
                for (BaseObject extensionVersionObject : extensionVersions) {
                    if (extensionVersionObject != null) {
                        // Has a version
                        String extensionVersion =
                            extensionVersionObject.getStringValue(XWikiRepositoryModel.PROP_VERSION_VERSION);
                        if (StringUtils.isBlank(extensionVersion)) {
                            valid = false;
                            break;
                        }

                        // The download reference seems ok
                        String download =
                            extensionVersionObject.getStringValue(XWikiRepositoryModel.PROP_VERSION_DOWNLOAD);

                        if (StringUtils.isNotEmpty(download)) {
                            ResourceReference resourceReference = this.resourceReferenceParser.parse(download);

                            if (ResourceType.ATTACHMENT.equals(resourceReference.getType())) {
                                AttachmentReference attachmentReference =
                                    this.attachmentResolver.resolve(resourceReference.getReference(),
                                        document.getDocumentReference());

                                XWikiDocument attachmentDocument;
                                try {
                                    attachmentDocument =
                                        context.getWiki().getDocument(attachmentReference.getDocumentReference(),
                                            context);

                                    valid = attachmentDocument.getAttachment(attachmentReference.getName()) != null;
                                } catch (XWikiException e) {
                                    valid = false;
                                }
                            } else if (ResourceType.URL.equals(resourceReference.getType())) {
                                valid = true;
                            } else {
                                valid = false;
                            }
                        } else {
                            valid =
                                document.getAttachment(extensionId + "-" + extensionVersion + "."
                                    + extensionObject.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_TYPE)) != null;
                        }

                        ++nbVersions;
                    }

                    if (!valid) {
                        break;
                    }
                }
            }

            valid &= nbVersions > 0;
        }

        int currentValue = extensionObject.getIntValue(XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION, 0);

        if ((currentValue == 1) != valid) {
            try {
                // FIXME: We can't save directly the provided document coming from the event
                document = context.getWiki().getDocument(document, context);
                extensionObject = document.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);

                extensionObject.setIntValue(XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION, valid ? 1 : 0);

                context.getWiki().saveDocument(document, "Validated extension", true, context);
            } catch (XWikiException e) {
                this.logger.error("Failed to validate extension [{}]", document, e);
            }
        }
    }

    private XWikiDocument updateLastVersion(XWikiDocument document, BaseObject extensionObject, XWikiContext context)
    {
        String lastVersion = findLastVersion(document);

        if (lastVersion != null
            && !StringUtils.equals(lastVersion,
                extensionObject.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION))) {
            try {
                // FIXME: We can't save directly the provided document coming from the event
                document = context.getWiki().getDocument(document, context);
                extensionObject = document.getXObject(extensionObject.getReference());

                extensionObject.setStringValue(XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION, lastVersion);

                context.getWiki().saveDocument(document, "Update extension last version", context);
            } catch (XWikiException e) {
                this.logger.error("Failed to update extension [{}] last version", document, e);
            }
        }

        return document;
    }

    private DocumentReference getClassReference(XWikiDocument document, EntityReference localReference)
    {
        return this.referenceResolver.resolve(localReference, document.getDocumentReference().getWikiReference());
    }

    /**
     * Compare all version located in a document to find the last one.
     * 
     * @param document the extension document
     * @return the last version
     */
    private String findLastVersion(XWikiDocument document)
    {
        DocumentReference versionClassReference =
            getClassReference(document, XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE);

        List<BaseObject> versionObjects = document.getXObjects(versionClassReference);

        String lastVersion = null;
        if (versionObjects != null) {
            for (BaseObject versionObject : versionObjects) {
                if (versionObject != null) {
                    String version = versionObject.getStringValue(XWikiRepositoryModel.PROP_VERSION_VERSION);
                    if (version != null) {
                        if (lastVersion == null || this.versionManager.compareVersions(version, lastVersion) > 0) {
                            lastVersion = version;
                        }
                    }
                }
            }
        }

        return lastVersion;
    }
}
