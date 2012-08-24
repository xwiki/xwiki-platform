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
package org.xwiki.rendering.macro.wikibridge;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.macro.Macro;

/**
 * Interface for defining wiki content based xwiki rendering macros.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public interface WikiMacro extends Macro<WikiMacroParameters>
{
    /**
     * @return the id under which this macro is registered with the component manager
     * @deprecated since 2.3M1, use {@link org.xwiki.rendering.macro.descriptor.MacroDescriptor#getId()} instead
     */
    @Deprecated
    String getId();

    /**
     * @return the {@link DocumentReference} of the Wiki Macro
     * @since 2.3M1
     */
    DocumentReference getDocumentReference();

    /**
     * @return the author of the Wiki Macro
     * @since 4.2M1
     */
    DocumentReference getAuthorReference();
}
