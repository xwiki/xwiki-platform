package org.xwiki.rendering.macro.code;

public enum CodeMacroLayout {
	PLAIN(Constants.PLAIN_HINT),
	LINE_NUM(Constants.LINE_NUM_HINT);
	
	private String hint;
	
	private CodeMacroLayout(String hint) {
		this.hint = hint;
	}
	
	public final String getHint() {
		return hint;
	}
	
	public static class Constants {
		public static final String PLAIN_HINT = "plain";
		public static final String LINE_NUM_HINT = "line_num";
	}
}
