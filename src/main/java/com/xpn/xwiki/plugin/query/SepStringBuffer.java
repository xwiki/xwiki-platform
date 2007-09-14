/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package com.xpn.xwiki.plugin.query;

/** StringBuffer with Separator */
public class SepStringBuffer {
	final String _separator;
	String cursep = "";
	public final StringBuffer sb;
	public SepStringBuffer(String separator) {		
		this._separator = separator;
		sb = new StringBuffer();
	}
	public SepStringBuffer(String text, String separator) {		
		this._separator = separator;
		sb = new StringBuffer(text);
	}
	public final SepStringBuffer appendSeparator() {
		sb.append(cursep);
		cursep = _separator;
		return this;
	}
	
	public final int length() { return sb.length(); }
	
	public final String toString() { return sb.toString(); }
	
	public final SepStringBuffer append(String s) { sb.append(s); return this; }	
	public final SepStringBuffer append(Object s) { sb.append(s); return this; }
	public final SepStringBuffer append(double v) { sb.append(String.valueOf(v)); return this; }	
	public final SepStringBuffer append(long v)   { sb.append(String.valueOf(v)); return this; }
	public final SepStringBuffer append(char c)   { sb.append(c); return this; }
	public final SepStringBuffer appendWithSep(String s) { appendSeparator(); sb.append(s); return this; }
}
