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
package org.xwiki.rendering.internal.parser;

import org.wikimodel.wem.IWikiParser;
import org.wikimodel.wem.xwiki.xwiki20.XWikiParser;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.syntax.Syntax;

/**
 * @version $Id$
 * @since 2.1M1
 */
@Component("xwiki/2.0")
public class WikiModelXWiki20Parser extends AbstractWikiModelParser
{
    /**
     * @see #getLinkReferenceParser()
     */
    @Requirement("xwiki/2.0/link")
    private ResourceReferenceParser linkReferenceParser;

    /**
     * @see #getImageReferenceParser()
     */
    @Requirement("xwiki/2.0/image")
    private ResourceReferenceParser imageReferenceParser;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.Parser#getSyntax()
     * @see org.xwiki.rendering.parser.StreamParser#getSyntax()
     */
    public Syntax getSyntax()
    {
        return Syntax.XWIKI_2_0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser#createWikiModelParser()
     */
    @Override
    public IWikiParser createWikiModelParser()
    {
        return new XWikiParser();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiModelParser#getLinkReferenceParser()
     * @since 2.5RC1
     */
    @Override
    public ResourceReferenceParser getLinkReferenceParser()
    {
        return this.linkReferenceParser;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser#getImageReferenceParser()
     * @since 2.5RC1
     */
    @Override
    public ResourceReferenceParser getImageReferenceParser()
    {
        return this.imageReferenceParser;
    }
}
