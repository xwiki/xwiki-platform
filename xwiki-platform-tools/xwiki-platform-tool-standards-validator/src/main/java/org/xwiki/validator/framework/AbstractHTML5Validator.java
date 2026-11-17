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
package org.xwiki.validator.framework;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.xwiki.validator.ValidationError;

/**
 * @version $Id$
 */
public abstract class AbstractHTML5Validator extends AbstractXMLValidator
{
    /**
     * Href attribute.
     */
    protected static final String ATTR_HREF = "href";

    /**
     * Source attribute.
     */
    protected static final String ATTR_SRC = "src";

    /**
     * Document parsed with JSoup.
     */
    protected Document html5Document;

    @Override
    public void setDocument(InputStream document)
    {
        setHTML5Document(document);
    }

    /**
     * Set document to validate.
     *
     * @param document document to validate
     */
    public void setHTML5Document(InputStream document)
    {
        this.html5Document = null;

        if (document != null) {
            try {
                this.html5Document = Jsoup.parse(document, null, "");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return the parsed html5 document
     */
    public Document getHTML5Document()
    {
        return this.html5Document;
    }

    @Override
    public List<ValidationError> validate()
    {
        if (this.html5Document == null) {
            return this.errorHandler.getErrors();
        }

        this.errorHandler.clear();

        validate(this.document);

        return this.errorHandler.getErrors();
    }

    /**
     * Asserts that a condition is true. If it isn't it puts an error message in the validation results.
     *
     * @param errorType type of the error
     * @param message the message to add
     * @param condition condition to be checked
     */
    protected void assertTrue(ValidationError.Type errorType, String message, boolean condition)
    {
        if (!condition) {
            // TODO: handle line/column
            addError(errorType, -1, -1, message);
        }
    }

    /**
     * Get all the elements matching one of the given tags.
     *
     * @param tagNames tag names to match
     * @return the list of matching tags
     */
    public Elements getElements(Collection<String> tagNames)
    {
        Elements elements = new Elements();
        for (String tagName : tagNames) {
            elements.addAll(this.html5Document.getElementsByTag(tagName));
        }
        return elements;
    }
}
