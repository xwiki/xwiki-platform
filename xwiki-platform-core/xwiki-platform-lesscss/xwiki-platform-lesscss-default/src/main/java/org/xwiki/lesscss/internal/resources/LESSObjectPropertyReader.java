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
package org.xwiki.lesscss.internal.resources;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSObjectPropertyResourceReference;
import org.xwiki.lesscss.resources.LESSResourceReader;
import org.xwiki.lesscss.resources.LESSResourceReference;

/**
 * Specialized implementation of {@link org.xwiki.lesscss.resources.LESSResourceReader} to read resources referenced by
 * an {@link LESSObjectPropertyResourceReference} object.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component
@Named("org.xwiki.lesscss.resources.LESSObjectPropertyResourceReference")
@Singleton
public class LESSObjectPropertyReader implements LESSResourceReader
{
    @Inject
    private DocumentAccessBridge bridge;

    @Override
    public String getContent(LESSResourceReference lessResourceReference, String skin)
        throws LESSCompilerException
    {
        if (!(lessResourceReference instanceof LESSObjectPropertyResourceReference)) {
            throw new LESSCompilerException("Invalid LESS resource type.");
        }

        LESSObjectPropertyResourceReference lessObjectPropertyResourceReference =
                (LESSObjectPropertyResourceReference) lessResourceReference;

        return (String) bridge.getProperty(lessObjectPropertyResourceReference.getObjectPropertyReference());
    }
}
