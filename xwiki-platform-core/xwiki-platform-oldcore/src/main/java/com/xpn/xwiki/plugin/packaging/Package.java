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

package com.xpn.xwiki.plugin.packaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XARImportedEvent;
import com.xpn.xwiki.internal.event.XARImportingEvent;
import com.xpn.xwiki.internal.xml.XMLWriter;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

import net.sf.json.JSONObject;

public class Package
{
    public static final int OK = 0;

    public static final int Right = 1;

    public static final String DEFAULT_FILEEXT = "xml";

    public static final String XAR_FILENAME_ENCODING = "UTF-8";

    public static final String DefaultPackageFileName = "package.xml";

    public static final String DefaultPluginName = "package";

    private static final Logger LOGGER = LoggerFactory.getLogger(Package.class);

    private String name = "My package";

    private String description = "";

    /**
     * @see #isInstallExension()
     */
    private boolean installExension = true;

    private String extensionId;

    private String version = "1.0.0";

    private String licence = "LGPL";

    private String authorName = "XWiki";

    private List<DocumentInfo> files = null;

    private List<DocumentInfo> customMappingFiles = null;

    private List<DocumentInfo> classFiles = null;

    private boolean backupPack = false;

    private boolean preserveVersion = false;

    private boolean withVersions = true;

    private List<DocumentFilter> documentFilters = new ArrayList<DocumentFilter>();

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getId()
    {
        return this.extensionId;
    }

    public void setId(String id)
    {
        this.extensionId = id;
    }

    /**
     * @return <code>true</code> if the extension packaged in the XAR should be registered as such automatically,
     *         <code>false</code> otherwise.
     */
    public boolean isInstallExension()
    {
        return this.installExension;
    }

    public void setInstallExension(boolean installExension)
    {
        this.installExension = installExension;
    }

    public String getExtensionId()
    {
        return this.extensionId;
    }

    public void setExtensionId(String extensionId)
    {
        this.extensionId = extensionId;
    }

    public String getVersion()
    {
        return this.version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getLicence()
    {
        return this.licence;
    }

    public void setLicence(String licence)
    {
        this.licence = licence;
    }

    public String getAuthorName()
    {
        return this.authorName;
    }

    public void setAuthorName(String authorName)
    {
        this.authorName = authorName;
    }

    /**
     * If true, the package will preserve the original author during import, rather than updating the author to the
     * current (importing) user.
     *
     * @see #isWithVersions()
     * @see #isVersionPreserved()
     */
    public boolean isBackupPack()
    {
        return this.backupPack;
    }

    public void setBackupPack(boolean backupPack)
    {
        this.backupPack = backupPack;
    }

    public boolean hasBackupPackImportRights(XWikiContext context)
    {
        return isFarmAdmin(context);
    }

    /**
     * If true, the package will preserve the current document version during import, regardless of whether or not the
     * document history is included.
     *
     * @see #isWithVersions()
     * @see #isBackupPack()
     */
    public boolean isVersionPreserved()
    {
        return this.preserveVersion;
    }

    public void setPreserveVersion(boolean preserveVersion)
    {
        this.preserveVersion = preserveVersion;
    }

    public List<DocumentInfo> getFiles()
    {
        return this.files;
    }

    public List<DocumentInfo> getCustomMappingFiles()
    {
        return this.customMappingFiles;
    }

    public boolean isWithVersions()
    {
        return this.withVersions;
    }

    /**
     * If set to true, history revisions in the archive will be imported when importing documents.
     */
    public void setWithVersions(boolean withVersions)
    {
        this.withVersions = withVersions;
    }

    public void addDocumentFilter(Object filter) throws PackageException
    {
        if (filter instanceof DocumentFilter) {
            this.documentFilters.add((DocumentFilter) filter);
        } else {
            throw new PackageException(PackageException.ERROR_PACKAGE_INVALID_FILTER, "Invalid Document Filter");
        }
    }

    public Package()
    {
        this.files = new ArrayList<DocumentInfo>();
        this.customMappingFiles = new ArrayList<DocumentInfo>();
        this.classFiles = new ArrayList<DocumentInfo>();
    }

    public boolean add(XWikiDocument doc, int defaultAction, XWikiContext context) throws XWikiException
    {
        if (!context.getWiki().checkAccess("edit", doc, context)) {
            return false;
        }

        for (int i = 0; i < this.files.size(); i++) {
            DocumentInfo di = this.files.get(i);
            if (di.getFullName().equals(doc.getFullName()) && (di.getLanguage().equals(doc.getLanguage()))) {
                if (defaultAction != DocumentInfo.ACTION_NOT_DEFINED) {
                    di.setAction(defaultAction);
                }
                if (!doc.isNew()) {
                    di.setDoc(doc);
                }

                return true;
            }
        }

        doc = doc.clone();

        try {
            filter(doc, context);

            DocumentInfo docinfo = new DocumentInfo(doc);
            docinfo.setAction(defaultAction);
            this.files.add(docinfo);
            BaseClass bclass = doc.getXClass();
            if (bclass.getFieldList().size() > 0) {
                this.classFiles.add(docinfo);
            }
            if (bclass.getCustomMapping() != null) {
                this.customMappingFiles.add(docinfo);
            }
            return true;
        } catch (ExcludeDocumentException e) {
            LOGGER.info("Skip the document " + doc.getDocumentReference());

            return false;
        }
    }

    public boolean add(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return add(doc, DocumentInfo.ACTION_NOT_DEFINED, context);
    }

    public boolean updateDoc(String docFullName, int action, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(docFullName, context);
        return add(doc, action, context);
    }

    public boolean add(String docFullName, int DefaultAction, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = context.getWiki().getDocument(docFullName, context);
        add(doc, DefaultAction, context);
        List<String> languages = doc.getTranslationList(context);
        for (String language : languages) {
            if (!((language == null) || (language.equals("")) || (language.equals(doc.getDefaultLanguage())))) {
                add(doc.getTranslatedDocument(language, context), DefaultAction, context);
            }
        }

        return true;
    }

    public boolean add(String docFullName, String language, int DefaultAction, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument doc = context.getWiki().getDocument(docFullName, context);
        if ((language == null) || (language.equals(""))) {
            add(doc, DefaultAction, context);
        } else {
            add(doc.getTranslatedDocument(language, context), DefaultAction, context);
        }

        return true;
    }

    public boolean add(String docFullName, XWikiContext context) throws XWikiException
    {
        return add(docFullName, DocumentInfo.ACTION_NOT_DEFINED, context);
    }

    public boolean add(String docFullName, String language, XWikiContext context) throws XWikiException
    {
        return add(docFullName, language, DocumentInfo.ACTION_NOT_DEFINED, context);
    }

    public void filter(XWikiDocument doc, XWikiContext context) throws ExcludeDocumentException
    {
        for (DocumentFilter docFilter : this.documentFilters) {
            docFilter.filter(doc, context);
        }
    }

    public String export(OutputStream os, XWikiContext context) throws IOException, XWikiException
    {
        if (this.files.size() == 0) {
            return "No Selected file";
        }

        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(os);
        zos.setEncoding(XAR_FILENAME_ENCODING);
        // By including the unicode extra fields, it is possible to extract XAR-files
        // containing documents with non-ascii characters in the document name using InfoZIP,
        // and the filenames will be correctly converted to the character set of the local
        // file system.
        zos.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);
        for (int i = 0; i < this.files.size(); i++) {
            DocumentInfo docinfo = this.files.get(i);
            XWikiDocument doc = docinfo.getDoc();
            addToZip(doc, zos, this.withVersions, context);
        }
        addInfosToZip(zos, context);
        zos.finish();
        zos.flush();

        return "";
    }

