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

import org.xwiki.skin.Skin;
import org.xwiki.template.TemplateManager;

/**
 * Class that hold the LESS code from a string, but look for included files into the skin templates.
 *  
 * @version $Id$
 * @since 7.0RC1
 */
public class CustomContentLESSSource extends AbstractLESSSource
{
    private String lessCode;

    /**
     * Default constructor.
     * @param lessCode the LESS code to compile 
     * @param templateManager the template manager component
     * @param skin the skin holding the template
     */
    public CustomContentLESSSource(String lessCode, TemplateManager templateManager, Skin skin)
    {
        super(templateManager, skin, "less");
        this.lessCode = lessCode;
    }

    @Override
    public String getContent() throws FileNotFound, CannotReadFile
    {
        return lessCode;
    }

    @Override
    public byte[] getBytes() throws FileNotFound, CannotReadFile
    {
        return lessCode.getBytes();
    }
}
