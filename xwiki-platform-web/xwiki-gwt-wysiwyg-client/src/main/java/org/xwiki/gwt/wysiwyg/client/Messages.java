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
package org.xwiki.gwt.wysiwyg.client;

import com.google.gwt.core.client.GWT;

/**
 * A collection of {@link com.google.gwt.i18n.client.Messages} used to localize the user interface.
 * <p>
 * Use this interface only for l10n strings that have parameters. Make sure you follow the
 * {@link java.text.MessageFormat} style.
 * 
 * @version $Id$
 */
public interface Messages extends com.google.gwt.i18n.client.Messages
{
    /**
     * An instance of this message bundle that can be used anywhere in the code to obtain localized strings.
     */
    Messages INSTANCE = GWT.create(Messages.class);

    /**
     * @param macroName the name of the macro that is going to be inserted
     * @return a string that can be used as a tool tip for the tool bar button that opens the insert macro dialog for
     *         the specified macro
     */
    String macroInsertTooltip(String macroName);
}
