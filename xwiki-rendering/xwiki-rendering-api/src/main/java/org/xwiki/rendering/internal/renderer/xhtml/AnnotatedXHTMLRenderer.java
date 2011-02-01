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
package org.xwiki.rendering.internal.renderer.xhtml;

import org.xwiki.rendering.internal.renderer.xhtml.image.XHTMLImageRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.XHTMLLinkRenderer;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.EmptyBlockChainingListener;
import org.xwiki.rendering.listener.chaining.MetaDataStateChainingListener;
import org.xwiki.rendering.renderer.AbstractChainingPrintRenderer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.phase.Initializable;

/**
 * Generates Annotated XHTML (ie XHTML containing metadata information, for example macro definition or
 * link definition) from a {@link org.xwiki.rendering.block.XDOM} object being traversed.
 * The annotations allow initial source content to be fully reconstructed from the generated XHTML. This is required
 * for example for doing round tripping between wiki syntax and XHTML syntax in the WYSIWYG editor.
 *
 * @version $Id$
 * @since 2.0M3
 */
@Component("annotatedxhtml/1.0")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class AnnotatedXHTMLRenderer extends AbstractChainingPrintRenderer implements Initializable
{
    /**
     * To render link events into annotated XHTML. This is done so that it's pluggable because link rendering depends
     * on how the underlying system wants to handle it. For example for XWiki we check if the document exists, we get
     * the document URL, etc.
     */
    @Requirement("annotated")
    private XHTMLLinkRenderer linkRenderer;

    /**
     * To render image events into annotated XHTML. This is done so that it's pluggable because image rendering depends
     * on how the underlying system wants to handle it. For example for XWiki we check if the image exists as a
     * document attachments, we get its URL, etc.
     */
    @Requirement("annotated")
    private XHTMLImageRenderer imageRenderer;

    /**
     * {@inheritDoc}
     * @see org.xwiki.component.phase.Initializable#initialize()
     * @since 2.0M3
     */
    public void initialize() throws InitializationException
    {
        ListenerChain chain = new ListenerChain();
        setListenerChain(chain);

        // Construct the listener chain in the right order. Listeners early in the chain are called before listeners
        // placed later in the chain.
        chain.addListener(this);
        chain.addListener(new BlockStateChainingListener(chain));
        chain.addListener(new EmptyBlockChainingListener(chain));
        chain.addListener(new MetaDataStateChainingListener(chain));
        chain.addListener(new AnnotatedXHTMLChainingRenderer(this.linkRenderer, this.imageRenderer, chain));
    }
}
