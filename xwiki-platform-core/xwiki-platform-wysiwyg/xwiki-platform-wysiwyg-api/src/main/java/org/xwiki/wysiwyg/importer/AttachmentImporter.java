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
package org.xwiki.wysiwyg.importer;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.AttachmentReference;

/**
 * Interface used to import existing attachments into the content of a WYSIWYG editor.
 * 
 * @version $Id$
 * @since 9.8
 */
@Role
public interface AttachmentImporter
{
    /**
     * Generates the HTML needed to import the specified attachment.
     * 
     * @param attachmentReference specifies the attachment to import
     * @param parameters import parameters
     * @return the HTML to insert into the content of the WYSIWIYG editor
     * @throws Exception if importing the specified attachment fails
     */
    String toHTML(AttachmentReference attachmentReference, Map<String, Object> parameters) throws Exception;
}
