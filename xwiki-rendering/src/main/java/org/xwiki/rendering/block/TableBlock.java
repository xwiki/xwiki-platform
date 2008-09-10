package org.xwiki.rendering.block;

import java.util.List;
import java.util.Map;

import org.xwiki.rendering.listener.Listener;

public class TableBlock extends AbstractFatherBlock
{
    public TableBlock(List<Block> list, Map<String, String> parameters)
    {
        super(list, parameters);
    }

    public void before(Listener listener)
    {
        listener.beginTable(getParameters());
    }

    public void after(Listener listener)
    {
        listener.endTable(getParameters());
    }
}
