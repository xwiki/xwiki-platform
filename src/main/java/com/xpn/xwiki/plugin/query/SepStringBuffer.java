package com.xpn.xwiki.plugin.query;

/** StringBuffer with Separator */
public class SepStringBuffer {
	String _separator;
	String sep = "";
	public StringBuffer sb;
	public SepStringBuffer(String separator) {		
		this._separator = separator;
		sb = new StringBuffer();
	}
	public SepStringBuffer(String text, String separator) {		
		this._separator = separator;
		sb = new StringBuffer(text);
	}
	public final SepStringBuffer appendSeparator() {
		sb.append(sep);
		sep = _separator;
		return this;
	}
	
	public int length() { return sb.length(); }
	
	public String toString() { return sb.toString(); }
	
	public final SepStringBuffer append(String s) { sb.append(s); return this; }	
	public final SepStringBuffer append(Object s) { sb.append(s); return this; }
	public final SepStringBuffer append(double v) { sb.append(String.valueOf(v)); return this; }	
	public final SepStringBuffer append(long v)   { sb.append(String.valueOf(v)); return this; }
	public final SepStringBuffer append(char c)   { sb.append(c); return this; }
	public final SepStringBuffer appendWithSep(String s) { appendSeparator(); sb.append(s); return this; }
}
