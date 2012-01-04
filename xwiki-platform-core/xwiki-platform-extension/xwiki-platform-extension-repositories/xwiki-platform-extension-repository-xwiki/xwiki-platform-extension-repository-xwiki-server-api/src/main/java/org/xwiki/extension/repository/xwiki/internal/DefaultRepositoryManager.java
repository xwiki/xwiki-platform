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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.context.Execution;
import org.xwiki.extension.repository.xwiki.internal.resources.AbstractExtensionRESTResource;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class DefaultRepositoryManager implements RepositoryManager
{
    /**
     * Get the reference of the class in the current wiki.
     */
    @Inject
    @Named("default/reference")
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    /**
     * Used to validate download reference.
     */
    @Inject
    @Named("link")
    private ResourceReferenceParser resourceReferenceParser;

    @Inject
    EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Used to validate download reference.
     */
    @Inject
    private AttachmentReferenceResolver<String> attachmentResolver;

    @Inject
    private QueryManager queryManager;

    @Inject
    private Execution execution;

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }

    @Override
    public XWikiDocument getExtensionDocumentById(String extensionId) throws QueryException, XWikiException
    {
        Query query =
            this.queryManager.createQuery("from doc.object(" + XWikiRepositoryModel.EXTENSION_CLASSNAME
                + ") as extension where extension." + XWikiRepositoryModel.PROP_EXTENSION_ID + " = :extensionId",
                Query.XWQL);

        query.bindValue("extensionId", extensionId);

        List<String> documentNames = query.execute();

        if (documentNames.isEmpty()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        XWikiContext xcontext = getXWikiContext();

        return xcontext.getWiki().getDocument(documentNames.get(0), xcontext);
    }

    @Override
    public void validateExtension(XWikiDocument document, boolean readOnly) throws XWikiException
    {
        BaseObject extensionObject = document.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);

        if (extensionObject == null) {
            // Not an extension
            return;
        }

        boolean needSave = false;

        XWikiContext xcontext = getXWikiContext();

        XWikiDocument documentToSave = null;
        BaseObject extensionObjectToSave = null;

        // Update last version field
        String lastVersion = findLastVersion(document);

        if (lastVersion != null
            && !StringUtils.equals(lastVersion,
                extensionObject.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION))) {
            // FIXME: We can't save directly the provided document coming from the event
            documentToSave = xcontext.getWiki().getDocument(document, xcontext);
            extensionObjectToSave = documentToSave.getXObject(extensionObject.getReference());

            extensionObjectToSave.set(XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION, lastVersion, xcontext);

            needSave = true;
        }

        // Update valid extension field

        boolean valid = isValid(document, extensionObject, xcontext);

        int currentValue = extensionObject.getIntValue(XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION, 0);

        if ((currentValue == 1) != valid) {
            if (documentToSave == null) {
                // FIXME: We can't save directly the provided document coming from the event
                documentToSave = xcontext.getWiki().getDocument(document, xcontext);
                extensionObjectToSave = documentToSave.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
            }

            extensionObjectToSave.setIntValue(XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION, valid ? 1 : 0);

            needSave = true;
        }

        // Make sure all searched fields are set in valid extensions (otherwise they won't appear in the search result).
        // Would probably be cleaner and safer to do a left join instead but can't find a standard way to do it trough
        // XWQL or HSQL.

        if (valid) {
            for (String fieldName : AbstractExtensionRESTResource.EPROPERTIES_EXTRA) {
                if (extensionObject.safeget(fieldName) == null) {
                    if (extensionObjectToSave == null) {
                        // FIXME: We can't save directly the provided document coming from the event
                        documentToSave = xcontext.getWiki().getDocument(document, xcontext);
                        extensionObjectToSave =
                            documentToSave.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
                    }

                    extensionObjectToSave.set(fieldName, "", xcontext);
                    needSave = true;
                }
            }
        }

        // Save document

        if (needSave) {
            xcontext.getWiki().saveDocument(documentToSave, "Validated extension", true, xcontext);
        }
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

        DefaultVersion lastVersion = null;
        if (versionObjects != null) {
            for (BaseObject versionObject : versionObjects) {
                if (versionObject != null) {
                    String versionString = versionObject.getStringValue(XWikiRepositoryModel.PROP_VERSION_VERSION);
                    if (versionString != null) {
                        DefaultVersion version = new DefaultVersion(versionString);
                        if (lastVersion == null || version.compareTo(lastVersion) > 0) {
                            lastVersion = version;
                        }
                    }
                }
            }
        }

        return lastVersion != null ? lastVersion.getValue() : null;
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

                        ResourceReference resourceReference = getDownloadReference(document, extensionVersionObject);

                        if (resourceReference != null) {
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
                            valid = false;
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

    @Override
    public void validateExtensions() throws QueryException, XWikiException
    {
        Query query =
            this.queryManager.createQuery("from doc.object(" + XWikiRepositoryModel.EXTENSION_CLASSNAME
                + ") as extension", Query.XWQL);

        XWikiContext xcontext = getXWikiContext();

        for (String documentFullName : query.<String> execute()) {
            validateExtension(xcontext.getWiki().getDocument(documentFullName, xcontext), false);
        }
    }

    @Override
    public ResourceReference getDownloadReference(XWikiDocument document, BaseObject extensionVersionObject)
    {
        // Has a version
        String extensionVersion = extensionVersionObject.getStringValue(XWikiRepositoryModel.PROP_VERSION_VERSION);

        // The download reference seems ok
        String download = extensionVersionObject.getStringValue(XWikiRepositoryModel.PROP_VERSION_DOWNLOAD);

        ResourceReference resourceReference = null;

        if (StringUtils.isNotEmpty(download)) {
            resourceReference = this.resourceReferenceParser.parse(download);
        } else {
            BaseObject extensionObject = document.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
            String extensionId = extensionObject.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_ID);

            String fileName =
                extensionId + '-' + extensionVersion + '.'
                    + extensionObject.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_TYPE);

            XWikiAttachment attachment = document.getAttachment(fileName);
            if (attachment == null) {
                // Try without the prefix
                int index = extensionId.indexOf(':');
                if (index != -1 && index < extensionId.length()) {
                    fileName =
                        extensionId.substring(index + 1) + '-' + extensionVersion + '.'
                            + extensionObject.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_TYPE);

                    attachment = document.getAttachment(fileName);
                    if (attachment != null) {
                        resourceReference =
                            new AttachmentResourceReference(this.entityReferenceSerializer.serialize(attachment
                                .getReference()));
                    }
                }
            }
        }

        return resourceReference;
    }
}
