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
package org.xwiki.gwt.wysiwyg.client.syntax;

import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

/**
 * A validation rule states that some of the editor's features (like 'bold', 'indent' etc.) should not be enabled when
 * the text area is in a specific state.
 * 
 * @version $Id$
 */
public interface ValidationRule
{
    /**
     * @return The features this rule is referring to.
     */
    String[] getFeatures();

    /**
     * Verifies if the underlying features of this rule should be enabled considering the current state of the given
     * text area.
     * 
     * @param textArea The text area whose current state should be considered.
     * @return true if all the underlying features should be enabled, false otherwise.
     */
    boolean areValid(RichTextArea textArea);
}
