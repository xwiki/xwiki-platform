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
package org.xwiki.gwt.wysiwyg.client.plugin.link;

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigJSONParser;

/**
 * Creates link configuration objects based on the current selection in the rich text area.
 * 
 * @version $Id$
 */
public class LinkConfigFactory
{
    /**
     * The rich text area whose selection is used to create the link configuration objects.
     */
    private final RichTextArea rta;

    /**
     * The object used to create a {@link LinkConfig} from JSON.
     */
    private final LinkConfigJSONParser linkConfigJSONParser = new LinkConfigJSONParser();

    /**
     * The object used to parse image configuration from JSON.
     */
    private final ImageConfigJSONParser imageConfigJSONParser = new ImageConfigJSONParser();

    /**
     * Creates a new factory for {@link LinkConfig} objects that can be used to create or edit links inside the given
     * rich text area.
     * 
     * @param rta the target rich text area
     */
    public LinkConfigFactory(RichTextArea rta)
    {
        this.rta = rta;
    }

    /**
     * @return a new link configuration object based on the current selection in the underlying rich text area
     */
    public LinkConfig createLinkConfig()
    {
        String linkJSON = rta.getCommandManager().getStringValue(Command.CREATE_LINK);
        if (linkJSON != null) {
            // Edit link.
            return linkConfigJSONParser.parse(linkJSON);
        }

        // Insert link.
        LinkConfig linkConfig = new LinkConfig();
        Range range = rta.getDocument().getSelection().getRangeAt(0);
        linkConfig.setLabel(range.toHTML());
        // Check the special case when the selection is an image and add a link on an image.
        String imageJSON = rta.getCommandManager().getStringValue(Command.INSERT_IMAGE);
        if (imageJSON != null) {
            // It's an image selection. Set the label read only and put the image reference in the label text.
            linkConfig.setLabelText(imageConfigJSONParser.parse(imageJSON).getReference());
            linkConfig.setReadOnlyLabel(true);
        } else {
            linkConfig.setLabelText(range.toString());
        }
        return linkConfig;
    }
}
