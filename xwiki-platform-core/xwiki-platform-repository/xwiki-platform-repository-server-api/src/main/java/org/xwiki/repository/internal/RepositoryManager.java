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
package org.xwiki.repository.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionAuthor;
import org.xwiki.extension.ExtensionComponent;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionIssueManagement;
import org.xwiki.extension.ExtensionNotFoundException;
import org.xwiki.extension.ExtensionScm;
import org.xwiki.extension.ExtensionSupportPlans;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.internal.ExtensionUtils;
import org.xwiki.extension.internal.converter.ExtensionComponentConverter;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.result.IterableResult;
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
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer;
import org.xwiki.repository.internal.reference.ExtensionResourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;

@Component(roles = RepositoryManager.class)
@Singleton
public class RepositoryManager
{
    private static final Pattern PATTERN_NEWLINE = Pattern.compile("[\n\r]");

    /**
     * Get the reference of the class in the current wiki.
     */
    @Inject
    @Named("default")
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentStringResolver;

    /**
     * Used to validate download reference.
     */
    @Inject
    @Named("link")
    private ResourceReferenceParser resourceReferenceParser;

    @Inject
    private ResourceReferenceTypeSerializer resourceReferenceSerializer;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    /**
     * Used to validate download reference.
     */
    @Inject
    private AttachmentReferenceResolver<String> attachmentResolver;

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private RepositoryConfiguration configuration;

    @Inject
    private ExtensionFactory extensionFactory;

    @Inject
    protected ExtensionStore extensionStore;

    @Inject
    private Logger logger;

    private int maxTitleSize = -1;

    private int maxStringPropertySize = -1;

    public void validateExtension(XWikiDocument document, boolean save) throws XWikiException
    {
        BaseObject extensionObject = document.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);

        if (extensionObject == null) {
            // Not an extension
            return;
        }

        boolean needSave = false;

        XWikiContext xcontext = this.xcontextProvider.get();

        // Update the legacy recommended property based on the support

