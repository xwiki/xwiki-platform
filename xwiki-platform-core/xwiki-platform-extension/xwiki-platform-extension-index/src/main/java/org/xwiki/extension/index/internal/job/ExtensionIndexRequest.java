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
package org.xwiki.extension.index.internal.job;

import java.util.Arrays;
import java.util.List;

import org.xwiki.job.AbstractRequest;

/**
 * The request to use to configure the {@link ExtensionIndexJob} job.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
public class ExtensionIndexRequest extends AbstractRequest
{
    /**
     * The identifier of the job.
     */
    public static final List<String> JOB_ID = Arrays.asList("extension", "index");

    private final boolean withLocalExtension;

    /**
     * @param withLocalExtensions true if local extensions should be loaded (generally for the first run of the job)
     */
    public ExtensionIndexRequest(boolean withLocalExtensions)
    {
        setId(JOB_ID);

        this.withLocalExtension = withLocalExtensions;
    }

    /**
     * @return the localExtensionEnabled true if local extensions should be loaded (generally for the first run of the
     *         job)
     */
    public boolean isLocalExtensionEnabled()
    {
        return this.withLocalExtension;
    }
}
