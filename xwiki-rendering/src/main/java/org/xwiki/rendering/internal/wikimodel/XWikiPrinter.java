package org.xwiki.rendering.internal.wikimodel;

import java.io.PrintWriter;

import org.wikimodel.wem.IWikiPrinter;

public class XWikiPrinter implements IWikiPrinter 
{
	private PrintWriter writer;
	
	public XWikiPrinter(PrintWriter writer)
	{
		this.writer = writer;
	}
	
	public void print(String text)
	{
		this.writer.write(text);
	}

	public void println(String text)
	{
		this.writer.write(text + "\n");
	}
}
