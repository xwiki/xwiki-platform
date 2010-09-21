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
package org.xwiki.officepreview;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

/**
 * Exposes office preview utility methods to velocity scripts.
 * 
 * @since 2.5M2
 * @version $Id$
 */
@ComponentRole
public interface OfficePreviewScriptService extends ScriptService
{
    /**
     * Builds a preview of the specified office attachment in xhtml/1.0 syntax.
     * 
     * @param attachmentStringReference string identifying the office attachment
     * @param filterStyles whether office document styles should be filtered
     * @return preview of the specified office attachment or {@code null} if an error occurs
     */
    String preview(String attachmentStringReference, boolean filterStyles);

    /**
     * Builds a preview of the specified office attachment in xhtml/1.0 syntax.
     * 
     * @param documentStringReference string identifying the document holding office attachment
     * @param fileName the office file name
     * @param filterStyles whether office document styles should be filtered
     * @return preview of the specified office attachment or {@code null} if an error occurs
     */
    String preview(String documentStringReference, String fileName, boolean filterStyles);

    /**
     * Builds a preview of the specified office attachment in xhtml/1.0 syntax.
     * 
     * @param documentReference reference to the document holding the attachment to be previewed
     * @param fileName the office file name
     * @param filterStyles whether office document styles should be filtered
     * @return preview of the specified office attachment or {@code null} if an error occurs
     */
    String preview(DocumentReference documentReference, String fileName, boolean filterStyles);

    /**
     * Builds a preview of the specified office attachment in xhtml/1.0 syntax.
     * 
     * @param attachmentReference reference to the attachment to be previewed
     * @param filterStyles whether office document styles should be filtered
     * @return preview of the specified office attachment or {@code null} if an error occurs
     */
    String preview(AttachmentReference attachmentReference, boolean filterStyles);

    /**
     * @return the exception caught during the last preview on the current execution, or {@code null} if no exception
     *         has been thrown
     */
    Exception getCaughtException();
}
