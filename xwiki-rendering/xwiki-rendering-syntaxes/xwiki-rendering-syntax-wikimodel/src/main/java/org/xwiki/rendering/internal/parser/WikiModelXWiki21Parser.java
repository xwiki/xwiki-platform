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
import org.wikimodel.wem.xwiki.xwiki21.XWikiParser;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser;
import org.xwiki.rendering.internal.parser.wikimodel.XWikiGeneratorListener;
import org.xwiki.rendering.internal.parser.wikimodel.xwiki.XWiki21XWikiGeneratorListener;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ImageParser;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.util.IdGenerator;

/**
 * Parses XWiki Syntax 2.1.
 *
 * @version $Id$
 * @since 2.5M2
 */
@Component("xwiki/2.1")
public class WikiModelXWiki21Parser extends AbstractWikiModelParser
{
    /**
     * @see #getResourceReferenceParser()
     */
    @Requirement("xwiki/2.1")
    private ResourceReferenceParser referenceParser;

    /**
     * @see #getImageParser()
     */
    @Requirement("xwiki/2.0")
    private ImageParser imageParser;

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.parser.Parser#getSyntax()
     * @see org.xwiki.rendering.parser.StreamParser#getSyntax()
     */
    public Syntax getSyntax()
    {
        return Syntax.XWIKI_2_1;
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
     * @see org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser#getImageParser()
     */
    @Override
    public ImageParser getImageParser()
    {
        return this.imageParser;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser#getResourceReferenceParser()
     */
    @Override
    public ResourceReferenceParser getResourceReferenceParser()
    {
        return this.referenceParser;
    }

    /**
     * {@inheritDoc}
     *
     * @see AbstractWikiModelParser#createXWikiGeneratorListener(Listener, IdGenerator)
     */
    @Override
    public XWikiGeneratorListener createXWikiGeneratorListener(Listener listener, IdGenerator idGenerator)
    {
        return new XWiki21XWikiGeneratorListener(getLinkLabelParser(), listener, getResourceReferenceParser(),
            getImageParser(), this.plainRendererFactory, idGenerator);
    }
}
