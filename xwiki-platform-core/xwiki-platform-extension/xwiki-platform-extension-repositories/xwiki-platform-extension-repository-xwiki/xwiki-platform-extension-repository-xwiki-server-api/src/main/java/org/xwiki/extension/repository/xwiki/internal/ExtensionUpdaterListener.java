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

        BaseObject extensionObject = document.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);

        if (extensionObject != null) {
            try {
                validateExtension(document, extensionObject, context);
            } catch (XWikiException e) {
                this.logger.error("Failed to validate extension in document [{}]", document.getDocumentReference(), e);
            }
        }
    }

    private void validateExtension(XWikiDocument document, BaseObject extensionObject, XWikiContext context)
        throws XWikiException
    {
        boolean needSave = false;

        XWikiDocument documentToSave = null;
        BaseObject extensionObjectToSave = null;

        // Update last version field
        String lastVersion = findLastVersion(document);

        if (lastVersion != null
            && !StringUtils.equals(lastVersion,
                extensionObject.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION))) {
            // FIXME: We can't save directly the provided document coming from the event
            documentToSave = context.getWiki().getDocument(document, context);
            extensionObjectToSave = documentToSave.getXObject(extensionObject.getReference());

            extensionObjectToSave.set(XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION, lastVersion, context);

            needSave = true;
        }

        // Update valid extension field

        boolean valid = isValid(document, extensionObject, context);

        int currentValue = extensionObject.getIntValue(XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION, 0);

        if ((currentValue == 1) != valid) {
            if (documentToSave == null) {
                // FIXME: We can't save directly the provided document coming from the event
                documentToSave = context.getWiki().getDocument(document, context);
                extensionObjectToSave = documentToSave.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
            }

            extensionObjectToSave.setIntValue(XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION, valid ? 1 : 0);

            needSave = true;
        }

        // Make sure all searched fields are set in valid extensions (otherwise they won't appear in the search result).
        // Would probably be cleaner and safer to do a left join instead but can't find a standard way to do it trough
        // XWQL or HSQL.

        if (valid) {
            if (extensionObject.safeget(XWikiRepositoryModel.PROP_EXTENSION_SUMMARY) == null) {
                if (extensionObjectToSave == null) {
                    documentToSave = context.getWiki().getDocument(document, context);
                    extensionObjectToSave = documentToSave.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
                }

                extensionObjectToSave.set(XWikiRepositoryModel.PROP_EXTENSION_SUMMARY, "", context);
                needSave = true;
            }
            if (extensionObject.safeget(XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION) == null) {
                if (extensionObjectToSave == null) {
                    documentToSave = context.getWiki().getDocument(document, context);
                    extensionObjectToSave = documentToSave.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
                }

                extensionObjectToSave.set(XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION, "", context);
                needSave = true;
            }
            if (extensionObject.safeget(XWikiRepositoryModel.PROP_EXTENSION_WEBSITE) == null) {
                if (extensionObjectToSave == null) {
                    documentToSave = context.getWiki().getDocument(document, context);
                    extensionObjectToSave = documentToSave.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
                }

                extensionObjectToSave.set(XWikiRepositoryModel.PROP_EXTENSION_WEBSITE, "", context);
                needSave = true;
            }
            if (extensionObject.safeget(XWikiRepositoryModel.PROP_EXTENSION_AUTHORS) == null) {
                if (extensionObjectToSave == null) {
                    documentToSave = context.getWiki().getDocument(document, context);
                    extensionObjectToSave = documentToSave.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
                }

                extensionObjectToSave.set(XWikiRepositoryModel.PROP_EXTENSION_AUTHORS, "", context);
                needSave = true;
            }
            if (extensionObject.safeget(XWikiRepositoryModel.PROP_EXTENSION_FEATURES) == null) {
                if (extensionObjectToSave == null) {
                    documentToSave = context.getWiki().getDocument(document, context);
                    extensionObjectToSave = documentToSave.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
                }

                extensionObjectToSave.set(XWikiRepositoryModel.PROP_EXTENSION_FEATURES, "", context);
                needSave = true;
            }
            if (extensionObject.safeget(XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME) == null) {
                if (extensionObjectToSave == null) {
                    documentToSave = context.getWiki().getDocument(document, context);
                    extensionObjectToSave = documentToSave.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
                }

                extensionObjectToSave.set(XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME, "", context);
                needSave = true;
            }
        }

        // Save document

        if (needSave) {
            context.getWiki().saveDocument(documentToSave, "Validated extension", true, context);
        }
    }

    private DocumentReference getClassReference(XWikiDocument document, EntityReference localReference)
    {
        return this.referenceResolver.resolve(localReference, document.getDocumentReference().getWikiReference());
    }

    /**
     * @param document the extension document
     * @param extensionObject the extension object
     * @param context the XWiki context
     * @return true if the extension is valid from Extension Manager point of view
     */
    private boolean isValid(XWikiDocument document, BaseObject extensionObject, XWikiContext context)
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

        return valid;
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
