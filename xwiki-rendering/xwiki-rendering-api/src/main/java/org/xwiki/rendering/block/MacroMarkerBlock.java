package org.xwiki.rendering.block;

import org.xwiki.rendering.listener.Listener;

import java.util.List;
import java.util.Map;

/**
 * A special block that Macro Blocks emits when they are executed so that it's possible to reconstruct
 * the initial syntax even after Macros have been executed.
 */
public class MacroMarkerBlock extends AbstractFatherBlock
{
    private String name;

    private Map<String, String> parameters;

    private String content;

    public MacroMarkerBlock(String name, Map<String, String> parameters, List<Block> childBlocks)
    {
        this(name, parameters, null, childBlocks);
    }

    public MacroMarkerBlock(String name, Map<String, String> parameters, String content, List<Block> childBlocks)
    {
        super(childBlocks);
        this.name = name;
        this.parameters = parameters;
        this.content = content;
    }

    public String getName()
    {
        return this.name;
    }

    public Map<String, String> getParameters()
    {
        return this.parameters;
    }

    public String getContent()
    {
        return this.content;
    }
    
    public void before(Listener listener)
    {
        listener.beginMacroMarker(getName(), getParameters(), getContent());
    }

    public void after(Listener listener)
    {
        listener.endMacroMarker(getName(), getParameters(), getContent());
    }
}
