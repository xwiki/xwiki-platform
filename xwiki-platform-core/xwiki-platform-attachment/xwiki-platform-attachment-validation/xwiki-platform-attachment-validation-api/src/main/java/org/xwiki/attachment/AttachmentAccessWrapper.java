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
package org.xwiki.attachment;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides the metadata required to access an attachment without depending on a specific implementation.
 * TODO: Should be moved to xwiki-platform-attachment-api as soon at its dependency to oldcore is removed.
 *
 * @version $Id$
 * @since 14.10
 */
public interface AttachmentAccessWrapper
{
    /**
     * @return the attachment size in bytes
     */
    long getSize();

    /**
     * @return the attachment input steam
     * @throws IOException in case of error when retrieving the input steam
     */
    InputStream getInputStream() throws IOException;

    /**
     * @return the attachment filename
     */
    String getFileName();
}
