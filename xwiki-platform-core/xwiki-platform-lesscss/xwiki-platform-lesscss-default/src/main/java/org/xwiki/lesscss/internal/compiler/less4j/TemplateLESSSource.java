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
package org.xwiki.lesscss.internal.compiler.less4j;

import java.io.File;

import org.xwiki.skin.Skin;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.template.TemplateManager;

/**
 * Class that get the LESS code from skin templates. Execute velocity on .vm files too. 
 *  
 * @version $Id$
 * @since 7.0RC1 
 */
public class TemplateLESSSource extends AbstractLESSSource
{
    private String templateName;

    /**
     * Default constructor.
     * @param templateManager the template manager component
     * @param skin the skin holding the template
     * @param templateName the name of the template
     * @param useVelocity set if velocity is executed or not 
     */
    public TemplateLESSSource(TemplateManager templateManager, Skin skin, String templateName, boolean useVelocity)
    {
        super(templateManager, skin, useVelocity, new File(templateName).getParent());
        this.templateName = templateName;
    }

    @Override
    public String getContent() throws FileNotFound, CannotReadFile
    {
        try {
            // Execute Velocity on some files 
            if (useVelocity && templateName.endsWith(".vm")) {
                return templateManager.renderFromSkin(templateName, skin);
            } 
            
            // Otherwise, return the raw content
            Template template = templateManager.getTemplate(templateName, skin);
            TemplateContent templateContent = template.getContent();
            return templateContent.getContent();
            
        } catch (Exception e) {
            throw new CannotReadFile();
        }
    }

    @Override
    public byte[] getBytes() throws FileNotFound, CannotReadFile
    {
        return getContent().getBytes();
    }

    @Override
    public String getName()
    {
        return templateName;
    }
}
