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
package org.xwiki.validator;

import java.util.Arrays;
import java.util.List;

import org.jsoup.nodes.Element;
import org.w3c.dom.Document;
import org.xwiki.validator.ValidationError.Type;
import org.xwiki.validator.framework.AbstractHTML5Validator;

/**
 * Validator allowing to validate (X)HTML content against some XWiki rules.
 * 
 * @version $Id$
 */
public class XWikiValidator extends AbstractHTML5Validator
{
    @Override
    protected void validate(Document document)
    {
        validateFailingMacros();
        validateNotEmptyHeadingId();
        validateUrlOrResourcesWithoutDoubleInterrogationMark();
    }

    /**
     * Check if there is any rendering error in the generated XHTML.
     */
    public void validateFailingMacros()
    {
        assertTrue(Type.ERROR, "Found rendering error",
            this.html5Document.getElementsByClass("xwikirenderingerror").isEmpty());
    }

    /**
     * Check if there is any heading with empty (generally generated) id value.
     */
    public void validateNotEmptyHeadingId()
    {
        List<String> headings = Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6");

        for (Element element : getElements(headings)) {
            String id = element.attr("id");

            // Verify that no heading contains an empty id.
            assertTrue(Type.ERROR, "A " + element.tagName() + " heading with empty id (\"H\") has been found",
                id == null || !id.equals("H"));
        }
    }

    private void checkQueryParameter(String attribute, String failureMessage)
    {
        assertTrue(Type.ERROR, failureMessage, attribute.split("\\?").length < 3);
    }

    /**
     * Ensure that no URL or resource link contains two "?".
     */
    public void validateUrlOrResourcesWithoutDoubleInterrogationMark()
    {
        for (Element link : getHTML5Document().getElementsByAttribute(ATTR_HREF)) {
            String errorMessage = String.format("A link contain two query parameter delimiter '?' (url: %s)",
                link.attr(ATTR_HREF));
            checkQueryParameter(link.attr(ATTR_HREF), errorMessage);
        }

        for (Element resource : getHTML5Document().getElementsByAttribute(ATTR_SRC)) {
            String errorMessage = String.format("A resource %s contain two query parameter delimiter '?' (url: %s)",
                resource.tagName(), resource.attr(ATTR_SRC));
            checkQueryParameter(resource.attr(ATTR_SRC), errorMessage);
        }
    }

    @Override
    public String getName()
    {
        return "XWiki";
    }
}
