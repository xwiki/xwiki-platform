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

package org.xwiki.store.filesystem.internal.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.HibernateException;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.filesystem.internal.StoreFileUtils;
import org.xwiki.store.internal.FileSystemStoreUtils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-15620. Move the store to a new location. Change the path from URL encoding to hash based to
 * support long references.
 *
 * @version $Id$
 * @since 11.0
 */
@Component
@Named("R1100000XWIKI15620")
@Singleton
public class R1100000XWIKI15620DataMigration extends AbstractFileStoreDataMigration
{
    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    private ConfigurationSource configuration;

    @Inject
    private FilesystemStoreTools fstools;

    @Override
    public String getDescription()
    {
        return "Move the store to a new location."
            + " Change the path from URL encoding to hash based to support long references.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(1100000);
    }

    @Override
    public void hibernateMigrate() throws XWikiException, DataMigrationException
    {
        if (getXWikiContext().isMainWiki()) {
            // Move the whole store folder content to the new location
            File oldStore = getPre11StoreRootDirectory();

            // Check if there is a filesystem store at all
            File[] children = oldStore.listFiles();
            if (ArrayUtils.isEmpty(children)) {
                // Nothing to migrate
                return;
            }

            File newStore = this.fstools.getStoreRootDirectory();

            this.logger.info("Moving content of folder [{}] to new location [{}]", oldStore, newStore);

            for (File child : children) {
                try {
                    FileUtils.moveToDirectory(child, newStore, true);
                } catch (IOException e) {
                    throw new DataMigrationException("Failed to move old filesystem store to the new location", e);
                }
            }

            // Switch root store directory
            setStoreRootDirectory(newStore);

            // Get rid of old store
            try {
                Files.delete(oldStore.toPath());
            } catch (IOException e) {
                this.logger.warn("Failed to delete old store location [{}]", oldStore);
            }
        }

        // Rewrite store paths based on reference hash instead of URL encoding for the current wiki
        getStore().executeWrite(getXWikiContext(), session -> {
            try {
                migrate(this.fstools.getWikiDir(getXWikiContext().getWikiId()), true);
            } catch (Exception e) {
                throw new HibernateException("Failed to refactor filesystem store paths", e);
            }

            return null;
        });
    }

