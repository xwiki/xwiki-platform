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

import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.internal.renderer.ParametersPrinter;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer;

/**
 * Serialize a Resource Reference into XHTML comments using the syntax
 * {@code (isTyped)|-|(type)|-|(reference)|-|(parameters: key="value")}. This is used for example to save a Link or
 * Image Reference in XHTML Comment in the Annotated XHTML Renderer.
 *
 * @version $Id$
 * @since 2.5RC1
 */
@Component("xhtmlmarker")
public class XHTMLMarkerResourceReferenceSerializer implements ResourceReferenceSerializer
{
    /**
     * Character to separate Link reference and parameters in XHTML comments.
     */
    private static final String COMMENT_SEPARATOR = "|-|";

    /**
     * Used to print Link Parameters in XHTML comments.
     */
    private ParametersPrinter parametersPrinter = new ParametersPrinter();

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer#serialize(ResourceReference)
     */
    public String serialize(ResourceReference reference)
    {
        StringBuilder buffer = new StringBuilder();

        // Print if the Resource Reference is typed, the Resource Reference Type and the Reference itself
        buffer.append(reference.isTyped());
        buffer.append(COMMENT_SEPARATOR);
        buffer.append(reference.getType().getScheme());
        buffer.append(COMMENT_SEPARATOR);
        buffer.append(reference.getReference());

        // Print Resource Reference parameters. We need to do this so that the XHTML parser doesn't have
        // to parse the query string to extract the parameters. Doing so could lead to false result since
        // for example the XHTML renderer can add a parent parameter in the query string for links to non
        // existing documents.
        //
        // Also note that we don't need to print Resource Reference parameters since they are added as XHTML class
        // attributes by the XHTML Renderer and thus the XHTML parser will be able to get them again as attributes.
        Map<String, String> linkReferenceParameters = reference.getParameters();
        if (!linkReferenceParameters.isEmpty()) {
            buffer.append(COMMENT_SEPARATOR);
            buffer.append(this.parametersPrinter.print(linkReferenceParameters, '\\'));
        }

        return buffer.toString();
    }
}
