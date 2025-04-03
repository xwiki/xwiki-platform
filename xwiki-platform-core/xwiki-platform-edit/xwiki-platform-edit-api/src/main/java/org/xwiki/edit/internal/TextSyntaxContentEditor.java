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
package org.xwiki.edit.internal;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.edit.AbstractTemplateEditor;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorDescriptor;
import org.xwiki.edit.EditorDescriptorBuilder;
import org.xwiki.rendering.syntax.SyntaxContent;

/**
 * {@link SyntaxContent} Text {@link Editor}.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Singleton
@Named(TextXDOMEditor.ROLE_HINT)
public class TextSyntaxContentEditor extends AbstractTemplateEditor<SyntaxContent> implements Initializable
{
    @Inject
    private EditorDescriptorBuilder editorDescriptorBuilder;

    @Override
    public String getTemplateName()
    {
        // Re-use the XDOM Text editor template.
        return "editors/xdomText.vm";
    }

    @Override
    public EditorDescriptor getDescriptor()
    {
        // Re-build the descriptor for the current localization context.
        return this.editorDescriptorBuilder.build();
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.editorDescriptorBuilder.setId(TextXDOMEditor.ROLE_HINT).setCategory(TextXDOMEditor.ROLE_HINT);
    }
}
