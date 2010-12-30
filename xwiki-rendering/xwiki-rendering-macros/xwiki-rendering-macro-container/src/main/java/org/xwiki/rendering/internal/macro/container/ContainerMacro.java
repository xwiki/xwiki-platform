package org.xwiki.rendering.internal.macro.container;

import java.io.StringReader;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.container.AbstractContainerMacro;
import org.xwiki.rendering.macro.container.ContainerMacroParameters;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Macro to hold a list groups and style them together, for example laying them out as indicated by the styleLayout
 * parameter. For the moment this macro handles only the layouting, and only the columns layout. When it will be
 * enhanced with other layout styles, it should be split in multiple classes, one to handle each.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component(ContainerMacro.MACRO_NAME)
public class ContainerMacro extends AbstractContainerMacro<ContainerMacroParameters>
{
    /**
     * The name of this macro.
     */
    public static final String MACRO_NAME = "container";

    /**
     * The description of this macro.
     */
    private static final String DESCRIPTION = "A macro to enclose multiple groups and add decoration, such as layout.";

    /**
     * Creates a container macro.
     */
    public ContainerMacro()
    {
        super("Container", DESCRIPTION, ContainerMacroParameters.class);
    }

    /**
     * Get the parser of the desired wiki syntax.
     * 
     * @param syntaxId the syntax to get the parser for
     * @return the parser of the current wiki syntax
     * @throws MacroExecutionException Failed to find source parser
     */
    protected Parser getSyntaxParser(String syntaxId) throws MacroExecutionException
    {
        try {
            return (Parser) getComponentManager().lookup(Parser.class, syntaxId);
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException("Failed to find source parser", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.container.AbstractContainerMacro
     *      #getContent(org.xwiki.rendering.macro.container.ContainerMacroParameters, java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    @Override
    protected List<Block> getContent(ContainerMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        XDOM parsedDom;

        // get a parser for the desired syntax identifier
        Parser parser = getSyntaxParser(context.getSyntax().toIdString());

        try {
            // parse the content of the wiki macro that has been injected by the component manager
            parsedDom = parser.parse(new StringReader(content == null ? "" : content));
        } catch (ParseException e) {
            throw new MacroExecutionException("Failed to parse content [" + content + "] with Syntax parser ["
                + parser.getSyntax() + "]", e);
        }

        return parsedDom.getChildren();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#getPriority()
     */
    @Override
    public int getPriority()
    {
        return 750;
    }

}
