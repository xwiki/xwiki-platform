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
package org.xwiki.pagetemplate;

import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Gives access to the template providers.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Role
@Unstable
public interface TemplateProviderManager
{
    /**
     * Reads the template provider defined in a document.
     *
     * @param reference the reference of the document holding the template provider object
     * @return the template provider, empty if the document doesn't hold a template provider object
     * @throws TemplateProviderException if the document fails to load
     */
    Optional<TemplateProvider> getTemplateProvider(DocumentReference reference) throws TemplateProviderException;
}
