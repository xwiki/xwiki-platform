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

import org.xwiki.skin.Resource;
import org.xwiki.skin.Skin;
import org.xwiki.template.TemplateManager;

import com.github.sommeri.less4j.LessSource;

/**
 * Abstract implementation of LessSource that looks for relative sources in the skin templates.
 *  
 * @version $Id$
 * @since 7.0RC1
 */
public abstract class AbstractLESSSource extends LessSource 
{
    protected TemplateManager templateManager;

    protected Skin skin;
    
    private String folder;

    /**
     * Default constructor.
     * @param templateManager the template manager component
     * @param skin the skin holding the templates
     * @param folder the folder in which the template is located
     */
    public AbstractLESSSource(TemplateManager templateManager, Skin skin, String folder)
    {
        this.templateManager = templateManager;
        this.skin = skin;
        this.folder = folder;
    }
    
    @Override
    public LessSource relativeSource(String filename) throws FileNotFound
    {
        String template = folder + "/" + filename;
        Resource resource = skin.getResource(template);
        if (resource != null) {
            return new TemplateLESSSource(templateManager, skin, template);
        }

        // The file has not been found
        throw new FileNotFound();
    }
}
