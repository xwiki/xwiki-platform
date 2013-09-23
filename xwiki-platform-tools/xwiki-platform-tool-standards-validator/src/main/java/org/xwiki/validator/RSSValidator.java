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

import org.w3c.dom.Document;
import org.xwiki.validator.ValidationError.Type;
import org.xwiki.validator.framework.AbstractXMLValidator;
import org.xwiki.validator.framework.XMLErrorHandler;

import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.ParsingFeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * Validate provided input.
 * 
 * @version $Id$
 */
public class RSSValidator extends AbstractXMLValidator
{
    @Override
    protected XMLErrorHandler createXMLErrorHandler()
    {
        return new RSSErrorHandler();
    }

    @Override
    protected void validate(Document document)
    {
        try {
            SyndFeedInput input = new SyndFeedInput();
            input.build(getDocument());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (FeedException e) {
            if (e instanceof ParsingFeedException) {
                ParsingFeedException pfe = (ParsingFeedException) e;
                addError(Type.ERROR, pfe.getLineNumber(), pfe.getColumnNumber(), e.getMessage());
            } else {
                addError(Type.ERROR, -1, -1, e.getMessage());
            }
        }
    }

    @Override
    public String getName()
    {
        return "RSS";
    }
}
