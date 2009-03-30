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
package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.regex.Pattern;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.xwiki10.AbstractSyntaxFilter;

/**
 * @version $Id$
 * @since 1.8M1
 */
@Component("strikeout")
public class StrikeoutSyntaxFilter extends AbstractSyntaxFilter implements Initializable
{
    private static final Pattern UNDERLINESYNTAX_PATTERN =
        Pattern.compile("(?<![-!])--([^\\p{Space}>](?:[^-\n]*+|-)*?(?<=[^\\p{Space}]))--(?!-)");

    /**
     * {@inheritDoc}
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(1000);
    }

    public StrikeoutSyntaxFilter()
    {
        super(UNDERLINESYNTAX_PATTERN, "--");
    }
}
