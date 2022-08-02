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
package org.xwiki.test.ui.po;

/**
 * The pane used to display and edit in-place the document syntax.
 * 
 * @version $Id$
 * @since 12.6.3
 * @since 12.9RC1
 */
public class DocumentSyntaxPropertyPane extends EditablePropertyPane<String>
{
    public DocumentSyntaxPropertyPane()
    {
        super("syntax");
    }

    @Override
    public DocumentSyntaxPropertyPane clickEdit()
    {
        return (DocumentSyntaxPropertyPane) super.clickEdit();
    }

    @Override
    public DocumentSyntaxPropertyPane clickCancel()
    {
        DocumentSyntaxPicker syntaxPicker = getSyntaxPicker();
        super.clickCancel();
        // Wait for the syntax change to be reverted.
        syntaxPicker.waitUntilEnabled();
        return this;
    }

    @Override
    public DocumentSyntaxPropertyPane clickSave()
    {
        return (DocumentSyntaxPropertyPane) super.clickSave();
    }

    public DocumentSyntaxPicker getSyntaxPicker()
    {
        return new DocumentSyntaxPicker();
    }
}
