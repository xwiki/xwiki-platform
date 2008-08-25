package org.xwiki.rendering.macro.xhtml;

public class XHTMLMacroParameters
{
    /**
     * Indicate if the user has asked to escape wiki syntax or not.
     */
    boolean escapeWikiSyntax;

    /**
     * @return indicate if the user has asked to escape wiki syntax or not.
     */
    public boolean isEscapeWikiSyntax()
    {
        return this.escapeWikiSyntax;
    }

    /**
     * @param escapeWikiSyntax indicate if the user has asked to escape wiki syntax or not.
     */
    public void setEscapeWikiSyntax(boolean escapeWikiSyntax)
    {
        this.escapeWikiSyntax = escapeWikiSyntax;
    }
}
