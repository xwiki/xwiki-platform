package org.xwiki.rendering.parser;

import java.util.List;

public interface SyntaxFactory
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = SyntaxFactory.class.getName();

    Syntax createSyntaxFromIdString(String syntaxAsIdString) throws ParseException;

    /**
     * @return a list of all Syntaxes for which there's a {@link Parser} available
     */
    List<Syntax> getAvailableSyntaxes();
}
