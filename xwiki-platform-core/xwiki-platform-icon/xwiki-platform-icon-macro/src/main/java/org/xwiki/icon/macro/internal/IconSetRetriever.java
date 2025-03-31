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
package org.xwiki.icon.macro.internal;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.icon.macro.DisplayIconMacroParameters;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;


class IconSetRetriever
{
    public IconSet getIconSet(DisplayIconMacroParameters parameters,
                              IconSetManager iconSetManager, DocumentAccessBridge documentAccessBridge,
                              ContextualAuthorizationManager contextualAuthorization)
            throws IconException, MacroExecutionException
    {
        IconSet iconSet;

        if (parameters.getIconSet() == null) {
            iconSet = iconSetManager.getCurrentIconSet();
        } else {
            iconSet = iconSetManager.getIconSet(parameters.getIconSet());
        }

        // Check if the current user can access the icon theme. If not, fall back to the default icon theme or throw
        // an exception when the fallback is disabled.
        if (iconSet != null && iconSet.getSourceDocumentReference() != null
                && !contextualAuthorization.hasAccess(Right.VIEW, iconSet.getSourceDocumentReference()))
        {
            if (parameters.isFallback()) {
                iconSet = null;
            } else {
                throw new MacroExecutionException(
                        String.format("Current user [%s] doesn't have view rights on the icon set's document [%s]",
                                documentAccessBridge.getCurrentUserReference(), iconSet.getSourceDocumentReference()));
            }
        }

        if (parameters.isFallback() && (iconSet == null || !iconSet.hasIcon(parameters.getName()))) {
            iconSet = iconSetManager.getDefaultIconSet();
        }

        return iconSet;
    }
}
