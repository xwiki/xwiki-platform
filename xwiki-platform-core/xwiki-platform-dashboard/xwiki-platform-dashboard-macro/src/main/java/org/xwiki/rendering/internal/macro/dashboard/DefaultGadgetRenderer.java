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
package org.xwiki.rendering.internal.macro.dashboard;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.macro.dashboard.Gadget;
import org.xwiki.rendering.macro.dashboard.GadgetRenderer;

/**
 * Default implementation of the gadget renderer, rendering the title of the gadget in a level 2 heading and the content
 * as is, in a container that helps group it together and separate from title.
 * 
 * @version $Id$
 * @since 3.0rc1
 */
@Component
@Singleton
public class DefaultGadgetRenderer implements GadgetRenderer
{
    /**
     * The HTML class attribute name.
     */
    protected static final String CLASS = "class";

    /**
     * The HTML id attribute name.
     */
    protected static final String ID = "id";

    @Inject
    @Named("empty")
    private XDOMChecker emptyXDOMChecker;

    @Override
    public List<Block> decorateGadget(Gadget gadget)
    {
        List<Block> result;

        // We only decorate the gadget if it has some content. This allows to dynamically decide whether to display
        // a gadget or not.
        if (!this.emptyXDOMChecker.check(gadget.getContent())) {
            // prepare the title of the gadget, in a heading 2
            HeaderBlock titleBlock = new HeaderBlock(gadget.getTitle(), HeaderLevel.LEVEL2);
            titleBlock.setParameter(CLASS, "gadget-title");

            // And then the content wrapped in a group block with class, to style it
            GroupBlock contentGroup = new GroupBlock();
            contentGroup.setParameter(CLASS, "gadget-content");
            contentGroup.addChildren(gadget.getContent());

            // and wrap everything in a container, to give it a class
            GroupBlock gadgetBlock = new GroupBlock();
            String idPrefix = "gadget";
            gadgetBlock.setParameter(CLASS, idPrefix);
            // put an underscore here because it doesn't hurt at this level and it helps scriptaculous on the frontend
            gadgetBlock.setParameter(ID, idPrefix + "_" + gadget.getId());
            gadgetBlock.addChild(titleBlock);
            gadgetBlock.addChild(contentGroup);

            result = Collections.singletonList(gadgetBlock);
        } else {
            result = Collections.emptyList();
        }

        return result;
    }
}
