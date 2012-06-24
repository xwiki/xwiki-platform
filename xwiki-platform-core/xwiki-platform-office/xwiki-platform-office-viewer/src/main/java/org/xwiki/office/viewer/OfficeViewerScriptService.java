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
package org.xwiki.office.viewer;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.script.service.ScriptService;

/**
 * Exposes utility methods for viewing office attachments in server-side scripts.
 * 
 * @since 2.5M2
 * @version $Id$
 */
@Role
public interface OfficeViewerScriptService extends ScriptService
{
    /**
     * Builds a XHTML representation of the specified office attachment.
     * 
     * @param attachmentReference reference to the office attachment to be viewed
     * @return a XHTML representation of the specified office attachment or {@code null} if an error occurs
     */
    String view(AttachmentReference attachmentReference);

    /**
     * Builds a XHTML representation of the specified office attachment.
     * 
     * @param attachmentReference reference to the office attachment to be viewed
     * @param parameters view parameters specific to the underlying {@link OfficeViewer} implementation
     * @return a XHTML representation of the specified office attachment or {@code null} if an error occurs
     */
    String view(AttachmentReference attachmentReference, Map<String, String> parameters);

    /**
     * @return the exception caught during the last office attachment view on the current execution, or {@code null} if
     *         no exception has been thrown
     */
    Exception getCaughtException();

    /**
     * Checks if a given mime type is supported for viewing.
     * 
     * @param mimeType a mime type
     * @return {@code true} if the specified mime type can be viewed, {@code false} otherwise
     */
    boolean isMimeTypeSupported(String mimeType);
}