    public String exportToDir(File dir, XWikiContext context) throws IOException, XWikiException
    {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Object[] args = new Object[1];
                args[0] = dir.toString();
                throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_MKDIR,
                    "Error creating directory {0}", null, args);
            }
        }

        for (int i = 0; i < this.files.size(); i++) {
            DocumentInfo docinfo = this.files.get(i);
            XWikiDocument doc = docinfo.getDoc();
            addToDir(doc, dir, this.withVersions, context);
        }
        addInfosToDir(dir, context);

        return "";
    }

    /**
     * Load this package in memory from a byte array. It may be installed later using {@link #install(XWikiContext)}.
     * Your should prefer {@link #Import(InputStream, XWikiContext)} which may avoid loading the package twice in
     * memory.
     *
     * @param file a byte array containing the content of a zipped package file
     * @param context current XWikiContext
     * @return an empty string, useless.
     * @throws IOException while reading the ZipFile
     * @throws XWikiException when package content is broken
     */
    public String Import(byte file[], XWikiContext context) throws IOException, XWikiException
    {
        return Import(new ByteArrayInputStream(file), context);
    }

    /**
     * Load this package in memory from an InputStream. It may be installed later using {@link #install(XWikiContext)}.
     *
     * @param file an InputStream of a zipped package file
     * @param context current XWikiContext
     * @return an empty string, useless.
     * @throws IOException while reading the ZipFile
     * @throws XWikiException when package content is broken
     * @since 2.3M2
     */
    public String Import(InputStream file, XWikiContext context) throws IOException, XWikiException
    {
        ZipArchiveInputStream zis;
        ArchiveEntry entry;
        Document description = null;

        try {
            zis = new ZipArchiveInputStream(file, XAR_FILENAME_ENCODING, false);

            List<XWikiDocument> docsToLoad = new LinkedList<XWikiDocument>();
            /*
             * Loop 1: Cycle through the zip input stream and load out all of the documents, when we find the
             * package.xml file we put it aside to so that we only include documents which are in the file.
             */
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || (entry.getName().indexOf("META-INF") != -1)) {
                    // The entry is either a directory or is something inside of the META-INF dir.
                    // (we use that directory to put meta data such as LICENSE/NOTICE files.)
                    continue;
                } else if (entry.getName().compareTo(DefaultPackageFileName) == 0) {
                    // The entry is the manifest (package.xml). Read this differently.
                    description = fromXml(new CloseShieldInputStream(zis));
                } else {
                    XWikiDocument doc = null;
                    try {
                        doc = readFromXML(new CloseShieldInputStream(zis));
                    } catch (Throwable ex) {
                        LOGGER.warn("Failed to parse document [" + entry.getName()
                            + "] from XML during import, thus it will not be installed. " + "The error was: "
                            + ex.getMessage());
                        // It will be listed in the "failed documents" section after the import.
                        addToErrors(entry.getName().replaceAll("/", "."), context);

                        continue;
                    }

                    // Run all of the registered DocumentFilters on this document and
                    // if no filters throw exceptions, add it to the list to import.
                    try {
                        this.filter(doc, context);
                        docsToLoad.add(doc);
                    } catch (ExcludeDocumentException e) {
                        LOGGER.info("Skip the document '" + doc.getDocumentReference() + "'");
                    }
                }
            }
            // Make sure a manifest was included in the package...
            if (description == null) {
                throw new PackageException(XWikiException.ERROR_XWIKI_UNKNOWN, "Could not find the package definition");
            }
            /*
             * Loop 2: Cycle through the list of documents and if they are in the manifest then add them, otherwise log
             * a warning and add them to the skipped list.
             */
            for (XWikiDocument doc : docsToLoad) {
                if (documentExistInPackageFile(doc.getFullName(), doc.getLanguage(), description)) {
                    this.add(doc, context);
                } else {
                    LOGGER.warn("document " + doc.getDocumentReference() + " does not exist in package definition."
                        + " It will not be installed.");
                    // It will be listed in the "skipped documents" section after the
                    // import.
                    addToSkipped(doc.getFullName(), context);
                }
            }

            updateFileInfos(description);
        } catch (DocumentException e) {
            throw new PackageException(XWikiException.ERROR_XWIKI_UNKNOWN, "Error when reading the XML");
        }

        return "";
    }

    private boolean documentExistInPackageFile(String docName, String language, Document xml)
    {
        Element docFiles = xml.getRootElement();
        Element infosFiles = docFiles.element("files");

        @SuppressWarnings("unchecked")
        List<Element> fileList = infosFiles.elements("file");

        for (Element el : fileList) {
            String tmpDocName = el.getStringValue();
            if (tmpDocName.compareTo(docName) != 0) {
                continue;
            }
            String tmpLanguage = el.attributeValue("language");
            if (tmpLanguage == null) {
                tmpLanguage = "";
            }
            if (tmpLanguage.compareTo(language) == 0) {
                return true;
            }
        }

        return false;
    }

    private void updateFileInfos(Document xml)
    {
        Element docFiles = xml.getRootElement();
        Element infosFiles = docFiles.element("files");

        @SuppressWarnings("unchecked")
        List<Element> fileList = infosFiles.elements("file");
        for (Element el : fileList) {
            String defaultAction = el.attributeValue("defaultAction");
            String language = el.attributeValue("language");
            if (language == null) {
                language = "";
            }
            String docName = el.getStringValue();
            setDocumentDefaultAction(docName, language, Integer.parseInt(defaultAction));
        }
    }

    private void setDocumentDefaultAction(String docName, String language, int defaultAction)
    {
        if (this.files == null) {
            return;
        }

        for (DocumentInfo docInfo : this.files) {
            if (docInfo.getFullName().equals(docName) && docInfo.getLanguage().equals(language)) {
                docInfo.setAction(defaultAction);
                return;
            }
        }
    }

    public int testInstall(boolean isAdmin, XWikiContext context)
    {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Package test install");
        }

        int result = DocumentInfo.INSTALL_IMPOSSIBLE;
        try {
            if (this.files.size() == 0) {
                return result;
            }

            result = this.files.get(0).testInstall(isAdmin, context);
            for (DocumentInfo docInfo : this.files) {
                int res = docInfo.testInstall(isAdmin, context);
                if (res < result) {
                    result = res;
                }
            }

            return result;
        } finally {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Package test install result " + result);
            }
        }
    }

    public int install(XWikiContext context) throws XWikiException
    {
        boolean isAdmin = context.getWiki().getRightService().hasWikiAdminRights(context);

        if (testInstall(isAdmin, context) == DocumentInfo.INSTALL_IMPOSSIBLE) {
            setStatus(DocumentInfo.INSTALL_IMPOSSIBLE, context);
            return DocumentInfo.INSTALL_IMPOSSIBLE;
        }

        boolean hasCustomMappings = false;
        for (DocumentInfo docinfo : this.customMappingFiles) {
            BaseClass bclass = docinfo.getDoc().getXClass();
            hasCustomMappings |= context.getWiki().getStore().injectCustomMapping(bclass, context);
        }

        if (hasCustomMappings) {
            context.getWiki().getStore().injectUpdatedCustomMappings(context);
        }

        int status = DocumentInfo.INSTALL_OK;

        // Determine if the user performing the installation is a farm admin.
        // We allow author preservation from the package only to farm admins.
        // In order to prevent sub-wiki admins to take control of a farm with forged packages.
        // We test it once for the whole import in case one of the document break user during the import process.
        boolean backup = this.backupPack && isFarmAdmin(context);

        // Notify all the listeners about import
        ObservationManager om = Utils.getComponent(ObservationManager.class);

        // FIXME: should be able to pass some sort of source here, the name of the attachment or the list of
        // imported documents. But for the moment it's fine
        om.notify(new XARImportingEvent(), null, context);

        try {
            // Start by installing all documents having a class definition so that their
            // definitions are available when installing documents using them.
            for (DocumentInfo classFile : this.classFiles) {
                if (installDocument(classFile, isAdmin, backup, context) == DocumentInfo.INSTALL_ERROR) {
                    status = DocumentInfo.INSTALL_ERROR;
                }
            }

            // Install the remaining documents (without class definitions).
            for (DocumentInfo docInfo : this.files) {
                if (!this.classFiles.contains(docInfo)) {
                    if (installDocument(docInfo, isAdmin, backup, context) == DocumentInfo.INSTALL_ERROR) {
                        status = DocumentInfo.INSTALL_ERROR;
                    }
                }
            }
            setStatus(status, context);

        } finally {
            // FIXME: should be able to pass some sort of source here, the name of the attachment or the list of
            // imported documents. But for the moment it's fine
            om.notify(new XARImportedEvent(), null, context);

            registerExtension(context);
        }

        return status;
    }

    private void registerExtension(XWikiContext context)
    {
        // Register the package as extension if it's one
        if (isInstallExension() && StringUtils.isNotEmpty(getExtensionId()) && StringUtils.isNotEmpty(getVersion())) {
            ExtensionId extensionId = new ExtensionId(getExtensionId(), getVersion());

            try {
                LocalExtensionRepository localRepository = Utils.getComponent(LocalExtensionRepository.class);

                LocalExtension localExtension = localRepository.getLocalExtension(extensionId);
                if (localExtension == null) {
                    Extension extension;
                    try {
                        // Try to find and download the extension from a repository
                        extension = Utils.getComponent(ExtensionRepositoryManager.class).resolve(extensionId);
                    } catch (ResolveException e) {
                        LOGGER.debug("Can't find extension [{}]", extensionId, e);

                        // FIXME: Create a dummy extension. Need support for partial/lazy extension.
                        return;
                    }

                    localExtension = localRepository.storeExtension(extension);
                }

                InstalledExtensionRepository installedRepository =
                    Utils.getComponent(InstalledExtensionRepository.class);

                String namespace = "wiki:" + context.getWikiId();

                // Make sure it's not already there
                if (installedRepository.getInstalledExtension(localExtension.getId().getId(), namespace) == null) {
                    for (String feature : localExtension.getFeatures()) {
                        if (installedRepository.getInstalledExtension(feature, namespace) != null) {
                            // Already exist so don't register it or it could create a mess
                            return;
                        }
                    }
                } else {
                    return;
                }

                // Register the extension as installed
                installedRepository.installExtension(localExtension, namespace, false);
            } catch (Exception e) {
                LOGGER.error("Failed to register extenion [{}] from the XAR", extensionId, e);
            }
        }
    }

    /**
     * Indicate of the user has amin rights on the farm, i.e. that he has admin rights on the main wiki.
     *
     * @param context the XWiki context
     * @return true if the current user is farm admin
     */
    private boolean isFarmAdmin(XWikiContext context)
    {
        String wiki = context.getWikiId();

        try {
            context.setWikiId(context.getMainXWiki());

            return context.getWiki().getRightService().hasWikiAdminRights(context);
        } finally {
            context.setWikiId(wiki);
        }
    }

    private int installDocument(DocumentInfo doc, boolean isAdmin, boolean backup, XWikiContext context)
        throws XWikiException
    {
        if (this.preserveVersion && this.withVersions) {
            // Right now importing an archive and the history revisions it contains
            // without overriding the existing document is not supported.
            // We fallback on adding a new version to the existing history without importing the
            // archive's revisions.
            this.withVersions = false;
        }

        int result = DocumentInfo.INSTALL_OK;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Package installing document " + doc.getFullName() + " " + doc.getLanguage());
        }

        if (doc.getAction() == DocumentInfo.ACTION_SKIP) {
            addToSkipped(doc.getFullName() + ":" + doc.getLanguage(), context);
            return DocumentInfo.INSTALL_OK;
        }

        int status = doc.testInstall(isAdmin, context);
        if (status == DocumentInfo.INSTALL_IMPOSSIBLE) {
            addToErrors(doc.getFullName() + ":" + doc.getLanguage(), context);
            return DocumentInfo.INSTALL_IMPOSSIBLE;
        }
        if (status == DocumentInfo.INSTALL_OK || status == DocumentInfo.INSTALL_ALREADY_EXIST
            && doc.getAction() == DocumentInfo.ACTION_OVERWRITE) {
            XWikiDocument previousdoc = null;
            if (status == DocumentInfo.INSTALL_ALREADY_EXIST) {
                previousdoc = context.getWiki().getDocument(doc.getFullName(), context);
                // if this document is a translation: we should only delete the translation
                if (doc.getDoc().getTranslation() != 0) {
                    previousdoc = previousdoc.getTranslatedDocument(doc.getLanguage(), context);
                }
                // we should only delete the previous document
                // if we are overridding the versions and/or if this is a backup pack
                if (!this.preserveVersion || this.withVersions) {
                    try {
                        // This is not a real document delete, it's a upgrade. To be sure to not
                        // generate DELETE notification we directly use {@link XWikiStoreInterface}
                        context.getWiki().getStore().deleteXWikiDoc(previousdoc, context);
                    } catch (Exception e) {
                        // let's log the error but not stop
                        result = DocumentInfo.INSTALL_ERROR;
                        addToErrors(doc.getFullName() + ":" + doc.getLanguage(), context);
                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.error("Failed to delete document " + previousdoc.getDocumentReference());
                        }
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Failed to delete document " + previousdoc.getDocumentReference(), e);
                        }
                    }
                }
                else if (previousdoc.hasElement(XWikiDocument.HAS_ATTACHMENTS))
                {
                    // We conserve the old attachments in the new documents
                    List<XWikiAttachment> newDocAttachments = doc.getDoc().getAttachmentList();
                    for (XWikiAttachment att : previousdoc.getAttachmentList())
                    {
                        if (doc.getDoc().getAttachment(att.getFilename()) == null)
                        {
                            // We add the attachment to new document
                            newDocAttachments.add(att);
                            // But then we add it in the "to remove list" of the document
                            // So the attachment will be removed from the database when XWiki#saveDocument
                            // will be called
                            doc.getDoc().removeAttachment(att);
                        }
                    }
                }
                doc.getDoc().addXObjectsToRemoveFromVersion(previousdoc);
                doc.getDoc().setOriginalDocument(previousdoc);
            }
            try {
                if (!backup) {
                    doc.getDoc().setAuthorReference(context.getUserReference());
                    doc.getDoc().setContentAuthorReference(context.getUserReference());
                    // if the import is not a backup pack we set the date to now
                    Date date = new Date();
                    doc.getDoc().setDate(date);
                    doc.getDoc().setContentUpdateDate(date);
                }

                if (!this.withVersions) {
                    doc.getDoc().setVersion("1.1");
                }

                // Does the document to be imported already exists in the wiki ?
                boolean isNewDocument = previousdoc == null;

                // Conserve existing history only if asked for it and if this history exists
                boolean conserveExistingHistory = this.preserveVersion && !isNewDocument;

                // Does the document from the package contains history revisions ?
                boolean packageHasHistory = this.documentContainsHistory(doc);

                // Reset to initial (1.1) version when we don't want to conserve existing history and either we don't
                // want the package history or this latter one is empty
                boolean shouldResetToInitialVersion =
                    !conserveExistingHistory && (!this.withVersions || !packageHasHistory);

                if (conserveExistingHistory) {
                    // Insert the archive from the existing document
                    doc.getDoc().setDocumentArchive(previousdoc.getDocumentArchive(context));
                }

                else {
                    // Reset or replace history
                    // if there was not history in the source package then we should reset the version number to 1.1
                    if (shouldResetToInitialVersion) {
                        // Make sure the save will not increment the version to 2.1
                        doc.getDoc().setContentDirty(false);
                        doc.getDoc().setMetaDataDirty(false);
                    }
                }

                String saveMessage = context.getMessageTool().get("core.importer.saveDocumentComment");
                context.getWiki().saveDocument(doc.getDoc(), saveMessage, context);
                addToInstalled(doc.getFullName() + ":" + doc.getLanguage(), context);

                if ((this.withVersions && packageHasHistory) || conserveExistingHistory) {
                    // we need to force the saving the document archive.
                    if (doc.getDoc().getDocumentArchive() != null) {
                        context.getWiki().getVersioningStore()
                            .saveXWikiDocArchive(doc.getDoc().getDocumentArchive(context), true, context);
                    }
                }

                if (shouldResetToInitialVersion) {
                    // If we override and do not import version, (meaning reset document to 1.1)
                    // We need manually reset possible existing revision for the document
                    // This means making the history empty (it does not affect the version number)
                    doc.getDoc().resetArchive(context);
                }

            } catch (XWikiException e) {
                addToErrors(doc.getFullName() + ":" + doc.getLanguage(), context);
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Failed to save document " + doc.getFullName(), e);
                }
                result = DocumentInfo.INSTALL_ERROR;
            }
        }
        return result;
    }

    /**
     * @return true if the passed document contains a (not-empty) history of previous versions, false otherwise
     */
    private boolean documentContainsHistory(DocumentInfo doc)
    {
        if ((doc.getDoc().getDocumentArchive() == null) || (doc.getDoc().getDocumentArchive().getNodes() == null)
            || (doc.getDoc().getDocumentArchive().getNodes().size() == 0)) {
            return false;
        }
        return true;
    }

    private List<String> getStringList(String name, XWikiContext context)
    {
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) context.get(name);

        if (list == null) {
            list = new ArrayList<String>();
            context.put(name, list);
        }

        return list;
    }

    private void addToErrors(String fullName, XWikiContext context)
    {
        if (fullName.endsWith(":")) {
            fullName = fullName.substring(0, fullName.length() - 1);
        }

        getErrors(context).add(fullName);
    }

    private void addToSkipped(String fullName, XWikiContext context)
    {
        if (fullName.endsWith(":")) {
            fullName = fullName.substring(0, fullName.length() - 1);
        }

        getSkipped(context).add(fullName);
    }

    private void addToInstalled(String fullName, XWikiContext context)
    {
        if (fullName.endsWith(":")) {
            fullName = fullName.substring(0, fullName.length() - 1);
        }

        getInstalled(context).add(fullName);
    }

    private void setStatus(int status, XWikiContext context)
    {
        context.put("install_status", new Integer((status)));
    }

    public List<String> getErrors(XWikiContext context)
    {
        return getStringList("install_errors", context);
    }

    public List<String> getSkipped(XWikiContext context)
    {
        return getStringList("install_skipped", context);
    }

    public List<String> getInstalled(XWikiContext context)
    {
        return getStringList("install_installed", context);
    }

    public int getStatus(XWikiContext context)
    {
        Integer status = (Integer) context.get("install_status");
        if (status == null) {
            return -1;
        } else {
            return status.intValue();
        }
    }

    /**
     * Create a {@link XWikiDocument} from xml stream.
     *
     * @param is the xml stream.
     * @return the {@link XWikiDocument}.
     * @throws XWikiException error when creating the {@link XWikiDocument}.
     */
    private XWikiDocument readFromXML(InputStream is) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();

        doc.fromXML(is, this.withVersions);

        return doc;
    }

    /**
     * Create a {@link XWikiDocument} from xml {@link Document}.
     *
     * @param domDoc the xml {@link Document}.
     * @return the {@link XWikiDocument}.
     * @throws XWikiException error when creating the {@link XWikiDocument}.
     */
    private XWikiDocument readFromXML(Document domDoc) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();

        doc.fromXML(domDoc, this.withVersions);

        return doc;
    }

    /**
     * You should prefer {@link #toXML(com.xpn.xwiki.internal.xml.XMLWriter)}. If an error occurs, a stacktrace is dump
     * to logs, and an empty String is returned.
     *
     * @return a package.xml file for the this package
     */
    public String toXml(XWikiContext context)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            toXML(baos, context);
            return baos.toString(context.getWiki().getEncoding());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Write the package.xml file to an {@link XMLWriter}.
     *
     * @param wr the writer to write to
     * @throws IOException when an error occurs during streaming operation
     * @since 2.3M2
     */
    public void toXML(XMLWriter wr) throws IOException
    {
        Element docel = new DOMElement("package");
        wr.writeOpen(docel);

        Element elInfos = new DOMElement("infos");
        wr.write(elInfos);

        Element el = new DOMElement("name");
        el.addText(this.name);
        wr.write(el);

        el = new DOMElement("description");
        el.addText(this.description);
        wr.write(el);

        el = new DOMElement("licence");
        el.addText(this.licence);
        wr.write(el);

        el = new DOMElement("author");
        el.addText(this.authorName);
        wr.write(el);

        el = new DOMElement("version");
        el.addText(this.version);
        wr.write(el);

        el = new DOMElement("backupPack");
        el.addText(new Boolean(this.backupPack).toString());
        wr.write(el);

        el = new DOMElement("preserveVersion");
        el.addText(new Boolean(this.preserveVersion).toString());
        wr.write(el);

        Element elfiles = new DOMElement("files");
        wr.writeOpen(elfiles);

        for (DocumentInfo docInfo : this.files) {
            Element elfile = new DOMElement("file");
            elfile.addAttribute("defaultAction", String.valueOf(docInfo.getAction()));
            elfile.addAttribute("language", String.valueOf(docInfo.getLanguage()));
            elfile.addText(docInfo.getFullName());
            wr.write(elfile);
        }
    }

    /**
     * Write the package.xml file to an OutputStream
     *
     * @param out the OutputStream to write to
     * @param context curent XWikiContext
     * @throws IOException when an error occurs during streaming operation
     * @since 2.3M2
     */
    public void toXML(OutputStream out, XWikiContext context) throws IOException
    {
        XMLWriter wr = new XMLWriter(out, new OutputFormat("", true, context.getWiki().getEncoding()));

        Document doc = new DOMDocument();
        wr.writeDocumentStart(doc);
        toXML(wr);
        wr.writeDocumentEnd(doc);
    }

    /**
     * Write the package.xml file to a ZipOutputStream
     *
     * @param zos the ZipOutputStream to write to
     * @param context current XWikiContext
     */
    private void addInfosToZip(ZipArchiveOutputStream zos, XWikiContext context)
    {
        try {
            String zipname = DefaultPackageFileName;
            ZipArchiveEntry zipentry = new ZipArchiveEntry(zipname);
            zos.putArchiveEntry(zipentry);
            toXML(zos, context);
            zos.closeArchiveEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a relative path based on provided document.
     *
     * @param doc the document to export.
     * @return the corresponding path.
     */
    public String getPathFromDocument(XWikiDocument doc, XWikiContext context)
    {
        return getDirectoryForDocument(doc) + getFileNameFromDocument(doc, context);
    }

    /**
     * Generate a file name based on provided document.
     *
     * @param doc the document to export.
     * @return the corresponding file name.
     */
    public String getFileNameFromDocument(XWikiDocument doc, XWikiContext context)
    {
        StringBuilder fileName = new StringBuilder(doc.getDocumentReference().getName());

        // Add language
        String language = doc.getLanguage();
        if ((language != null) && (!language.equals(""))) {
            fileName.append(".");
            fileName.append(language);
        }

        // Add extension
        fileName.append('.').append(DEFAULT_FILEEXT);

        return fileName.toString();
    }

    /**
     * Generate a relative path based on provided document for the directory where the document should be stored.
     *
     * @param doc the document to export
     * @return the corresponding path
     */
    public String getDirectoryForDocument(XWikiDocument doc)
    {
        StringBuilder path = new StringBuilder();
        for (SpaceReference space : doc.getDocumentReference().getSpaceReferences()) {
            path.append(space.getName()).append('/');
        }
        return path.toString();
    }

    /**
     * Write an XML serialized XWikiDocument to a ZipOutputStream
     *
     * @param doc the document to serialize
     * @param zos the ZipOutputStream to write to
     * @param withVersions if true, also serialize all document versions
     * @param context current XWikiContext
     * @throws XWikiException when an error occurs during documents access
     * @throws IOException when an error occurs during streaming operation
     * @deprecated since 4.1M2
     */
    @Deprecated
    public void addToZip(XWikiDocument doc, ZipOutputStream zos, boolean withVersions, XWikiContext context)
        throws XWikiException, IOException
    {
        String zipname = getPathFromDocument(doc, context);
        ZipEntry zipentry = new ZipEntry(zipname);
        zos.putNextEntry(zipentry);
        doc.toXML(zos, true, false, true, withVersions, context);
        zos.closeEntry();
    }

    /**
     * Write an XML serialized XWikiDocument to a ZipOutputStream
     *
     * @param doc the document to serialize
     * @param zos the ZipOutputStream to write to
     * @param withVersions if true, also serialize all document versions
     * @param context current XWikiContext
     * @throws XWikiException when an error occurs during documents access
     * @throws IOException when an error occurs during streaming operation
     * @since 4.1M2
     */
    private void addToZip(XWikiDocument doc, ZipArchiveOutputStream zos, boolean withVersions, XWikiContext context)
        throws XWikiException, IOException
    {
        String zipname = getPathFromDocument(doc, context);
        ZipArchiveEntry zipentry = new ZipArchiveEntry(zipname);
        zos.putArchiveEntry(zipentry);
        doc.toXML(zos, true, false, true, withVersions, context);
        zos.closeArchiveEntry();
    }

    public void addToDir(XWikiDocument doc, File dir, boolean withVersions, XWikiContext context) throws XWikiException
    {
        try {
            filter(doc, context);
            File spacedir = new File(dir, getDirectoryForDocument(doc));
            if (!spacedir.exists()) {
                if (!spacedir.mkdirs()) {
                    Object[] args = new Object[1];
                    args[0] = dir.toString();
                    throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_MKDIR,
                        "Error creating directory {0}", null, args);
                }
            }
            String filename = getFileNameFromDocument(doc, context);
            File file = new File(spacedir, filename);
            FileOutputStream fos = new FileOutputStream(file);
            doc.toXML(fos, true, false, true, withVersions, context);
            fos.flush();
            fos.close();
        } catch (ExcludeDocumentException e) {
            LOGGER.info("Skip the document " + doc.getDocumentReference());
        } catch (Exception e) {
            Object[] args = new Object[1];
            args[0] = doc.getDocumentReference();

            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_XWIKI_DOC_EXPORT,
                "Error creating file {0}", e, args);
        }
    }

    private void addInfosToDir(File dir, XWikiContext context)
    {
        try {
            String filename = DefaultPackageFileName;
            File file = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(file);
            toXML(fos, context);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String getElementText(Element docel, String name)
    {
        return getElementText(docel, name, "");
    }

    protected String getElementText(Element docel, String name, String def)
    {
        Element el = docel.element(name);
        if (el == null) {
            return def;
        } else {
            return el.getText();
        }
    }

    protected Document fromXml(InputStream xml) throws DocumentException
    {
        SAXReader reader = new SAXReader();
        Document domdoc = reader.read(xml);

        Element docEl = domdoc.getRootElement();

        Element infosEl = docEl.element("infos");

        this.name = getElementText(infosEl, "name");
        this.description = getElementText(infosEl, "description");
        this.licence = getElementText(infosEl, "licence");
        this.authorName = getElementText(infosEl, "author");
        this.extensionId = getElementText(infosEl, "extensionId", null);
        this.version = getElementText(infosEl, "version");
        this.backupPack = new Boolean(getElementText(infosEl, "backupPack")).booleanValue();
        this.preserveVersion = new Boolean(getElementText(infosEl, "preserveVersion")).booleanValue();

        return domdoc;
    }

    public void addAllWikiDocuments(XWikiContext context) throws XWikiException
    {
        XWiki wiki = context.getWiki();
        try {
            List<String> documentNames = wiki.getStore().getQueryManager().getNamedQuery("getAllDocuments").execute();
            for (String docName : documentNames) {
                add(docName, DocumentInfo.ACTION_OVERWRITE, context);
            }
        } catch (QueryException ex) {
            throw new PackageException(XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                "Cannot retrieve the list of documents to export", ex);
        }
    }

    public void deleteAllWikiDocuments(XWikiContext context) throws XWikiException
    {
        XWiki wiki = context.getWiki();
        List<String> spaces = wiki.getSpaces(context);
        for (int i = 0; i < spaces.size(); i++) {
            List<String> docNameList = wiki.getSpaceDocsName(spaces.get(i), context);
            for (String docName : docNameList) {
                String docFullName = spaces.get(i) + "." + docName;
                XWikiDocument doc = wiki.getDocument(docFullName, context);
                wiki.deleteAllDocuments(doc, context);
            }
        }
    }

    /**
     * Load document files from provided directory and sub-directories into packager.
     *
     * @param dir the directory from where to load documents.
     * @param context the XWiki context.
     * @param description the package descriptor.
     * @return the number of loaded documents.
     * @throws IOException error when loading documents.
     * @throws XWikiException error when loading documents.
     */
    public int readFromDir(File dir, XWikiContext context, Document description) throws IOException, XWikiException
    {
        File[] files = dir.listFiles();

        SAXReader reader = new SAXReader();

        int count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                count += readFromDir(file, context, description);
            } else {
                boolean validWikiDoc = false;
                Document domdoc = null;

                try {
                    domdoc = reader.read(new FileInputStream(file));
                    validWikiDoc = XWikiDocument.containsXMLWikiDocument(domdoc);
                } catch (DocumentException e1) {
                }

                if (validWikiDoc) {
                    XWikiDocument doc = readFromXML(domdoc);

                    try {
                        filter(doc, context);

                        if (documentExistInPackageFile(doc.getFullName(), doc.getLanguage(), description)) {
                            add(doc, context);

                            ++count;
                        } else {
                            throw new PackageException(XWikiException.ERROR_XWIKI_UNKNOWN, "document "
                                + doc.getDocumentReference() + " does not exist in package definition");
                        }
                    } catch (ExcludeDocumentException e) {
                        LOGGER.info("Skip the document '" + doc.getDocumentReference() + "'");
                    }
                } else if (!file.getName().equals(DefaultPackageFileName)) {
                    LOGGER.info(file.getAbsolutePath() + " is not a valid wiki document");
                }
            }
        }

        return count;
    }

    /**
     * Load document files from provided directory and sub-directories into packager.
     *
     * @param dir the directory from where to load documents.
     * @param context the XWiki context.
     * @return
     * @throws IOException error when loading documents.
     * @throws XWikiException error when loading documents.
     */
    public String readFromDir(File dir, XWikiContext context) throws IOException, XWikiException
    {
        if (!dir.isDirectory()) {
            throw new PackageException(PackageException.ERROR_PACKAGE_UNKNOWN, dir.getAbsolutePath()
                + " is not a directory");
        }

        int count = 0;
        try {
            File infofile = new File(dir, DefaultPackageFileName);
            Document description = fromXml(new FileInputStream(infofile));

            count = readFromDir(dir, context, description);

            updateFileInfos(description);
        } catch (DocumentException e) {
            throw new PackageException(PackageException.ERROR_PACKAGE_UNKNOWN, "Error when reading the XML");
        }

        LOGGER.info("Package read " + count + " documents");

        return "";
    }

    /**
     * Outputs the content of this package in the JSON format
     *
     * @param wikiContext the XWiki context
     * @return a representation of this package under the JSON format
     * @since 2.2M1
     */
    public JSONObject toJSON(XWikiContext wikiContext)
    {
        Map<String, Object> json = new HashMap<String, Object>();

        Map<String, Object> infos = new HashMap<String, Object>();
        infos.put("name", this.name);
        infos.put("description", this.description);
        infos.put("licence", this.licence);
        infos.put("author", this.authorName);
        infos.put("version", this.version);
        infos.put("backup", this.isBackupPack());

        Map<String, Map<String, List<Map<String, String>>>> files =
            new HashMap<String, Map<String, List<Map<String, String>>>>();

        for (DocumentInfo docInfo : this.files) {
            Map<String, String> fileInfos = new HashMap<String, String>();
            fileInfos.put("defaultAction", String.valueOf(docInfo.getAction()));
            fileInfos.put("language", String.valueOf(docInfo.getLanguage()));
            fileInfos.put("fullName", docInfo.getFullName());

            // If the space does not exist in the map of spaces, we create it.
            if (files.get(docInfo.getDoc().getSpace()) == null) {
                files.put(docInfo.getDoc().getSpace(), new HashMap<String, List<Map<String, String>>>());
            }

            // If the document name does not exists in the space map of docs, we create it.
            if (files.get(docInfo.getDoc().getSpace()).get(docInfo.getDoc().getName()) == null) {
                files.get(docInfo.getDoc().getSpace()).put(docInfo.getDoc().getName(),
                    new ArrayList<Map<String, String>>());
            }

            // Finally we add the file infos (language, fullname and action) to the list of translations
            // for that document.
            files.get(docInfo.getDoc().getSpace()).get(docInfo.getDoc().getName()).add(fileInfos);
        }

        json.put("infos", infos);
        json.put("files", files);

        JSONObject jsonObject = JSONObject.fromObject(json);

        return jsonObject;
    }
}
