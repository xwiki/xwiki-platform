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
package org.xwiki.user.internal.document;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.user.UserReference;

/**
 * Converts a {@link String} representing a user id into a {@link UserReference}, relatively to the current wiki or
 * sub-wiki.
 * <p>
 * For example {@code XWiki.U1} will be resolved as {@code xwiki:XWiki.U1} on the main wiki, and as {@code s1:XWiki.U1}
 * in the context of the sub-wiki {@code s1}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Singleton
@Component
@Named("current/document")
public class CurrentDocumentStringUserReferenceResolver extends AbstractDocumentStringUserReferenceResolver
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Override
    protected DocumentReferenceResolver<String> getDocumentReferenceResolver()
    {
        return this.resolver;
    }
}
