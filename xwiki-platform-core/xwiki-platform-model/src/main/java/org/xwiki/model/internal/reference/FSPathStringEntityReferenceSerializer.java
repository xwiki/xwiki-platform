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
package org.xwiki.model.internal.reference;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Generate a filesystem path representation of an entity reference (eg "Wiki/Space/Page" for a Document Reference in
 * the "wiki" Wiki, the "space" Space and the "page" Page). Note that the difference with
 * {@link PathStringEntityReferenceSerializer} is that we don't encode the last dot ("."). The reason is for example
 * when this is used to generate a "file:///" URL for a SRC HTML tag, browsers require a file extension to display
 * images.
 *
 * @version $Id$
 * @since 8.4.6
 * @since 9.4RC1
 */
@Component
@Named("fspath")
@Singleton
public class FSPathStringEntityReferenceSerializer extends PathStringEntityReferenceSerializer
{
    @Override
    protected String replaceDot(String name, boolean isLastReference)
    {
        String replacedName;

        // Encode the dot only if we're not on the last reference name
        if (!isLastReference) {
            replacedName = super.replaceDot(name, isLastReference);
        } else {
            replacedName = name;
        }

        return replacedName;
    }
}
