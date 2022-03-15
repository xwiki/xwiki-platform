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
package org.xwiki.internal.web;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provide the operations to know if a document must be considered as existing in the current context. In other words,
 * validate if the current request must return a 404 response for the current document.
 *
 * @version $Id$
 * @since 13.10.4
 * @since 14.2RC1
 */
@Role
public interface DocExistValidator
{
    /**
     * Validate if a 404 response must be returned for the current document, according to the request action
     * parameters.
     *
     * @param doc the document to validate
     * @param context the current context
     * @return {@code true} if the document does not exist and a 404 error will be returned, {@code false} otherwise
     */
    boolean docExist(XWikiDocument doc, XWikiContext context);
}
