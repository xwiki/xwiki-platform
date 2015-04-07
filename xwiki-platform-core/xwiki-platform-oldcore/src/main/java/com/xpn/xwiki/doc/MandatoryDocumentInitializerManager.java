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
import org.xwiki.model.reference.DocumentReference;

/**
 * Provide utility method to access various mandatory document initializers.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Role
public interface MandatoryDocumentInitializerManager
{
    /**
     * @param reference the document reference
     * @return the initializer associated to the passed document reference
     */
    MandatoryDocumentInitializer getMandatoryDocumentInitializer(DocumentReference reference);
}
