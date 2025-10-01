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
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorConfiguration;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.SyntaxContent;

/**
 * Custom configuration for {@link SyntaxContent} {@link Editor}s, which serves two roles:
 * <ul>
 * <li>preserves backward compatibility (because it looks for configuration properties that existed before the edit
 * module was written)</li>
 * <li>provides the default editor when there's no one configured</li>
 * </ul>
 * .
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Singleton
public class SyntaxContentEditorConfiguration implements EditorConfiguration<SyntaxContent>
{
    @Inject
    private EditorConfiguration<XDOM> xdomEditorConfiguration;

    @Override
    public String getDefaultEditor()
    {
        return this.xdomEditorConfiguration.getDefaultEditor();
    }

    @Override
    public String getDefaultEditor(String category)
    {
        return this.xdomEditorConfiguration.getDefaultEditor(category);
    }
}
