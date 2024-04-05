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
package com.xpn.xwiki.internal.template;

import java.io.Writer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.skin.Skin;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * @version $Id$
 * @since 7.0M1
 */
@Component
@Singleton
public class DefaultTemplateManager implements TemplateManager
{
    @Inject
    private InternalTemplateManager templateManager;

    @Override
    public XDOM getXDOMNoException(Template template)
    {
        return this.templateManager.getXDOMNoException(template);
    }

    @Override
    public XDOM getXDOMNoException(String templateName)
    {
        return this.templateManager.getXDOMNoException(templateName);
    }

    @Override
    public XDOM getXDOM(Template template) throws Exception
    {
        return this.templateManager.getXDOM(template);
    }

    @Override
    public XDOM getXDOM(String templateName) throws Exception
    {
        return this.templateManager.getXDOM(templateName);
    }

    @Override
    public String renderNoException(String templateName)
    {
        return renderNoException(templateName, false);
    }

    @Override
    public String renderNoException(String templateName, boolean inline)
    {
        return this.templateManager.renderNoException(templateName, inline);
    }

    @Override
    public String render(String templateName) throws Exception
    {
        return render(templateName, false);
    }

    @Override
    public String render(String templateName, boolean inline) throws Exception
    {
        return this.templateManager.render(templateName, inline);
    }

    @Override
    public String renderFromSkin(String templateName, Skin skin) throws Exception
    {
        return renderFromSkin(templateName, skin, false);
    }

    @Override
    public String renderFromSkin(String templateName, Skin skin, boolean inline) throws Exception
    {
        return this.templateManager.renderFromSkin(templateName, skin, inline);
    }

    @Override
    public void renderNoException(Template template, Writer writer)
    {
        renderNoException(template, false, writer);
    }

    @Override
    public void renderNoException(Template template, boolean inline, Writer writer)
    {
        this.templateManager.renderNoException(template, inline, writer);
    }

    @Override
    public void renderNoException(String templateName, Writer writer)
    {
        this.templateManager.renderNoException(templateName, false, writer);
    }

    @Override
    public void renderNoException(String templateName, boolean inline, Writer writer)
    {
        this.templateManager.renderNoException(templateName, inline, writer);
    }

    @Override
    public void render(Template template, Writer writer) throws Exception
    {
        render(template, false, writer);
    }

    @Override
    public void render(Template template, boolean inline, Writer writer) throws Exception
    {
        this.templateManager.render(template, inline, writer);
    }

    @Override
    public void render(String templateName, Writer writer) throws Exception
    {
        render(templateName, false, writer);
    }

    @Override
    public void render(String templateName, boolean inline, Writer writer) throws Exception
    {
        this.templateManager.render(templateName, inline, writer);
    }

    @Override
    public void renderFromSkin(String templateName, Skin skin, Writer writer) throws Exception
    {
        renderFromSkin(templateName, skin, false, writer);
    }

    @Override
    public void renderFromSkin(String templateName, Skin skin, boolean inline, Writer writer) throws Exception
    {
        this.templateManager.renderFromSkin(templateName, skin, inline, writer);
    }

    @Override
    public XDOM executeNoException(Template template)
    {
        return (XDOM) executeNoException(template, false);
    }

    @Override
    public Block executeNoException(Template template, boolean inline)
    {
        return this.templateManager.executeNoException(template, inline);
    }

    @Override
    public XDOM executeNoException(String templateName)
    {
        return (XDOM) executeNoException(templateName, false);
    }

    @Override
    public Block executeNoException(String templateName, boolean inline)
    {
        return this.templateManager.executeNoException(templateName, inline);
    }

    @Override
    public XDOM execute(Template template) throws Exception
    {
        return (XDOM) execute(template, false);
    }

    @Override
    public Block execute(Template template, boolean inline) throws Exception
    {
        return this.templateManager.execute(template, inline);
    }

    @Override
    public XDOM execute(String templateName) throws Exception
    {
        return (XDOM) execute(templateName, false);
    }

    @Override
    public Block execute(String templateName, boolean inline) throws Exception
    {
        return this.templateManager.execute(templateName, inline);
    }

    @Override
    public Template getSkinTemplate(String templateName, Skin skin)
    {
        return this.templateManager.getResourceTemplate(templateName, skin);
    }

    @Override
    public Template getTemplate(String templateName, Skin skin)
    {
        return this.templateManager.getTemplate(templateName, skin);
    }

    @Override
    public Template getTemplate(String templateName)
    {
        return this.templateManager.getTemplate(templateName);
    }

    @Override
    @Deprecated
    public Template createStringTemplate(String content, DocumentReference author) throws Exception
    {
        return createStringTemplate(content, author, null);
    }

    @Override
    @Deprecated(since = "15.9RC1")
    public Template createStringTemplate(String content, DocumentReference author, DocumentReference sourceReference)
        throws Exception
    {
        return createStringTemplate("Unknown template", content, author, sourceReference);
    }

    @Override
    public Template createStringTemplate(String id, String content, DocumentReference author,
        DocumentReference sourceReference) throws Exception
    {
        return this.templateManager.createStringTemplate(id, content, author, sourceReference);
    }
}
