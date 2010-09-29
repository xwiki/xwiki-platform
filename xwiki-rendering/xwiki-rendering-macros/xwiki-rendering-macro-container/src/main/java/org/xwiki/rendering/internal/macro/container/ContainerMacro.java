package org.xwiki.rendering.internal.macro.container;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.container.ContainerMacroParameters;
import org.xwiki.rendering.macro.container.LayoutManager;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Macro to hold a list groups and style them together, for example laying them out as indicated by the styleLayout
 * parameter. For the moment this macro handles only the layouting, and only the columns layout. When it will be
 * enhanced with other layout styles, it should be split in multiple classes, one to handle each
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component(ContainerMacro.MACRO_NAME)
public class ContainerMacro extends AbstractMacro<ContainerMacroParameters>
{
    /**
     * The name of this macro.
     */
    public static final String MACRO_NAME = "container";

    /**
     * The name of the parameter to convey style information to the HTML (html style attribute).
     */
    private static final String PARAMETER_STYLE = "style";

    /**
     * The description of this macro.
     */
    private static final String DESCRIPTION = "A macro to enclose multiple groups and add decoration, such as layout.";

    /**
     * The component manager used to dynamically fetch components (syntax parsers, in this case).
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Creates a container macro.
     */
    public ContainerMacro()
    {
        super("Container", DESCRIPTION, ContainerMacroParameters.class);
    }

    /**
     * {@inheritDoc}
     */
    public List<Block> execute(ContainerMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        XDOM parsedDom;

        // get a parser for the desired syntax identifier
        Parser parser = getSyntaxParser(context.getSyntax().toIdString());

        try {
            // parse the content of the wiki macro that has been injected by the
            // component manager
            parsedDom = parser.parse(new StringReader(content == null ? "" : content));
        } catch (ParseException e) {
            throw new MacroExecutionException("Failed to parse content [" + content + "] with Syntax parser ["
                + parser.getSyntax() + "]", e);
        }

        // transform the container in a group, with appropriate parameters
        Map<String, String> containerParameters = new HashMap<String, String>();
        if (parameters.isJustify()) {
            containerParameters.put(PARAMETER_STYLE, "text-align: justify;");
        }

        // create the root block for the container macro, as a group block, and add all the blocks resulted from parsing
        // its content
        Block containerRoot = new GroupBlock(containerParameters);
        containerRoot.addChildren(parsedDom.getChildren());

        // grab the layout manager to layout this container
        LayoutManager layoutManager = getLayoutManager(parameters.getLayoutStyle());
        // if a suitable layout manager was found, layout this container
        if (layoutManager != null) {
            layoutManager.layoutContainer(containerRoot);
        }

        // and finally return the styled container root
        return Collections.singletonList(containerRoot);
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
            return (Parser) this.componentManager.lookup(Parser.class, syntaxId);
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException("Failed to find source parser", e);
        }
    }

    /**
     * @param layoutStyle the style passed to the container component
     * @return the layout manager to do the layouting according to the specified layout style
     */
    protected LayoutManager getLayoutManager(String layoutStyle)
    {
        try {
            return componentManager.lookup(LayoutManager.class, layoutStyle);
        } catch (ComponentLookupException e) {
            // TODO: maybe should log?
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean supportsInlineMode()
    {
        return false;
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