    private void cleanEmptyfolder(File directory)
    {
        try {
            Files.delete(directory.toPath());
        } catch (Exception e) {
            this.logger.warn("Failed to clean legacy folder [{}]: {}", directory,
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void migrate(File directory, boolean wiki) throws IOException, DataMigrationException
    {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        // Migrate directory content
        for (File child : directory.listFiles()) {
            if (child.isDirectory()) {
                if (!wiki && child.getName().equals(THIS_DIR_NAME)) {
                    migrateDocumentContent(child);
                } else {
                    migrate(child, false);
                }
            }
        }

        if (!wiki) {
            // Cleanup
            cleanEmptyfolder(directory);
        }
    }

    private void migrateDocumentContent(File oldDocumentContentDirectory) throws IOException, DataMigrationException
    {
        DocumentReference documentReference = getPre11DocumentReference(oldDocumentContentDirectory.getParentFile());

        File newDocumentContentDirectory = this.fstools.getDocumentContentDir(documentReference);

        this.logger.info("Moving document folder [{}] to new location [{}]", oldDocumentContentDirectory,
            newDocumentContentDirectory);

        FileUtils.moveDirectory(oldDocumentContentDirectory, newDocumentContentDirectory);

        migrateAttachments(newDocumentContentDirectory, documentReference);
        migrateDeletedAttachments(newDocumentContentDirectory, documentReference);
        migrateDocumentLocales(newDocumentContentDirectory);
    }

    private void migrateAttachments(File documentContentDirectory, DocumentReference documentReference)
        throws IOException
    {
        File attachmentsDirectory = new File(documentContentDirectory, FilesystemStoreTools.ATTACHMENTS_DIR_NAME);

        if (attachmentsDirectory.isDirectory()) {
            for (File oldAttachmentDirectory : attachmentsDirectory.listFiles()) {
                if (oldAttachmentDirectory.isDirectory()) {
                    AttachmentReference attachmentReference = new AttachmentReference(
                        FileSystemStoreUtils.decode(oldAttachmentDirectory.getName()), documentReference);

                    File newAttachmentDirectory = this.fstools.getAttachmentDir(attachmentReference);

                    this.logger.info("Moving attachment folder [{}] to new location [{}]", oldAttachmentDirectory,
                        newAttachmentDirectory);

                    FileUtils.moveDirectory(oldAttachmentDirectory, newAttachmentDirectory);

                    migrateAttachmentFiles(newAttachmentDirectory, attachmentReference.getName());
                }
            }
        }
    }

    private void migrateAttachmentFiles(File attachmentDirectory, String attachmentName) throws IOException
    {
        String encodedAttachmentName = FileSystemStoreUtils.encode(attachmentName, false);

        int indexOfExtension = FilenameUtils.indexOfExtension(encodedAttachmentName);
        String baseStoreAttachmentName = FilenameUtils.removeExtension(encodedAttachmentName);

        for (File file : attachmentDirectory.listFiles()) {
            if (file.getName().startsWith(baseStoreAttachmentName)) {
                String version = null;
                if (file.getName().length() > encodedAttachmentName.length()) {
                    version = file.getName().substring(baseStoreAttachmentName.length() + 2);
                    if (indexOfExtension != -1) {
                        version = version.substring(0,
                            version.length() - (encodedAttachmentName.length() - indexOfExtension));
                    }
                }

                File newAttachmentFile =
                    new File(attachmentDirectory, StoreFileUtils.getStoredFilename(attachmentName, version));

                this.logger.info("Moving attachment file [{}] to new location [{}]", file, newAttachmentFile);

                Files.move(file.toPath(), newAttachmentFile.toPath());
            }
        }
    }

    private void migrateDeletedAttachments(File documentContentDirectory, DocumentReference documentReference)
        throws IOException
    {
        File deletedAttachmentsDirectory =
            new File(documentContentDirectory, FilesystemStoreTools.DELETED_ATTACHMENTS_DIR_NAME);

        if (deletedAttachmentsDirectory.isDirectory()) {
            for (File oldDeletedAttachmentDirectory : deletedAttachmentsDirectory.listFiles()) {
                if (oldDeletedAttachmentDirectory.isDirectory()) {
                    String folderName = FileSystemStoreUtils.decode(oldDeletedAttachmentDirectory.getName());

                    // Parse <attachmentName>-id<id>
                    int index = folderName.lastIndexOf('-');
                    String attachmentName = folderName.substring(0, index);
                    long id = Long.parseLong(folderName.substring(index + 1));

                    AttachmentReference attachmentReference =
                        new AttachmentReference(attachmentName, documentReference);

                    File newDeletedAttachmentDirectory = this.fstools.getDeletedAttachmentDir(attachmentReference, id);

                    this.logger.info("Moving deleted attachment folder [{}] to new location [{}]",
                        oldDeletedAttachmentDirectory, newDeletedAttachmentDirectory);

                    FileUtils.moveDirectory(oldDeletedAttachmentDirectory, newDeletedAttachmentDirectory);

                    migrateAttachmentFiles(newDeletedAttachmentDirectory, attachmentReference.getName());
                }
            }
        }
    }

    private void migrateDocumentLocales(File documentContentDirectory) throws DataMigrationException
    {
        File localesDirectory = new File(documentContentDirectory, FilesystemStoreTools.DOCUMENT_LOCALES_DIR_NAME);

        if (localesDirectory.isDirectory()) {
            for (File localeDirectory : localesDirectory.listFiles()) {
                migrateDocumentLocale(documentContentDirectory, localeDirectory);
            }
        }
    }

    private Locale toLocale(File localeFile)
    {
        String localeString = FileSystemStoreUtils.decode(localeFile.getName());

        if (localeString.equals(DOCUMENT_LOCALE_ROOT_NAME)) {
            return Locale.ROOT;
        }

        return LocaleUtils.toLocale(localeString);
    }

    private void migrateDocumentLocale(File documentContentDirectory, File localeDirectory)
        throws DataMigrationException
    {
        File localeThisDirectory = new File(localeDirectory, THIS_DIR_NAME);

        if (localeThisDirectory.isDirectory()) {
            Locale locale = toLocale(localeDirectory);

            File targetFolder;
            if (locale.equals(Locale.ROOT)) {
                targetFolder = documentContentDirectory;
            } else {
                targetFolder = localeDirectory;
            }

            moveFolderContent(localeThisDirectory, targetFolder);

            // Cleanup
            cleanEmptyfolder(localeThisDirectory);
            if (locale.equals(Locale.ROOT)) {
                cleanEmptyfolder(localeDirectory);
            }
        }
    }
}
