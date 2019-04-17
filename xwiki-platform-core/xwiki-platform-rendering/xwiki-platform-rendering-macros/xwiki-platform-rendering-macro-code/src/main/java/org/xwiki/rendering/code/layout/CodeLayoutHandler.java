package org.xwiki.rendering.code.layout;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;

@Role
public interface CodeLayoutHandler {
	List<Block> layout(List<Block> blocks, String originalContent);
}
