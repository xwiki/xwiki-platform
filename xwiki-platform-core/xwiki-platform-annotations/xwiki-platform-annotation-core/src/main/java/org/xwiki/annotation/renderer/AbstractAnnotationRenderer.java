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
package org.xwiki.annotation.renderer;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.content.ContentAlterer;
import org.xwiki.annotation.internal.renderer.AnnotationGeneratorChainingListener;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.listener.chaining.BlockStateChainingListener;
import org.xwiki.rendering.listener.chaining.EmptyBlockChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.MetaDataStateChainingListener;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.AbstractChainingPrintRenderer;
import org.xwiki.rendering.renderer.reference.link.LinkLabelGenerator;

/**
 * Abstract class for annotation renderer, any specific syntax renderer should implement this class and provide the
 * specific annotation listener.
 * 
 * @version $Id$
 * @since 2.3M1
 */
public abstract class AbstractAnnotationRenderer extends AbstractChainingPrintRenderer implements Initializable,
    AnnotationPrintRenderer
{
    /**
     * Selection cleaner so that the selection can be mapped on the content. <br>
     * TODO: not really sure if this is the right place for this pull, but the annotations generator is not a component
     * so it cannot 'require' it.
     */
    @Inject
    @Named("whitespace")
    protected ContentAlterer selectionAlterer;

    /**
     * Plain text parser used to parse generated link labels.
     */
    @Inject
    @Named("plain/1.0")
    protected StreamParser plainTextParser;

    /**
     * The annotations generator listener to use in this renderer.
     */
    protected AnnotationGeneratorChainingListener annotationsGenerator;

    @Override
    public void initialize() throws InitializationException
    {
        ListenerChain chain = new ListenerChain();
        setListenerChain(chain);

        // create the annotations generator
        annotationsGenerator = new AnnotationGeneratorChainingListener(selectionAlterer, chain);

        // chain'em all
        // Construct the listener chain in the right order. Listeners early in the chain are called before listeners
        // placed later in the chain.
        chain.addListener(this);

        // empty block listener is needed by the label generator
        chain.addListener(new GeneratorEmptyBlockChainingListener(chain));
        // link label generator generates events for link labels automatically generated for empty links
        // TODO: find a better way for this. Right now, the stream of events is modified by this link label generator,
        // which means that a xwiki 2.0 syntax renderer would be in trouble when trying to render after this generator,
        // since it will get word events for link labels. However, if the linkLabelGenerator is the generator which
        // always returns empty labels, rendering will happen as it would without it. With the exception that, for links
        // which are external uris, the label is not generated using the generator, so an empty labels generator would
        // still not do much. crap!
        chain.addListener(new LinkLabelGeneratorChainingListener(getLinkLabelGenerator(), plainTextParser, chain));
        // annotations generator, chained to map the annotations and maintain the annotations state while rendering
        chain.addListener((AnnotationGeneratorChainingListener) annotationsGenerator);
        // Following listeners are needed by the XHTML renderer
        chain.addListener(new BlockStateChainingListener(chain));
        chain.addListener(new EmptyBlockChainingListener(chain));
        chain.addListener(new MetaDataStateChainingListener(chain));
        // the actual annotations renderer
        chain.addListener(getAnnotationPrintRenderer(chain));
    }

    /**
     * @param chain the chain in which the renderer needs to be added.
     * @return the print renderer which should render the result with annotations on it
     */
    public abstract ChainingPrintRenderer getAnnotationPrintRenderer(ListenerChain chain);

    /**
     * Getter for the link label generator to be used for generating link labels in this mapping and rendering process
     * for links that don't have labels.
     * 
     * @return the {@link LinkLabelGenerator} used to generate labels for links without labels by this renderer
     */
    public abstract LinkLabelGenerator getLinkLabelGenerator();

    @Override
    public void setAnnotations(Collection<Annotation> annotations)
    {
        this.annotationsGenerator.setAnnotations(annotations);
    }
}
