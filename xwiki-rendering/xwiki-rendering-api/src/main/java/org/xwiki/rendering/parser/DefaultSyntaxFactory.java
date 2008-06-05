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
