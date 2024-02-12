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
package org.xwiki.lesscss.internal.resources;

import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * A reference to a skin file located in the "less" directory.
 *
 * @since 6.4M2
 * @version $Id :$
 */
public class LESSSkinFileResourceReference implements LESSResourceReference
{
    private String fileName;

    private TemplateManager templateManager;
    
    private SkinManager skinManager;

    /**
     * Constructor.
     * @param fileName name of the file inside the "less" directory in the skin
     * @param templateManager component to get templates
     * @param skinManager component to get skins
     */
    public LESSSkinFileResourceReference(String fileName, TemplateManager templateManager, SkinManager skinManager)
    {
        this.fileName = fileName;
        this.templateManager = templateManager;
        this.skinManager = skinManager;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof LESSSkinFileResourceReference) {
            return fileName.equals(((LESSSkinFileResourceReference) o).fileName);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return fileName.hashCode();
    }

    @Override public String getContent(String skin) throws LESSCompilerException
    {
        Template template = templateManager.getTemplate("less/" + fileName, skinManager.getSkin(skin));
        if (template == null) {
            throw new LESSCompilerException(String.format("The template [%s] does not exist.", fileName));
        }

        try {
            return template.getContent().getContent();
        } catch (Exception e) {
            throw new LESSCompilerException(
                    String.format("Failed to get the content of the template [%s].", fileName), e);
        }
    }

    @Override
    public String serialize()
    {
        return String.format("LessSkinFile[%s]", fileName);
    }

    @Override
    public String toString()
    {
        return serialize();
    }
}
