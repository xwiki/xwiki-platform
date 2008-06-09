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
package org.xwiki.rendering.parser;

import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.logging.AbstractLogEnabled;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class DefaultSyntaxFactory extends AbstractLogEnabled implements SyntaxFactory, Composable, Initializable
{
    private static final Pattern SYNTAX_PATTERN = Pattern.compile("(.*)\\/(.*)");

    private ComponentManager componentManager;

    private List<Syntax> syntaxes;

    /**
     * {@inheritDoc}
     * @see Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        List<Syntax> syntaxes = new ArrayList<Syntax>();
        List<Parser> parsers;
        try {
            parsers = this.componentManager.lookupList(Parser.ROLE);
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to lookup the list of available Syntaxes", e);
        }

        for (Parser parser: parsers) {
            syntaxes.add(parser.getSyntax());
        }

        this.syntaxes = syntaxes;
    }

    public Syntax createSyntaxFromIdString(String syntaxIdAsString) throws ParseException
    {
        Matcher matcher = SYNTAX_PATTERN.matcher(syntaxIdAsString);
        if (!matcher.matches()) {
            throw new ParseException("Failed to parse Syntax string [" + syntaxIdAsString + "]");
        }

        String syntaxId = matcher.group(1);
        String version = matcher.group(2);

        SyntaxType syntaxType;
        if (syntaxId.equalsIgnoreCase(SyntaxType.XWIKI.toIdString())) {
            syntaxType = SyntaxType.XWIKI;
        } else if (syntaxId.equalsIgnoreCase(SyntaxType.CONFLUENCE.toIdString())) {
            syntaxType = SyntaxType.CONFLUENCE;
        } else if (syntaxId.equalsIgnoreCase(SyntaxType.CREOLE.toIdString())) {
            syntaxType = SyntaxType.CREOLE;
        } else if (syntaxId.equalsIgnoreCase(SyntaxType.JSPWIKI.toIdString())) {
            syntaxType = SyntaxType.JSPWIKI;
        } else if (syntaxId.equalsIgnoreCase(SyntaxType.MEDIAWIKI.toIdString())) {
            syntaxType = SyntaxType.MEDIAWIKI;
        } else if (syntaxId.equalsIgnoreCase(SyntaxType.TWIKI.toIdString())) {
            syntaxType = SyntaxType.TWIKI;
        } else {
            throw new ParseException("Unknown Syntax id [" + syntaxId + "]. Valid syntaxes are [xwiki] and "
                + "[confluence]");
        }

        return new Syntax(syntaxType, version);
    }

    public List<Syntax> getAvailableSyntaxes()
    {
        return this.syntaxes;
    }
}
