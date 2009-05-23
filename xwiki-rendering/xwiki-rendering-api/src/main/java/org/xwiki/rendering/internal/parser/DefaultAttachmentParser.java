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
package org.xwiki.rendering.internal.parser;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.Attachment;
import org.xwiki.rendering.listener.DefaultAttachement;
import org.xwiki.rendering.parser.AttachmentParser;

/**
 * Parses attachment definitions, of the following format: <code>reference@attachmentName</code> where
 * <code>attachmentName</code> is the name of the attachment (for example "my.txt").
 * 
 * @version $Id$
 * @since 1.7.1
 */
@Component
public class DefaultAttachmentParser implements AttachmentParser
{
    /**
     * The separator character between document name and attachment name.
     */
    private static final String DOCUMENTATTACHMENT_SEP = "@";

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.AttachmentParser#parse(java.lang.String)
     */
    public Attachment parse(String attachmentLocation)
    {
        String documentName = null;
        String attachmentName;
        int attachmentSeparatorPosition = attachmentLocation.indexOf(DOCUMENTATTACHMENT_SEP);
        if (attachmentSeparatorPosition > -1) {
            documentName = attachmentLocation.substring(0, attachmentSeparatorPosition);
            attachmentName = attachmentLocation.substring(attachmentSeparatorPosition + 1);
        } else {
            attachmentName = attachmentLocation;
        }

        return new DefaultAttachement(documentName, attachmentName);
    }
}
