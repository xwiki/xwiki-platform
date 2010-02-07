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
package org.xwiki.gwt.wysiwyg.client.syntax.rule;

import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.syntax.ValidationRule;


/**
 * Validation rule used to define the behavior when the selection is on an image. This will disable features that refer
 * to text formatting (bold, italic, superscript) and in general all features that use the selection as text or replace
 * it with other elements (such as the symbol plugin).
 * 
 * @version $Id$
 */
public class ImageSelectionBehaviourRule implements ValidationRule
{
    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#areValid(RichTextArea)
     */
    public boolean areValid(RichTextArea textArea)
    {
        return !textArea.getCommandManager().isExecuted(Command.INSERT_IMAGE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#getFeatures()
     */
    public String[] getFeatures()
    {
        return new String[] {"bold", "italic", "underline", "strikethrough", "subscript", "superscript",
            "unorderedlist", "orderedlist", "outdent", "indent", "format", "hr", "symbol", "inserttable", 
            "deletetable", "importer"};
    }
}
