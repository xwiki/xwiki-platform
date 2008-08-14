package org.xwiki.rendering.block;

import org.xwiki.rendering.listener.Listener;

/**
 * Represents an implicit new line triggered when the new line character is found ("\n"). Note
 * that this is different from a line break which is explicitely specified in wiki syntax.
 *
 * @version $Id: $
 * @since 1.5M2
 * @see LineBreakBlock
 */
public class NewLineBlock extends AbstractBlock
{
    public void traverse(Listener listener)
    {
        listener.onLineBreak();
    }
}
