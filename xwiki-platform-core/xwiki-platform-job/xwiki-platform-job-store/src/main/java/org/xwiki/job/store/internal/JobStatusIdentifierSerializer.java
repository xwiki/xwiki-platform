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
package org.xwiki.job.store.internal;

import java.util.List;

import javax.inject.Singleton;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.codec.digest.DigestUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.internal.JobStatusFolderResolver;

/**
 * Computes normalized identifiers for relational tables and blob stores.
 *
 * @version $Id$
 * @since 18.2.0RC1
 */
@Component(roles = JobStatusIdentifierSerializer.class)
@Singleton
public class JobStatusIdentifierSerializer
{
    private static final int MAX_IDENTIFIER_LENGTH = 512;

    private static final String HASH_SEPARATOR = "__";

    private static final int HASH_LENGTH = 128;

    private static final String DELIMITER = "/";

    @Inject
    @Named("version3")
    private JobStatusFolderResolver folderResolver;

    /**
     * @param jobId the identifier segments
     * @return the normalized key derived from the escaped identifier (used as blob locator), maximum length
     * {@value #MAX_IDENTIFIER_LENGTH}
     */
    public String getBlobKey(List<String> jobId)
    {
        String escapedId = String.join(DELIMITER, this.folderResolver.getFolderSegments(jobId));
        return normalize(escapedId);
    }

    /**
     * @param jobId the identifier segments
     * @return the normalized key used as identifier in relational tables, maximum length
     * {@value #MAX_IDENTIFIER_LENGTH}
     */
    public String getDatabaseKey(List<String> jobId)
    {
        if (jobId == null) {
            return "";
        }

        String rawId = String.join(DELIMITER, jobId);
        return normalize(rawId);
    }

    /**
     * @param jobId the identifier segments
     * @return the raw identifier, without normalization
     */
    public String getRawId(List<String> jobId)
    {
        if (jobId == null) {
            return "";
        }
        return String.join(DELIMITER, jobId);
    }

    private String normalize(String value)
    {
        if (value == null) {
            return "";
        }

        if (value.length() <= MAX_IDENTIFIER_LENGTH) {
            return value;
        }

        int hashLength = HASH_SEPARATOR.length() + HASH_LENGTH;
        int prefixLength = Math.max(0, MAX_IDENTIFIER_LENGTH - hashLength);

        String truncatedPart = value.substring(prefixLength);
        String prefix = value.substring(0, prefixLength);
        String hash = DigestUtils.sha512Hex(truncatedPart);
        return prefix + HASH_SEPARATOR + hash;
    }
}
