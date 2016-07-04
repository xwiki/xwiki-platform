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
package org.xwiki.wysiwyg.server.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.edit.AbstractTemplateEditor;
import org.xwiki.edit.DefaultEditorDescriptor;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorDescriptor;
import org.xwiki.rendering.block.XDOM;

/**
 * {@link XDOM} WYSIWYG {@link Editor} implemented using Google Web Toolkit.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Singleton
@Named(GwtXdomEditor.ROLE_HINT)
public class GwtXdomEditor extends AbstractTemplateEditor<XDOM>
{
    /**
     * The editor component hint.
     */
    public static final String ROLE_HINT = "xdomWysiwygGwt";

    private EditorDescriptor descriptor = new DefaultEditorDescriptor(ROLE_HINT, null, "Wysiwyg");

    @Override
    public String getTemplateName()
    {
        return String.format("editors/%s.vm", ROLE_HINT);
    }

    @Override
    public EditorDescriptor getDescriptor()
    {
        return this.descriptor;
    }
}
