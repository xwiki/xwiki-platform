package org.xwiki.rendering.internal.code.layout;

import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.code.layout.CodeLayoutHandler;
import org.xwiki.rendering.macro.code.CodeMacroLayout;

@Component
@Named(CodeMacroLayout.Constants.PLAIN_HINT)
public class PlainLayoutHandler implements CodeLayoutHandler {
	@Override
	public List<Block> layout(List<Block> blocks, String originalContent) {
		return blocks;
	}
}
