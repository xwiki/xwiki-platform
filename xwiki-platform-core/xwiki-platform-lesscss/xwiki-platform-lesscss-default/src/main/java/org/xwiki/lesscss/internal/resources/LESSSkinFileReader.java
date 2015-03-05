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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReader;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * Specialized implementation of {@link org.xwiki.lesscss.resources.LESSResourceReader} to read resources referenced by an
 * {@link LESSSkinFileResourceReference} object.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component
@Named("org.xwiki.lesscss.resources.LESSSkinFileResourceReference")
@Singleton
public class LESSSkinFileReader implements LESSResourceReader
{
    @Inject
    private TemplateManager templateManager;
    
    @Inject
    private SkinManager skinManager;

    @Override
    public String getContent(LESSResourceReference lessResourceReference, String skin) throws LESSCompilerException
    {
        if (!(lessResourceReference instanceof LESSSkinFileResourceReference)) {
            throw new LESSCompilerException("Invalid LESS resource type.");
        }

        LESSSkinFileResourceReference lessSkinFileResourceReference =
                (LESSSkinFileResourceReference) lessResourceReference;

        Template template = templateManager.getTemplate("less/" + lessSkinFileResourceReference.getFileName(),
                skinManager.getSkin(skin));
        if (template == null) {
            throw new LESSCompilerException(String.format("The template [%s] does not exists.",
                lessSkinFileResourceReference.getFileName()));
        }
        
        try {
            return template.getContent().getContent();
        } catch (Exception e) {
            throw new LESSCompilerException(String.format("Failed to get the content of the template [%s].",
                lessSkinFileResourceReference.getFileName()), e);
        }
    }
}
