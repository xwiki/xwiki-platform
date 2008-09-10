package org.xwiki.rendering.block;

import java.util.List;
import java.util.Map;

import org.xwiki.rendering.listener.Listener;

public class TableRowBlock extends AbstractFatherBlock
{
    public TableRowBlock(List<Block> list, Map<String, String> parameters)
    {
        super(list, parameters);
    }

    public void before(Listener listener)
    {
        listener.beginTableRow(getParameters());
    }

    public void after(Listener listener)
    {
        listener.endTableRow(getParameters());
    }
}
