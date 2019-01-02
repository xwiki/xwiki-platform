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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.internal.FileSystemStoreUtils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for XWIKI-14758. Change the path syntax to support case insensitive filesystems.
 *
 * @version $Id$
 * @since 10.1RC1
 */
@Component
@Named("R1001000XWIKI14758")
@Singleton
public class R1001000XWIKI14758DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    private ConfigurationSource configuration;

    @Inject
    private FilesystemStoreTools fstools;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Change the path syntax to support case insensitive filesystems.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(1001000);
    }

    @Override
    public void hibernateMigrate() throws XWikiException, DataMigrationException
    {
        // Move back metadata of deleted attachments located in the filesystem store
        getStore().executeWrite(getXWikiContext(), session -> {
            try {
                migrateDocument(this.fstools.getWikiDir(getXWikiContext().getWikiId()));
            } catch (Exception e) {
                throw new HibernateException("Failed to refactor filesystem store paths", e);
            }

            return null;
        });
    }

    private void migrateDocument(File directory) throws IOException
    {
        if (!directory.exists()) {
            return;
        }

        // Migrate document/space directory
        File newDirectory = migrate(directory, true);

        // Migrate document children
        if (newDirectory.isDirectory()) {
            for (File child : newDirectory.listFiles()) {
                if (child.isDirectory()) {
                    if (child.getName().equals(FilesystemStoreTools.DOCUMENT_DIR_NAME)) {
                        // Migrate content of the document
                        migrateThis(child);
                    } else {
                        // Migrate children
                        migrateDocument(child);
                    }
                } else {
                    migrateWithChildren(child);
                }
            }
        }
    }

    private File encode(File file, boolean caseInsensitive) throws UnsupportedEncodingException
    {
        String name = file.getName();

        String encodedName = FileSystemStoreUtils.encode(decode(name), caseInsensitive);

        return new File(file.getParentFile(), encodedName);
    }

    private String decode(String name) throws UnsupportedEncodingException
    {
        return URLDecoder.decode(name, StandardCharsets.UTF_8.name());
    }

    private void migrateThis(File directory) throws IOException
    {
        for (File child : directory.listFiles()) {
            if (child.getName().equals(FilesystemStoreTools.ATTACHMENT_DIR_NAME)) {
                // Migrate attachments
                migrateAttachments(child);
            } else {
                migrateWithChildren(child);
            }
        }
    }

    private void migrateAttachments(File directory) throws IOException
    {
        for (File file : directory.listFiles()) {
            // Migrate attachment directory
            File newFile = migrate(file, true);

            // Migrate children
            migrateChildren(newFile);
        }
    }

    private void migrateWithChildren(File file) throws IOException
    {
        // Migrate file
        File newFile = migrate(file, false);

        // Migrate children (if any)
        migrateChildren(newFile);
    }

    private void migrateChildren(File file) throws IOException
    {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                migrateWithChildren(child);
            }
        }
    }

    private File migrate(File file, boolean caseInsensitive) throws IOException
    {
        File newFile = encode(file, caseInsensitive);

        if (!newFile.equals(file)) {
            this.logger.info("Renaming folder [{}] into [{}]", file, newFile);

            // Rename the file if needed
            if (file.isDirectory()) {
                FileUtils.moveDirectory(file, newFile);
            } else {
                FileUtils.moveFile(file, newFile);
            }

            return newFile;
        }

        return file;
    }
}
