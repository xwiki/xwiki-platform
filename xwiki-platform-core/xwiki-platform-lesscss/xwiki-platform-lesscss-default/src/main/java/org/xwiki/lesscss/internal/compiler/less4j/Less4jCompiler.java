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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.TemplateManager;

import com.github.sommeri.less4j.Less4jException;
import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.LessSource;
import com.github.sommeri.less4j.core.DefaultLessCompiler;

/**
 * Compile some LESS code with Less4j and get the included files from the skin templates.
 * 
 * @version $Id$
 * @since 7.0RC1 
 */
@Component(roles = Less4jCompiler.class)
@Singleton
public class Less4jCompiler
{
    @Inject
    private TemplateManager templateManager;
    
    @Inject
    private SkinManager skinManager;

    /**
     * Compile the LESS code and get the included files from the skin templates.
     * @param lessCode code to compile
     * @param skin skin holding the templates
     * @return the results of the LESS compilation
     * @throws Less4jException if problems occur
     */
    public String compile(String lessCode, String skin) throws Less4jException
    {
        LessCompiler lessCompiler = new DefaultLessCompiler();
        LessCompiler.Configuration options = new LessCompiler.Configuration();
        options.setCompressing(true);
        LessSource lessSource = 
            new CustomContentLESSSource(lessCode, templateManager, skinManager.getSkin(skin));
        LessCompiler.CompilationResult lessResult = lessCompiler.compile(lessSource, options);
        return lessResult.getCss();
    }
}
