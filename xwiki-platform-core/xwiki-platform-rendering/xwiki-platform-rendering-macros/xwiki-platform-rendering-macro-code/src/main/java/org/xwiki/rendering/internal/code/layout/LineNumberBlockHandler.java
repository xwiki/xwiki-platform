package org.xwiki.rendering.internal.code.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.code.layout.CodeLayoutHandler;
import org.xwiki.rendering.macro.code.CodeMacroLayout;

@Component
@Named(CodeMacroLayout.Constants.LINE_NUM_HINT)
public class LineNumberBlockHandler implements CodeLayoutHandler {
	private Pattern linePattern = Pattern.compile("\r?\n");
	
	@Override
	public List<Block> layout(List<Block> blocks, String originalContent) {
		Matcher matcher = linePattern.matcher(originalContent);
		int lineCount = 1;
		while (matcher.find())
			lineCount++;
		
		List<Block> lineBlocks = new ArrayList<Block>(lineCount * 2);
    	for (int i = 0; i < lineCount; i++) {
    		lineBlocks.add(new WordBlock(Integer.toString(i + 1)));
    		lineBlocks.add(new NewLineBlock());
    	}
    	GroupBlock lineNumbers = new GroupBlock(lineBlocks);
    	lineNumbers.setParameter("class", "linenos");
    	
    	GroupBlock codeBlocks = new GroupBlock(blocks);
    	
    	GroupBlock wrapper = new GroupBlock(Arrays.asList(lineNumbers, codeBlocks));
    	wrapper.setParameter("class", "linenoswrapper");
    	return Arrays.<Block> asList(wrapper);
	}
}
