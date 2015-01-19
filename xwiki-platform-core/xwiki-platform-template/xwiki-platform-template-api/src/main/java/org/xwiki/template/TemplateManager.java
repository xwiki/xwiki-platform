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
package org.xwiki.template;

import java.io.Writer;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.skin.Skin;
import org.xwiki.stability.Unstable;

/**
 * Internal toolkit to experiment on wiki bases templates.
 *
 * @version $Id$
 * @since 7.0M1
 */
@Role
@Unstable
public interface TemplateManager
{
    public XDOM getXDOMNoException(String templateName);

    public XDOM getXDOM(String templateName) throws Exception;

    public String renderNoException(String template);

    public String render(String template) throws Exception;

    public String renderFromSkin(String template, String skinId) throws Exception;

    public String renderFromSkin(String template, Skin skin) throws Exception;

    public void renderNoException(String template, Writer writer);

    public void render(Template template, Writer writer) throws Exception;

    public void render(String template, Writer writer) throws Exception;

    public void renderFromSkin(final String templateName, Skin skin, final Writer writer) throws Exception;

    public XDOM executeNoException(String template);

    public XDOM execute(String templateName) throws Exception;

    public Template getSkinTemplate(String templateName, Skin skin);

    public Template getTemplate(String templateName, Skin skin);

    public Template getTemplate(String templateName);
}
