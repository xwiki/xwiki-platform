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
package org.xwiki.lesscss.internal.skin;

import org.xwiki.component.annotation.Role;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Constructs the appropriate reference for a given skin.
 *
 * @since 7.0RC1
 * @version $Id$
 */
@Role
@Unstable
public interface SkinReferenceFactory
{
    /**
     * Constructs the appropriate reference for a given skin.
     * @param skinName name of the skin
     * @return the reference to the skin
     * @throws LESSCompilerException if problem occurs
     */
    SkinReference createReference(String skinName) throws LESSCompilerException;

    /**
     * Constructs the appropriate reference for a given skin.
     * @param documentReference reference of a document holding a skin
     * @return the reference to the skin
     */
    SkinReference createReference(DocumentReference documentReference);
}
