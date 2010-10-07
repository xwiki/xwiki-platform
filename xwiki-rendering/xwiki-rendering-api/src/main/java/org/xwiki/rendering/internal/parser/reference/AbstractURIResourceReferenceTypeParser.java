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
package org.xwiki.rendering.internal.parser.reference;

import org.xwiki.rendering.listener.ResourceReference;
import org.xwiki.rendering.parser.ResourceReferenceTypeParser;

/**
 * Default resource reference type parser for URIs: just take the full reference as the Resource reference.
 * Note that this parser doesn't extract the scheme from the URI for the resource reference.
 *
 * @version $Id$
 * @since 2.5RC1
 */
public abstract class AbstractURIResourceReferenceTypeParser implements ResourceReferenceTypeParser
{
    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.parser.ResourceReferenceTypeParser#parse(String)
     */
    public ResourceReference parse(String reference)
    {
        return new ResourceReference(reference, getType());
    }
}