        ExtensionSupportPlans supportPlans = this.extensionStore.resolveExtensionSupportPlans(
            this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SUPPORTPLANS));
        boolean recommended = !supportPlans.getSupporters().isEmpty();
        if (this.extensionStore.getBooleanValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_RECOMMENDED,
            false) != recommended) {
            extensionObject.set(XWikiRepositoryModel.PROP_EXTENSION_RECOMMENDED, recommended ? "1" : "0", xcontext);

            needSave = true;
        }

        // Update last version field

        String lastVersion = StringUtils.defaultString(findLastVersion(document));
        if (!Strings.CS.equals(lastVersion, this.extensionStore.getValue(extensionObject,
            XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION, (String) null))) {
            extensionObject.set(XWikiRepositoryModel.PROP_EXTENSION_LASTVERSION, lastVersion, xcontext);

            needSave = true;
        }

        // Update valid extension field

        boolean valid;
        if (StringUtils.isEmpty(lastVersion)) {
            valid = false;
        } else {
            valid = isValid(document, extensionObject, xcontext);
        }

        if (valid) {
            this.logger.debug("The extension in the document [{}] is valid", document.getDocumentReference());
        } else {
            this.logger.debug("The extension in the document [{}] is not valid", document.getDocumentReference());
        }

        if (this.extensionStore.getBooleanValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION,
            false) != valid) {
            extensionObject.set(XWikiRepositoryModel.PROP_EXTENSION_VALIDEXTENSION, valid ? "1" : "0", xcontext);

            needSave = true;
        }

        // Save document

        if (save && needSave) {
            xcontext.getWiki().saveDocument(document, "Validated extension", true, xcontext);
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
        String extensionId = getExtensionId(document);

        DocumentReference versionClassReference =
            getClassReference(document, XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE);

        List<BaseObject> versionObjects = document.getXObjects(versionClassReference);

        DefaultVersion lastVersion = null;
        if (versionObjects != null) {
            for (BaseObject versionObject : versionObjects) {
                if (versionObject != null) {
                    String versionId =
                        this.extensionStore.getValue(versionObject, XWikiRepositoryModel.PROP_VERSION_ID);

                    if (StringUtils.isEmpty(versionId) || Objects.equals(extensionId, versionId)) {
                        String versionString =
                            this.extensionStore.getValue(versionObject, XWikiRepositoryModel.PROP_VERSION_VERSION);
                        if (versionString != null) {
                            DefaultVersion version = new DefaultVersion(versionString);
                            if (lastVersion == null || version.compareTo(lastVersion) > 0) {
                                lastVersion = version;
                            }
                        }
                    }
                }
            }
        }

        return lastVersion != null ? lastVersion.getValue() : null;
    }

    private String getExtensionId(XWikiDocument document)
    {
        return document.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_ID);
    }

    private DocumentReference getClassReference(XWikiDocument document, EntityReference localReference)
    {
        return this.referenceResolver.resolve(localReference, document.getDocumentReference().getWikiReference());
    }

    /**
     * @param document the extension document
     * @param extensionObject the extension object
     * @param xcontext the XWiki context
     * @return true if the extension is valid from Extension Manager point of view
     * @throws XWikiException unknown issue when manipulating the model
     */
    private boolean isValid(XWikiDocument document, BaseObject extensionObject, XWikiContext xcontext)
        throws XWikiException
    {
        String extensionId = this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ID);
        boolean valid = !StringUtils.isBlank(extensionId);
        if (valid) {
            // Type
            String type = this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_TYPE);

            valid = this.configuration.isValidType(type);

            if (valid) {
                // Versions
                valid = false;
                List<BaseObject> extensionVersions =
                    document.getXObjects(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE);
                if (extensionVersions != null) {
                    for (BaseObject extensionVersionObject : extensionVersions) {
                        if (extensionVersionObject != null) {
                            valid = isVersionValid(document, type, extensionVersionObject, xcontext);

                            if (!valid) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return valid;
    }

    private boolean isVersionValid(XWikiDocument document, String type, BaseObject extensionVersionObject,
        XWikiContext xcontext)
    {
        // Has a version
        String extensionVersion =
            this.extensionStore.getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_VERSION);
        if (StringUtils.isBlank(extensionVersion)) {
            this.logger.debug("No actual version provided for object [{}({})]",
                XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE, extensionVersionObject.getNumber());

            return false;
        }

        boolean valid;

        if (StringUtils.isEmpty(type)) {
            // We don't care about the file when type is "no file"
            valid = true;
        } else {
            ResourceReference resourceReference = null;
            try {
                resourceReference = getDownloadReference(document, extensionVersion, xcontext);
            } catch (ResolveException e) {
                logger.debug("Cannot obtain download source reference for object [{}({})]",
                    XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE, extensionVersionObject.getNumber());
                return false;
            }

            if (resourceReference != null) {
                if (ResourceType.ATTACHMENT.equals(resourceReference.getType())) {
                    AttachmentReference attachmentReference = this.attachmentResolver
                        .resolve(resourceReference.getReference(), document.getDocumentReference());

                    XWikiDocument attachmentDocument;
                    try {
                        if (attachmentReference.getDocumentReference().equals(document.getDocumentReference())) {
                            attachmentDocument = document;
                        } else {
                            attachmentDocument =
                                xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);
                        }

                        valid = attachmentDocument.getAttachment(attachmentReference.getName()) != null;
                    } catch (XWikiException e) {
                        this.logger.error("Failed to get document [{}]", attachmentReference.getDocumentReference(), e);

                        valid = false;
                    }

                    if (!valid) {
                        this.logger.debug("Attachment [{}] does not exist", attachmentReference);
                    }
                } else if (ResourceType.URL.equals(resourceReference.getType())
                    || ExtensionResourceReference.TYPE.equals(resourceReference.getType())) {
                    valid = true;
                } else {
                    valid = false;

                    this.logger.debug("Unknown resource type [{}]", resourceReference.getType());
                }
            } else {
                valid = false;

                this.logger.debug("No actual download provided for object [{}({})]",
                    XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE, extensionVersionObject.getNumber());
            }
        }

        return valid;
    }

    public void validateExtensions() throws QueryException, XWikiException
    {
        Query query = this.queryManager.createQuery("select doc.fullName from Document doc, doc.object("
            + XWikiRepositoryModel.EXTENSION_CLASSNAME + ") as extension", Query.XWQL);

        for (String documentName : query.<String>execute()) {
            validateExtension(this.extensionStore.getDocument(documentName), true);
        }
    }

    /**
     * @since 9.5RC1
     */
    public ResourceReference getDownloadReference(XWikiDocument document, String extensionVersion,
        XWikiContext xcontext) throws ResolveException
    {
        String downloadURL = null;

        BaseObject extensionVersionObject = getExtensionVersionObject(document, extensionVersion, false, xcontext);
        if (extensionVersionObject != null) {
            downloadURL =
                this.extensionStore.getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_DOWNLOAD);
        } else if (this.extensionStore.isVersionProxyingEnabled(document)) {
            downloadURL = resolveExtensionDownloadURL(document, extensionVersion);
        }

        ResourceReference resourceReference = null;

        if (StringUtils.isNotEmpty(downloadURL)) {
            resourceReference = this.resourceReferenceParser.parse(downloadURL);
        } else {
            BaseObject extensionObject = document.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
            String extensionId = this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ID);

            String fileName = extensionId + '-' + extensionVersion + '.'
                + this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_TYPE);

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
                resourceReference = new AttachmentResourceReference(
                    this.entityReferenceSerializer.serialize(attachment.getReference()));
            }
        }

        return resourceReference;
    }

    /**
     * This method should be used only, when the info about extension version cannot be obtained from document xobjects
     * but should be proxied.
     */
    private String resolveExtensionDownloadURL(XWikiDocument extensionDocument, String extensionVersion)
        throws ResolveException
    {
        Extension extension = resolveExtensionVersion(extensionDocument, extensionVersion);
        if (extension == null) {
            return null;
        }
        return getDownloadURL(extension);
    }

    private Version getVersions(String extensionId, ExtensionRepository repository, Type type,
        Map<Version, String> versions) throws ResolveException
    {
        Version lastVersion = null;

        IterableResult<Version> versionsIterable = repository.resolveVersions(extensionId, 0, -1);

        for (Version version : versionsIterable) {
            if (type == null || version.getType() == type) {
                versions.putIfAbsent(version, extensionId);
            }

            lastVersion = version;
        }

        return lastVersion;
    }

    public DocumentReference importExtension(String extensionId, ExtensionRepository repository, Type type)
        throws QueryException, XWikiException, ResolveException
    {
        TreeMap<Version, String> extensionVersions = new TreeMap<>();

        Version lastVersion = getVersions(extensionId, repository, type, extensionVersions);

        if (lastVersion == null) {
            throw new ExtensionNotFoundException(
                "Can't find any version for the extension [" + extensionId + "] on repository [" + repository + "]");
        } else if (extensionVersions.isEmpty()) {
            // If no valid version import the last version
            extensionVersions.put(lastVersion, extensionId);
        } else {
            // Select the last valid version
            lastVersion = extensionVersions.lastKey();
        }

        Extension extension = repository.resolve(new ExtensionId(extensionId, lastVersion));

        XWikiContext xcontext = this.xcontextProvider.get();

        boolean needSave = false;

        XWikiDocument extensionDocument = this.extensionStore.getExistingExtensionDocumentById(extensionId);

        if (extensionDocument == null) {
            // Create document
            extensionDocument = xcontext.getWiki().getDocument(
                new DocumentReference(xcontext.getWikiId(), Arrays.asList("Extension", extension.getName()), "WebHome"),
                xcontext);

            for (int i = 1; !extensionDocument.isNew(); ++i) {
                extensionDocument = xcontext.getWiki().getDocument(new DocumentReference(xcontext.getWikiId(),
                    Arrays.asList("Extension", extension.getName() + ' ' + i), "WebHome"), xcontext);
            }

            extensionDocument.readFromTemplate(
                this.currentResolver.resolve(XWikiRepositoryModel.EXTENSION_TEMPLATEREFERENCE), xcontext);

            needSave = true;
        }

        // Update document

        BaseObject extensionObject = extensionDocument.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
        if (extensionObject == null) {
            extensionObject = extensionDocument.newXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE, xcontext);
            needSave = true;
        }

        if (!Strings.CS.equals(extensionId,
            this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ID, (String) null))) {
            extensionObject.set(XWikiRepositoryModel.PROP_EXTENSION_ID, extensionId, xcontext);
            needSave = true;
        }

        // Update extension informations

        needSave |= updateExtensionMain(extension, extensionObject, xcontext);
        needSave |= updateExtension(extension, extensionObject, xcontext);

        // Get former ids versions
        TreeMap<Version, String> featureVersions = new TreeMap<>();
        List<String> previousIds = extensionObject.getListValue(XWikiRepositoryModel.PROP_EXTENSION_PREVIOUSIDS);
        if (previousIds.isEmpty()) {
            // If it's empty but not explicitly empty it means it's unset so we use the old behavior of using features
            // as previous ids
            if (extensionObject.getIntValue(XWikiRepositoryModel.PROP_EXTENSION_PREVIOUSIDS_EMPTY, 0) == 0) {
                Collection<ExtensionId> features = extension.getExtensionFeatures();
                for (ExtensionId feature : features) {
                    try {
                        getVersions(feature.getId(), repository, type, featureVersions);
                    } catch (ResolveException e) {
                        // Ignore
                    }
                }
            }
        } else {
            // Use the explicit previous ids
            for (String previousId : previousIds) {
                try {
                    getVersions(previousId, repository, type, featureVersions);
                } catch (ResolveException e) {
                    // Ignore
                }
            }
        }

        // Proxy marker

        BaseObject extensionProxyObject =
            extensionDocument.getXObject(XWikiRepositoryModel.EXTENSIONPROXY_CLASSREFERENCE);
        if (extensionProxyObject == null) {
            extensionProxyObject =
                extensionDocument.newXObject(XWikiRepositoryModel.EXTENSIONPROXY_CLASSREFERENCE, xcontext);
            extensionProxyObject.setIntValue(XWikiRepositoryModel.PROP_PROXY_AUTOUPDATE, 1);
            needSave = true;
        }

        needSave |= update(extensionProxyObject, XWikiRepositoryModel.PROP_PROXY_REPOSITORYID,
            repository.getDescriptor().getId());
        needSave |= update(extensionProxyObject, XWikiRepositoryModel.PROP_PROXY_REPOSITORYTYPE,
            repository.getDescriptor().getType());
        needSave |= update(extensionProxyObject, XWikiRepositoryModel.PROP_PROXY_REPOSITORYURI,
            repository.getDescriptor().getURI().toString());

        // Remove unexisting versions

        Set<String> validVersions = new HashSet<>();

        List<BaseObject> versionObjects =
            extensionDocument.getXObjects(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE);
        if (versionObjects != null) {
            for (BaseObject versionObject : versionObjects) {
                if (versionObject != null) {
                    String version =
                        this.extensionStore.getValue(versionObject, XWikiRepositoryModel.PROP_VERSION_VERSION);

                    if (StringUtils.isBlank(version) || (this.extensionStore.isVersionProxyingEnabled(extensionDocument)
                        && !new DefaultVersion(version).equals(extension.getId().getVersion()))) {
                        // Empty version OR old versions should be proxied
                        extensionDocument.removeXObject(versionObject);
                        needSave = true;
                    } else {
                        if (!extensionVersions.containsKey(new DefaultVersion(version))
                            && featureVersions.containsKey(new DefaultVersion(version))) {
                            // The version does not exist on remote repository
                            if (!isVersionValid(extensionDocument, extension.getType(), versionObject, xcontext)) {
                                // The version is invalid, removing it to not make the whole extension invalid
                                extensionDocument.removeXObject(versionObject);
                                needSave = true;
                            } else {
                                // The version is valid, lets keep it
                                validVersions.add(version);
                            }
                        } else {
                            // This version exist on remote repository
                            validVersions.add(version);
                        }
                    }
                }
            }
        }
        List<BaseObject> dependencyObjects =
            extensionDocument.getXObjects(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSREFERENCE);
        if (dependencyObjects != null) {
            for (BaseObject dependencyObject : dependencyObjects) {
                if (dependencyObject != null) {
                    String version = this.extensionStore.getValue(dependencyObject,
                        XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION);

                    if (!validVersions.contains(version)) {
                        // The version is invalid, removing it to not make the whole extension invalid
                        extensionDocument.removeXObject(dependencyObject);
                        needSave = true;
                    }
                }
            }
        }

        // Update features versions

        for (Map.Entry<Version, String> entry : featureVersions.entrySet()) {
            Version version = entry.getKey();
            String id = entry.getValue();

            // Give priority to extension version in case of conflict
            if (!extensionVersions.containsKey(version)) {
                updateVersion(id, version, extension, repository, extensionDocument);
            }
        }

        // Update extension versions

        for (Map.Entry<Version, String> entry : extensionVersions.entrySet()) {
            Version version = entry.getKey();
            String id = entry.getValue();

            updateVersion(id, version, extension, repository, extensionDocument);
        }

        // Save

        if (needSave) {
            extensionDocument.setAuthorReference(xcontext.getUserReference());
            if (extensionDocument.isNew()) {
                extensionDocument.setContentAuthorReference(xcontext.getUserReference());
                extensionDocument.setCreatorReference(xcontext.getUserReference());
            }

            xcontext.getWiki().saveDocument(extensionDocument,
                "Imported extension [" + extensionId + "] from repository [" + repository.getDescriptor() + "]", true,
                xcontext);
        }

        return extensionDocument.getDocumentReference();
    }

    private boolean updateVersion(String id, Version version, Extension extension, ExtensionRepository repository,
        XWikiDocument document)
    {
        try {
            Extension versionExtension;
            if (version.equals(extension.getId().getVersion())) {
                versionExtension = extension;
            } else if (this.extensionStore.isVersionProxyingEnabled(document)) {
                return false;
            } else {
                versionExtension = repository.resolve(new ExtensionId(id, version));
            }

            // Update version related informations
            return updateExtensionVersion(document, versionExtension);
        } catch (Exception e) {
            this.logger.error("Failed to resolve extension with id [" + id + "] and version [" + version
                + "] on repository [" + repository + "]", e);
        }

        return false;
    }

    /**
     * @since 42
     */
    public BaseObject getExtensionVersionObject(XWikiDocument extensionDocument, String version, XWikiContext xcontext)
        throws XWikiException
    {
        return getExtensionVersionObject(
            this.extensionStore.getExtensionVersionDocument(extensionDocument, version, xcontext), version, true,
            xcontext);
    }

    /**
     * @since 42
     */
    public BaseObject getExtensionVersionObject(XWikiDocument extensionDocument, String version, boolean allowProxying,
        XWikiContext xcontext)
    {
        if (version == null) {
            List<BaseObject> objects =
                extensionDocument.getXObjects(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE);

            if (objects == null || objects.isEmpty()) {
                return null;
            } else {
                return objects.get(objects.size() - 1);
            }
        }

        BaseObject extensionVersionObject = extensionDocument
            .getXObject(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE, "version", version, false);

        if (extensionVersionObject == null && allowProxying
            && this.extensionStore.isVersionProxyingEnabled(extensionDocument)) {
            // No ExtensionVersionClass object for the version, but proxy is enabled, so try to find remotely
            Extension extension = null;
            try {
                extension = resolveExtensionVersion(extensionDocument, version);
            } catch (ExtensionNotFoundException e) {
                this.logger.debug("No extension could be found remotely with version [{}] for extension page [{}]",
                    version, extensionDocument.getDocumentReference());
            } catch (ResolveException e) {
                throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
            }

            // No extension could be found for the provided version
            if (extension == null) {
                return null;
            }

            // Create a temporary xobject for that extension version
            // FIXME: find a more elegant solution
            try {
                XWikiDocument extensionDocumentClone = extensionDocument.clone();
                updateExtension(extension, extensionVersionObject, xcontext);
                updateExtensionVersion(extensionDocumentClone, extension);
                extensionVersionObject = extensionDocumentClone
                    .getXObject(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE, "version", version, false);
            } catch (XWikiException e) {
                throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        return extensionVersionObject;
    }

    private boolean updateExtensionMain(Extension extension, BaseObject extensionObject, XWikiContext xcontext)
        throws XWikiException
    {
        boolean needSave = false;

        // Description
        if (StringUtils.isEmpty(this.extensionStore.getValue(extensionObject,
            XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION, (String) null))) {
            extensionObject.set(XWikiRepositoryModel.PROP_EXTENSION_DESCRIPTION, getDescription(extension), xcontext);
            needSave = true;
        }

        // Issue Management
        ExtensionIssueManagement issueManagement = extension.getIssueManagement();
        if (issueManagement != null) {
            if (issueManagement.getSystem() != null) {
                needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_SYSTEM,
                    issueManagement.getSystem());
            }
            if (issueManagement.getURL() != null) {
                needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ISSUEMANAGEMENT_URL,
                    issueManagement.getURL());
            }
        }

        return needSave;
    }

    private boolean updateExtension(Extension extension, BaseObject extensionObject, XWikiContext xcontext)
        throws XWikiException
    {
        boolean needSave = false;

        // Update properties

        // Type
        needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_TYPE,
            StringUtils.defaultString(extension.getType()));

        // Name
        needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_NAME, extension.getName());

        // Summary
        needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SUMMARY, getSummary(extension));

        // Category
        if (extension.getCategory() != null) {
            needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_CATEGORY, extension.getCategory());
        }

        // Website
        /*
         * Don't import website since most of the time we want the new page to be the extension entry point needSave |=
         * update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_WEBSITE, extension.getWebSite());
         */

        // License
        if (!extension.getLicenses().isEmpty()
            && !Strings.CS.equals(extension.getLicenses().iterator().next().getName(), this.extensionStore
                .getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME, (String) null))) {
            extensionObject.set(XWikiRepositoryModel.PROP_EXTENSION_LICENSENAME,
                extension.getLicenses().iterator().next().getName(), xcontext);
            needSave = true;
        }

        // SCM
        ExtensionScm scm = extension.getScm();
        if (scm != null) {
            if (scm.getUrl() != null) {
                needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SCMURL, scm.getUrl());
            }
            if (scm.getConnection() != null) {
                needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SCMCONNECTION,
                    scm.getConnection().toString());
            }
            if (scm.getDeveloperConnection() != null) {
                needSave |= update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_SCMDEVCONNECTION,
                    scm.getDeveloperConnection().toString());
            }
        }

        // Authors
        needSave |= updateAuthors(extensionObject, extension.getAuthors());

        // Features
        needSave |= updateFeatures(XWikiRepositoryModel.PROP_EXTENSION_FEATURES, extensionObject,
            extension.getExtensionFeatures());

        // Previous ids
        String previousIdsString =
            extension.getProperty(Extension.IKEYPREFIX + XWikiRepositoryModel.PROP_EXTENSION_PREVIOUSIDS);
        if (previousIdsString != null) {
            List<String> previousIds = ExtensionUtils.importPropertyStringList(previousIdsString, true);
            needSave |= updateCollection(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_PREVIOUSIDS, previousIds,
                xcontext);
        }

        // Allowed namespaces
        needSave |= updateCollection(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ALLOWEDNAMESPACES,
            extension.getAllowedNamespaces(), xcontext);

        // Properties
        needSave |= updateProperties(extensionObject, extension);

        return needSave;
    }

    private String getSummary(Extension extension)
    {
        String summary = extension.getSummary();
        if (summary != null) {
            // Extract first not blank line
            Matcher matcher = PATTERN_NEWLINE.matcher(summary);
            int previousIndex = 0;
            while (matcher.find()) {
                int index = matcher.start();
                String str = summary.substring(previousIndex, index);
                if (StringUtils.isNotBlank(str)) {
                    summary = str.trim();
                    break;
                }
            }
            // truncated to max title size in case it's too long, TODO: should probably be handled at a lower level)
            if (summary.length() > getMaxTitleSize()) {
                summary = summary.substring(0, getMaxTitleSize());
            }
        } else {
            summary = "";
        }

        return summary;
    }

    private int getMaxTitleSize()
    {
        if (this.maxTitleSize == -1) {
            XWikiContext xcontext = this.xcontextProvider.get();
            if (xcontext != null) {
                this.maxTitleSize = xcontext.getWiki().getStore().getLimitSize(xcontext, XWikiDocument.class, "title");
            }
        }

        return this.maxTitleSize != -1 ? this.maxTitleSize : 768;
    }

    private int getMaxStringPropertySize()
    {
        if (this.maxStringPropertySize == -1) {
            XWikiContext xcontext = this.xcontextProvider.get();
            if (xcontext != null) {
                this.maxStringPropertySize =
                    xcontext.getWiki().getStore().getLimitSize(xcontext, StringProperty.class, "value");
            }
        }

        return this.maxStringPropertySize != -1 ? this.maxStringPropertySize : 768;
    }

    private String getDescription(Extension extension)
    {
        String description;

        if (extension.getDescription() != null) {
            description = extension.getDescription();
        } else if (extension.getSummary() != null) {
            description = extension.getSummary();
        } else {
            description = "";
        }

        return description;
    }

    private boolean updateAuthors(BaseObject extensionObject, Collection<ExtensionAuthor> authors) throws XWikiException
    {
        List<String> authorIds = new ArrayList<>(authors.size());

        for (ExtensionAuthor author : authors) {
            authorIds.add(resolveAuthorId(author.getName()));
        }

        return update(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_AUTHORS, authorIds);
    }

    private boolean updateFeatures(String fieldName, BaseObject extensionObject, Collection<ExtensionId> features)
        throws XWikiException
    {
        List<String> featureStrings = new ArrayList<>(features.size());

        for (ExtensionId feature : features) {
            featureStrings.add(ExtensionIdConverter.toString(feature));
        }

        return update(extensionObject, fieldName, featureStrings);
    }

    private String resolveAuthorId(String authorName)
    {
        String[] authorElements = StringUtils.split(authorName, ' ');

        XWikiContext xcontext = this.xcontextProvider.get();

        String authorId = resolveAuthorIdOnWiki(xcontext.getWikiId(), authorName, authorElements, xcontext);

        if (authorId == null && !xcontext.isMainWiki()) {
            authorId = resolveAuthorIdOnWiki(xcontext.getMainXWiki(), authorName, authorElements, xcontext);

            if (authorId != null) {
                authorId = xcontext.getMainXWiki() + ':' + authorId;
            }
        }

        return authorId != null ? authorId : authorName;
    }

    private String resolveAuthorIdOnWiki(String wiki, String authorName, String[] authorElements, XWikiContext xcontext)
    {
        Query query;
        try {
            query = this.queryManager.createQuery("from doc.object(XWiki.XWikiUsers) as user"
                + " where user.first_name like :userfirstname OR user.last_name like :userlastname", Query.XWQL);

            query.bindValue("userfirstname", '%' + authorElements[0] + '%');
            query.bindValue("userlastname", '%' + authorElements[authorElements.length - 1] + '%');

            query.setWiki(wiki);

            List<String> documentNames = query.execute();

            if (!documentNames.isEmpty()) {
                WikiReference wikiReference = new WikiReference(wiki);
                for (String documentName : documentNames) {
                    DocumentReference documentReference =
                        this.currentStringResolver.resolve(documentName, wikiReference);

                    String userDisplayName = xcontext.getWiki().getPlainUserName(documentReference, xcontext);

                    if (userDisplayName.equals(authorName)) {
                        return documentName;
                    }
                }
            }
        } catch (QueryException e) {
            this.logger.error("Failed to resolve extension author [{}]", authorName, e);
        }

        return null;
    }

    private boolean updateExtensionVersionDependencies(XWikiDocument document, Extension extension)
        throws XWikiException
    {
        boolean needSave = false;

        List<ExtensionDependency> dependencies = new ArrayList<>(extension.getDependencies());
        int dependencyIndex = 0;

        // Clean misplaced or bad existing dependencies associated to this extension version
        List<BaseObject> xobjects = document.getXObjects(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSREFERENCE);
        if (xobjects != null) {
            boolean deleteExistingObjects = false;

            // Clone since we are going to modify and parse it at the same time
            xobjects = new ArrayList<>(document.getXObjects(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSREFERENCE));

            for (int i = 0; i < xobjects.size(); ++i) {
                BaseObject dependencyObject = xobjects.get(i);

                if (dependencyObject != null) {
                    String extensionVersion = this.extensionStore.getValue(dependencyObject,
                        XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION, (String) null);

                    if (StringUtils.isNotEmpty(extensionVersion)
                        && extension.getId().getVersion().equals(new DefaultVersion(extensionVersion))) {
                        if (deleteExistingObjects) {
                            document.removeXObject(dependencyObject);
                            needSave = true;
                        } else {
                            String xobjectId =
                                this.extensionStore.getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_ID);
                            String xobjectConstraint = this.extensionStore.getValue(dependencyObject,
                                XWikiRepositoryModel.PROP_DEPENDENCY_CONSTRAINT);
                            List<String> xobjectRepositories = (List<String>) this.extensionStore
                                .getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_REPOSITORIES);
                            boolean xobjectOptional = this.extensionStore.getBooleanValue(dependencyObject,
                                XWikiRepositoryModel.PROP_DEPENDENCY_OPTIONAL, false);

                            if (dependencies.size() > dependencyIndex) {
                                ExtensionDependency dependency = dependencies.get(dependencyIndex);

                                DefaultExtensionDependency xobjectDependency = new DefaultExtensionDependency(xobjectId,
                                    new DefaultVersionConstraint(xobjectConstraint), xobjectOptional,
                                    dependency.getProperties());
                                xobjectDependency.setRepositories(XWikiRepositoryModel
                                    .toRepositoryDescriptors(xobjectRepositories, this.extensionFactory));

                                if (dependency.equals(xobjectDependency)) {
                                    ++dependencyIndex;

                                    continue;
                                }
                            }

                            deleteExistingObjects = true;

                            document.removeXObject(dependencyObject);
                            needSave = true;
                        }
                    }
                }
            }
        }

        // Add missing dependencies
        if (dependencyIndex < dependencies.size()) {
            XWikiContext xcontext = this.xcontextProvider.get();
            for (; dependencyIndex < dependencies.size(); ++dependencyIndex) {
                ExtensionDependency dependency = dependencies.get(dependencyIndex);

                BaseObject dependencyObject =
                    document.newXObject(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSREFERENCE, xcontext);

                dependencyObject.set(XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION,
                    extension.getId().getVersion().getValue(), xcontext);
                dependencyObject.set(XWikiRepositoryModel.PROP_DEPENDENCY_ID, dependency.getId(), xcontext);
                dependencyObject.set(XWikiRepositoryModel.PROP_DEPENDENCY_CONSTRAINT,
                    dependency.getVersionConstraint().getValue(), xcontext);
                dependencyObject.set(XWikiRepositoryModel.PROP_DEPENDENCY_OPTIONAL, dependency.isOptional() ? 1 : 0,
                    xcontext);
                dependencyObject.set(XWikiRepositoryModel.PROP_DEPENDENCY_REPOSITORIES,
                    XWikiRepositoryModel.toStringList(dependency.getRepositories()), xcontext);

                needSave = true;
            }
        }

        return needSave;
    }

    /**
     * This method factually resolves extension from remote source (when it's possible). Call it only when the data that
     * is going to be obtained cannot be got from extension document xobject
     * 
     * @since 9.5RC1
     */
    public Extension resolveExtensionVersion(XWikiDocument extensionDocument, String extensionVersion)
        throws ResolveException
    {
        BaseObject extensionObject = extensionDocument.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
        if (extensionObject == null) {
            return null;
        }
        String extensionId =
            this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_ID, (String) null);

        BaseObject extensionProxyObject =
            extensionDocument.getXObject(XWikiRepositoryModel.EXTENSIONPROXY_CLASSREFERENCE);
        if (extensionProxyObject == null) {
            return null;
        }
        String repositoryId = this.extensionStore.getValue(extensionProxyObject,
            XWikiRepositoryModel.PROP_PROXY_REPOSITORYID, (String) null);

        if (extensionId == null || repositoryId == null) {
            return null;
        }

        ExtensionRepository repository = this.extensionRepositoryManager.getRepository(repositoryId);
        if (isGivenVersionOneOfExtensionVersions(repository, extensionId, extensionVersion)) {
            return repository.resolve(new ExtensionId(extensionId, extensionVersion));
        } else {
            return tryToResolveExtensionFromExtensionFeatures(repository, extensionObject, extensionVersion);
        }
    }

    /**
     * @return resolved extension version or null if extension is not resolvable
     */
    private Extension tryToResolveExtensionFromExtensionFeatures(ExtensionRepository repository,
        BaseObject extensionObject, String extensionVersion)
    {
        List<String> features =
            (List<String>) this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_FEATURES);
        return features.stream().map(feature -> {
            try {
                String featureId = feature.split("/")[0];
                return repository.resolve(new ExtensionId(featureId, extensionVersion));
            } catch (ResolveException e) {
                return null;
            }
        }).filter(extension -> extension != null).findFirst().orElse(null);
    }

    private boolean isGivenVersionOneOfExtensionVersions(ExtensionRepository repository, String extensionId,
        String extensionVersion) throws ResolveException
    {
        IterableResult<Version> versions = repository.resolveVersions(extensionId, 0, -1);
        return StreamSupport.stream(versions.spliterator(), false)
            .anyMatch(version -> version.getValue().equals(extensionVersion));
    }

    private boolean updateExtensionVersion(XWikiDocument document, Extension extensionVersion) throws XWikiException
    {
        boolean needSave = false;

        XWikiContext xcontext = this.xcontextProvider.get();

        // Update version object
        BaseObject versionObject =
            this.extensionStore.getExtensionVersionObject(document, extensionVersion.getId().getVersion());
        if (versionObject == null) {
            versionObject = document.newXObject(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE, xcontext);

            versionObject.set(XWikiRepositoryModel.PROP_VERSION_VERSION,
                extensionVersion.getId().getVersion().getValue(), xcontext);

            needSave = true;
        }

        // Id
        needSave |= update(versionObject, XWikiRepositoryModel.PROP_VERSION_ID, extensionVersion.getId().getId());

        // Features
        needSave |= updateFeatures(XWikiRepositoryModel.PROP_VERSION_FEATURES, versionObject,
            extensionVersion.getExtensionFeatures());

        // Repositories
        List<String> repositories = XWikiRepositoryModel.toStringList(extensionVersion.getRepositories());
        needSave |= update(versionObject, XWikiRepositoryModel.PROP_VERSION_REPOSITORIES, repositories);

        // Update dependencies
        needSave |= updateExtensionVersionDependencies(document, extensionVersion);

        // Download
        if (!StringUtils.isEmpty(extensionVersion.getType())) {
            String download = getDownloadURL(extensionVersion);
            needSave |= update(versionObject, XWikiRepositoryModel.PROP_VERSION_DOWNLOAD, download);
        }

        // Common properties
        updateExtension(extensionVersion, versionObject, xcontext);

        return needSave;
    }

    private String getDownloadURL(Extension extension)
    {
        ExtensionResourceReference resource = new ExtensionResourceReference(extension.getId().getId(),
            extension.getId().getVersion().getValue(), extension.getRepository().getDescriptor().getId());
        return this.resourceReferenceSerializer.serialize(resource);
    }

    protected boolean updateProperties(BaseObject object, Extension extension) throws XWikiException
    {
        Map<String, Object> map = extension.getProperties();

        // [Retro compatibility] Reinject extension components as "unknow" properties since that's what XWiki version
        // between 13.3 and 14.5 expect to find
        Collection<ExtensionComponent> components = extension.getComponents();
        if (!components.isEmpty()) {
            map = new LinkedHashMap<>(map);
            map.put(Extension.IKEYPREFIX + Extension.FIELD_COMPONENTS,
                StringUtils.join(ExtensionComponentConverter.toStringList(components), '\n'));
        }

        List<String> list = new ArrayList<>(map.size());
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String entryString = entry.getKey() + '=' + entry.getValue();
            if (entryString.length() > getMaxStringPropertySize()) {
                // Protect against properties too big
                entryString = entryString.substring(0, getMaxStringPropertySize());
            }
            list.add(entryString);
        }

        if (ObjectUtils.notEqual(list,
            this.extensionStore.getValue(object, XWikiRepositoryModel.PROP_EXTENSION_PROPERTIES))) {
            object.set(XWikiRepositoryModel.PROP_EXTENSION_PROPERTIES, list, this.xcontextProvider.get());

            return true;
        }

        return false;
    }

    protected boolean update(BaseObject object, String fieldName, Object value) throws XWikiException
    {
        // Make sure collection are lists
        if (value instanceof List list) {
            value = new ArrayList<>(list);
        }

        if (ObjectUtils.notEqual(value, this.extensionStore.getValue(object, fieldName))) {
            object.set(fieldName, value, this.xcontextProvider.get());

            return true;
        }

        return false;
    }

    private boolean updateCollection(BaseObject extensionObject, String fieldName, Collection<String> values,
        XWikiContext xcontext) throws XWikiException
    {
        boolean needSave = update(extensionObject, fieldName, values != null ? values : Collections.emptyList());

        String fieldNameEmpty = fieldName + XWikiRepositoryModel.PROPSUFFIX_EMPTYCOLLECTION;
        if (extensionObject.getXClass(xcontext).get(fieldNameEmpty) != null) {
            update(extensionObject, fieldNameEmpty, values != null && values.isEmpty() ? 1 : 0);
        }

        return needSave;
    }
}
