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
package org.xwiki.rendering.internal.macro.message;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Displays a warning message.
 *
 * @version $Id$
 * @since 16.7.0RC1
 */
@Component
@Named("warning")
@Singleton
public class XWikiWarningMessageMacro extends WarningMessageMacro
{
    private static final String ICON_PRETTY_NAME_KEY = "rendering.macro.message.icon.alternative.warning";
    @Inject
    private ContextualLocalizationManager l10n;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public XWikiWarningMessageMacro()
    {
        super();
        this.iconPrettyName = l10n.getTranslationPlain(ICON_PRETTY_NAME_KEY);
    }
}
