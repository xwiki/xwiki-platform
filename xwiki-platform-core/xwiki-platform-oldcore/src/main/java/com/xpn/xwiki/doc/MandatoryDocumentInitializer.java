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
package com.xpn.xwiki.doc;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;

/**
 * Provide a document that should be initialized at startup and when creating a new wiki.
 * <p>
 * The role hint should be the local (or absolute if it's supposed to be used only for a specific wiki) reference of the
 * document so that I can easily be found.
 * <p/>
 * An additional {@link javax.annotation.Priority Priority} annotation can be added to affect the order in which the
 * initializer will be executed when a wiki is initialized. If not specified, {@value #DEFAULT_PRIORITY} will be
 * assumed.
 *
 * @version $Id$
 */
@Role
public interface MandatoryDocumentInitializer
{
    /**
     * Default priority with which a document initializer will be executed along side other document initializers.
     *
     * @since 7.0RC1
     */
    int DEFAULT_PRIORITY = 1000;

    /**
     * @return the reference of the document to update. Can be either local or absolute depending if the document is
     *         associated to a specific wiki or not
     */
    EntityReference getDocumentReference();

    /**
     * Update the provided document according to the need.
     *
     * @param document the existing document to update
     * @return true if the document has been modified, false otherwise
     */
    boolean updateDocument(XWikiDocument document);
}
