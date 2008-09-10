package org.xwiki.rendering.block;

import java.util.List;
import java.util.Map;

import org.xwiki.rendering.listener.Listener;

public class TableCellBlock extends AbstractFatherBlock
{
    public TableCellBlock(List<Block> list, Map<String, String> parameters)
    {
        super(list, parameters);
    }

    public void before(Listener listener)
    {
        listener.beginTableCell(getParameters());
    }

    public void after(Listener listener)
    {
        listener.endTableCell(getParameters());
    }
}
