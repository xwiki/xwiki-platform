package org.xwiki.rendering.block;

import java.util.List;
import java.util.Map;

import org.xwiki.rendering.listener.Listener;

public class TableHeadCellBlock extends TableCellBlock
{
    public TableHeadCellBlock(List<Block> list, Map<String, String> parameters)
    {
        super(list, parameters);
    }

    @Override
    public void before(Listener listener)
    {
        listener.beginTableHeadCell(getParameters());
    }

    @Override
    public void after(Listener listener)
    {
        listener.endTableHeadCell(getParameters());
    }
}
