package org.xwiki.localization;

import org.xwiki.rendering.block.Block;

public interface Translation
{
    Bundle getBundle();

    String getKey();

    Block render(Object... parameters);
}
