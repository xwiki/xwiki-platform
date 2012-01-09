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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.context.Execution;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.internal.reference.ExtensionResourceReference;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.xwiki.internal.resources.AbstractExtensionRESTResource;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.Version.Type;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
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
import org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

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
    private ResourceReferenceTypeSerializer resourceReferenceSerializer;

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

    @Inject
    private Logger logger;

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }

    @Override
    public XWikiDocument getExistingExtensionDocumentById(String extensionId) throws QueryException, XWikiException
    {
        Query query =
            this.queryManager.createQuery("from doc.object(" + XWikiRepositoryModel.EXTENSION_CLASSNAME
                + ") as extension where extension." + XWikiRepositoryModel.PROP_EXTENSION_ID + " = :extensionId",
                Query.XWQL);

        query.bindValue("extensionId", extensionId);

        List<String> documentNames = query.execute();

        if (documentNames.isEmpty()) {
            return null;
        }

        XWikiContext xcontext = getXWikiContext();

        return xcontext.getWiki().getDocument(documentNames.get(0), xcontext);
    }

    public BaseObject getExtensionVersion(XWikiDocument document, Version version)
    {
        for (BaseObject versionObject : document.getXObjects(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE)) {
            if (versionObject != null) {
                String versionString =
                    getValue(versionObject, XWikiRepositoryModel.PROP_VERSION_VERSION, (String) null);

                if (StringUtils.isNotEmpty(versionString) && version.equals(new DefaultVersion(versionString))) {
                    return versionObject;
                }
            }
        }

        return null;
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
                getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION, (String) null))) {
            // FIXME: We can't save directly the provided document coming from the event
            documentToSave = xcontext.getWiki().getDocument(document, xcontext);
            extensionObjectToSave = documentToSave.getXObject(extensionObject.getReference());

            extensionObjectToSave.set(XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION, lastVersion, xcontext);

            needSave = true;
        }

        // Update valid extension field

        boolean valid = isValid(document, extensionObject, xcontext);

        int currentValue = getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION, 0);

        if ((currentValue == 1) != valid) {
            if (documentToSave == null) {
                // FIXME: We can't save directly the provided document coming from the event
                documentToSave = xcontext.getWiki().getDocument(document, xcontext);
                extensionObjectToSave = documentToSave.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
            }

            extensionObjectToSave.set(XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION, valid ? "1" : "0", xcontext);

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
                    String versionString = getValue(versionObject, XWikiRepositoryModel.PROP_VERSION_VERSION);
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
        String extensionId = getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ID);
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
                            getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_VERSION);
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
                            } else if (ResourceType.URL.equals(resourceReference.getType())
                                || ExtensionResourceReference.TYPE.equals(resourceReference.getType())) {
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
        String extensionVersion = getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_VERSION);

        // The download reference seems ok
        String download = getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_DOWNLOAD);

        ResourceReference resourceReference = null;

        if (StringUtils.isNotEmpty(download)) {
            resourceReference = this.resourceReferenceParser.parse(download);
        } else {
            BaseObject extensionObject = document.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
            String extensionId = getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ID);

            String fileName =
                extensionId + '-' + extensionVersion + '.'
                    + getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_TYPE);

            XWikiAttachment attachment = document.getAttachment(fileName);
            if (attachment == null) {
                // Try without the prefix
                int index = fileName.indexOf(':');
                if (index != -1 && index < extensionId.length()) {
                    fileName = fileName.substring(index + 1);

                    attachment = document.getAttachment(fileName);
                }
            }

            if (attachment != null) {
                resourceReference =
                    new AttachmentResourceReference(this.entityReferenceSerializer.serialize(attachment.getReference()));
            }
        }

        return resourceReference;
    }

    @Override
    public void importExtension(Extension extension, boolean allVersions, Type type) throws QueryException,
        XWikiException, ResolveException
    {
        IterableResult<Version> versions;
        if (allVersions) {
            versions = extension.getRepository().resolveVersions(extension.getId().getId(), 0, -1);
        } else {
            versions = null;
        }

        XWikiContext xcontext = getXWikiContext();

        boolean needSave = false;

        XWikiDocument document = getExistingExtensionDocumentById(extension.getId().getId());

        if (document == null) {
            // Create document
            document =
                xcontext.getWiki().getDocument(
                    new DocumentReference(xcontext.getDatabase(), "Extension", extension.getName()), xcontext);

            for (int i = 1; !document.isNew(); ++i) {
                document =
                    xcontext.getWiki().getDocument(
                        new DocumentReference(xcontext.getDatabase(), "Extension", extension.getName() + ' ' + i),
                        xcontext);
            }

            needSave = true;
        }

        // Update document

        BaseObject extensionObject = document.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
        if (extensionObject == null) {
            extensionObject = document.newXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE, xcontext);
            needSave = true;
        }

        if (!StringUtils.equals(extension.getId().getId(),
            getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ID, (String) null))) {
            extensionObject.set(XWikiRepositoryModel.PROP_EXTENSION_ID, extension.getId().getId(), xcontext);
            needSave = true;
        }

        if (versions == null) {
            updateExtension(extension, extensionObject, xcontext);

            // Update version related informations
            needSave |= updateExtensionVersion(document, extension);
        } else {
            for (Iterator<Version> it = versions.iterator(); it.hasNext();) {
                Version version = it.next();
                try {
                    Extension versionExtension =
                        extension.getRepository().resolve(new ExtensionId(extension.getId().getId(), version));

                    if (!it.hasNext()) {
                        // Last version
                        needSave |= updateExtension(versionExtension, extensionObject, xcontext);
                    }

                    // Update version related informations
                    needSave |= updateExtensionVersion(document, versionExtension);
                } catch (Exception e) {
                    this.logger.error("Failed to resolve extension wuth id [" + extension.getId().getId()
                        + "] and version [" + version + "] on repository [" + extension.getRepository() + "]", e);
                }
            }
        }

        updateExtensionVersion(document, extension);

        if (needSave) {
            xcontext.getWiki().saveDocument(document,
                "Imported extension from repository [" + extension.getRepository() + "]", true, xcontext);
        }
    }

    private boolean updateExtension(Extension extension, BaseObject extensionObject, XWikiContext xcontext)
    {
        boolean needSave = false;

        // Update properties

        // Type
        needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_TYPE, extension.getType());

        // Name
        needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_NAME, extension.getName());

        // Summary
        needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SUMMARY, extension.getSummary());

        // Website
        needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_WEBSITE, extension.getWebSite());

        // Description
        if (StringUtils.isEmpty(getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION,
            (String) null))) {
            extensionObject.set(XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION, extension.getDescription(), xcontext);
            needSave = true;
        }

        // License
        if (!extension.getLicenses().isEmpty()
            && !StringUtils.equals(extension.getLicenses().iterator().next().getName(),
                getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME, (String) null))) {
            extensionObject.set(XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME, extension.getLicenses().iterator()
                .next().getName(), xcontext);
            needSave = true;
        }

        // Authors
        needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_AUTHORS, extension.getAuthors());

        // Features
        needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_FEATURES, extension.getFeatures());

        return needSave;
    }

    private boolean updateExtensionVersionDependencies(XWikiDocument document, Extension extension)
        throws XWikiException
    {
        boolean needSave = false;

        Set<ExtensionDependency> dependenciesToAdd = new HashSet<ExtensionDependency>(extension.getDependencies());

        List<BaseObject> xobjects = document.getXObjects(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSREFERENCE);
        for (int i = 0; i < xobjects.size(); ++i) {
            BaseObject dependencyObject = xobjects.get(i);

            if (dependencyObject != null) {
                String extensionVersion =
                    getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION, (String) null);

                if (StringUtils.isNotEmpty(extensionVersion)
                    && extension.getId().getVersion().equals(new DefaultVersion(extensionVersion))) {
                    String id = getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_ID);
                    String constraint = getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_CONSTRAINT);

                    ExtensionDependency dependency =
                        new DefaultExtensionDependency(id, new DefaultVersionConstraint(constraint));

                    if (!dependenciesToAdd.remove(dependency)) {
                        document.removeXObject(dependencyObject);
                        needSave = true;

                        --i;
                    }
                }
            }
        }

        // Add missing dependencies
        XWikiContext xcontext = getXWikiContext();
        for (ExtensionDependency dependency : dependenciesToAdd) {
            BaseObject dependencyObject =
                document.newXObject(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSREFERENCE, xcontext);

            dependencyObject.set(XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION, extension.getId().getVersion()
                .getValue(), xcontext);
            dependencyObject.set(XWikiRepositoryModel.PROP_DEPENDENCY_ID, dependency.getId(), xcontext);
            dependencyObject.set(XWikiRepositoryModel.PROP_DEPENDENCY_CONSTRAINT, dependency.getVersionConstraint()
                .getValue(), xcontext);

            needSave = true;
        }

        return needSave;
    }

    private boolean updateExtensionVersion(XWikiDocument document, Extension extension) throws XWikiException
    {
        boolean needSave;

        XWikiContext xcontext = getXWikiContext();

        // Update version object
        BaseObject versionObject = getExtensionVersion(document, extension.getId().getVersion());
        if (versionObject == null) {
            versionObject = document.newXObject(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE, xcontext);

            versionObject.set(XWikiRepositoryModel.PROP_VERSION_VERSION, extension.getId().getVersion().getValue(),
                xcontext);

            needSave = true;
        } else {
            needSave = false;
        }

        // Update dependencies
        needSave |= updateExtensionVersionDependencies(document, extension);

        // Download

        ExtensionResourceReference resource =
            new ExtensionResourceReference(extension.getId().getId(), extension.getId().getVersion().getValue(),
                extension.getRepository().getId().getId());
        String download = this.resourceReferenceSerializer.serialize(resource);
        needSave |= update(versionObject, XWikiRepositoryModel.PROP_VERSION_DOWNLOAD, download);

        return needSave;
    }

    protected <T> T getValue(BaseObject object, String field)
    {
        return getValue(object, field, (T) null);
    }

    protected <T> T getValue(BaseObject object, String field, T def)
    {
        BaseProperty< ? > property = (BaseProperty< ? >) object.safeget(field);

        return property != null ? (T) property.getValue() : def;
    }

    protected boolean update(BaseObject object, String fieldName, Object value)
    {
        if (ObjectUtils.notEqual(value, getValue(object, fieldName, (String) null))) {
            object.set(fieldName, value, getXWikiContext());

            return true;
        }

        return false;
    }
}
