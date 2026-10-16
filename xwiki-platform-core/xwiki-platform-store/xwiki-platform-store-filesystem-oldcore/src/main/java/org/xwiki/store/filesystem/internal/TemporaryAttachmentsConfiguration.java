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
package org.xwiki.store.filesystem.internal;

import java.io.File;
import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.XWikiContext;

import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_DEFAULT_MAXSIZE;
import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER;

/**
 * Configuration of the temporary attachments (the files uploaded in an editor before the document is saved).
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = TemporaryAttachmentsConfiguration.class)
@Singleton
public class TemporaryAttachmentsConfiguration
{
    /**
     * The default number of hours after which a temporary attachment is deleted.
     */
    public static final int DEFAULT_MAX_AGE_HOURS = 24;

    /**
     * The default maximum number of pending temporary attachments of a user in one browser.
     */
    public static final int DEFAULT_MAX_COUNT = 100;

    /**
     * The default maximum total size of the pending temporary attachments of a user in one browser, as a multiple of
     * the {@code upload_maxsize} preference.
     */
    public static final int DEFAULT_MAX_TOTAL_SIZE_FACTOR = 10;

    private static final String PREFIX = "store.temporaryAttachments.";

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Gets the age after which a temporary attachment is deleted, whatever happens to the editing session that
     * uploaded it.
     *
     * @return the value of {@code store.temporaryAttachments.maxAge} (a number of hours, {@code 24} by default, a value
     *     lower than {@code 1} falls back to the default)
     */
    public Duration getMaxAge()
    {
        int hours = this.configuration.getProperty(PREFIX + "maxAge", DEFAULT_MAX_AGE_HOURS);

        return Duration.ofHours(hours > 0 ? hours : DEFAULT_MAX_AGE_HOURS);
    }

    /**
     * Gets the directory of the temporary attachments when the {@code filesystem} blob store is used.
     *
     * @return the value of {@code store.temporaryAttachments.directory}, or {@code null} when it is not set (see
     *     {@link XWikiFilesystemBlobStorePropertiesCustomizer} for the default)
     */
    public File getDirectory()
    {
        String directory = this.configuration.getProperty(PREFIX + "directory");

        return StringUtils.isNotBlank(directory) ? new File(directory) : null;
    }

    /**
     * Gets the maximum number of pending temporary attachments of a user in one browser. An upload above this limit is
     * rejected until some of the pending ones are saved, cancelled or expired. The copies made by
     * {@code temporarilyAttach} are not counted.
     *
     * @return the value of {@code store.temporaryAttachments.maxCount} ({@code 100} by default, a negative value means
     *     no limit)
     */
    public int getMaxCount()
    {
        return this.configuration.getProperty(PREFIX + "maxCount", DEFAULT_MAX_COUNT);
    }

    /**
     * Gets the maximum total size of the pending temporary attachments of a user in one browser. An upload that would
     * exceed it is rejected.
     *
     * @return the value of {@code store.temporaryAttachments.maxTotalSize} in bytes; when it is not set, ten times the
     *     {@code upload_maxsize} preference of the current space (320 MiB with the default 32 MiB); a negative value
     *     means no limit
     */
    public long getMaxTotalSize()
    {
        Long size = this.configuration.getProperty(PREFIX + "maxTotalSize", Long.class);
        if (size != null) {
            return size;
        }

        XWikiContext context = this.contextProvider.get();
        long uploadMaxSize = UPLOAD_DEFAULT_MAXSIZE;
        if (context != null && context.getWiki() != null) {
            uploadMaxSize =
                context.getWiki().getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE, context);
        }

        return DEFAULT_MAX_TOTAL_SIZE_FACTOR * uploadMaxSize;
    }
}
