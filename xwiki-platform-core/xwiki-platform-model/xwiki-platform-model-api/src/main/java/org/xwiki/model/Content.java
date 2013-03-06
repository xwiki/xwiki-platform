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
package org.xwiki.model;

import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

/**
 * Represents some content along with the syntax in which it's written.
 *
 * Note that this class is immutable (text and syntax cannot be changed once set).
 *
 * @version $Id$
 * @since 5.0M2
 */
@Unstable
public class Content
{
    private final String text;

    private final Syntax syntax;

    public Content(String text, Syntax syntax)
    {
        this.text = text;
        this.syntax = syntax;
    }

    public String getText()
    {
        return this.text;
    }

    public Syntax getSyntax()
    {
        return this.syntax;
    }

    @Override
    public String toString()
    {
        return "text = [" + getText() + "], Syntax = [" +getSyntax() + "]";
    }

    @Override
    public int hashCode()
    {
        // Random number. See http://www.technofundo.com/tech/java/equalhash.html for the detail of this
        // algorithm.
        int hash = 8;
        hash = 31 * hash + (null == getText() ? 0 : getText().hashCode());
        hash = 31 * hash + (null == getSyntax() ? 0 : getSyntax().hashCode());
        return hash;
    }

    @Override
    public boolean equals(java.lang.Object object)
    {
        boolean result;

        // See http://www.technofundo.com/tech/java/equalhash.html for the detail of this algorithm.
        if (this == object) {
            result = true;
        } else {
            if ((object == null) || (object.getClass() != this.getClass())) {
                result = false;
            } else {
                // object must be Content at this point
                Content content = (Content) object;
                result =(getText() == content.getText() || (getText() != null
                    && getText().equals(content.getText())))
                        && (getSyntax() == content.getSyntax() || (getSyntax() != null && getSyntax().equals(
                            content.getSyntax())));
            }
        }
        return result;
    }
}
