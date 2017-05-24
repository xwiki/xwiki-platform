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
package org.xwiki.template.helpers;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.text.StringUtils;

/**
 * Allows the creation of a template directly by specifying its content as a string in the constructor.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class StringTemplate implements Template
{
    private class StringTemplateContent implements TemplateContent
    {
        private String content;

        private Syntax sourceSyntax;

        private Syntax rawSyntax;

        private Map<String, Object> properties;

        private DocumentReference authorReference;

        StringTemplateContent(
                String content,
                Syntax sourceSyntax,
                Syntax rawSyntax,
                DocumentReference authorReference) {
            this.content = content;
            this.sourceSyntax = sourceSyntax;
            this.rawSyntax = rawSyntax;
            this.properties = new HashMap<>();
            this.authorReference = authorReference;
        }

        @Override
        public String getContent()
        {
            return this.content;
        }

        @Override
        public Syntax getSourceSyntax()
        {
            return this.sourceSyntax;
        }

        @Override
        public Syntax getRawSyntax()
        {
            return this.rawSyntax;
        }

        @Override
        public <T> T getProperty(String name, Type type)
        {
            return (this.properties.containsKey(name)
                    && this.properties.get(name).getClass().getName().equals(type.getTypeName()))
                    ? (T) this.properties.get(name) : null;
        }

        @Override
        public <T> T getProperty(String name, T def)
        {
            return (this.properties.containsKey(name)) ? (T) this.properties.get(name) : def;
        }

        @Override
        public DocumentReference getAuthorReference()
        {
            return this.authorReference;
        }
    }

    private TemplateContent content;

    /**
     * Builds a new {@link StringTemplate}.
     *
     * @param content the template content
     * @param sourceSyntax the syntax of the source template
     * @param rawSyntax the raw syntax
     * @param authorReference a reference to the template author
     */
    public StringTemplate(
            String content,
            Syntax sourceSyntax,
            Syntax rawSyntax,
            DocumentReference authorReference) {
        this.content = new StringTemplateContent(content, sourceSyntax, rawSyntax, authorReference);
    }

    @Override
    public String getId()
    {
        return StringUtils.EMPTY;
    }

    @Override
    public String getPath()
    {
        return StringUtils.EMPTY;
    }

    @Override
    public TemplateContent getContent() throws Exception
    {
        return this.content;
    }
}
