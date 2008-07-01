package org.xwiki.rendering.doxia.internal;

import java.util.Map;

import org.apache.maven.doxia.sink.Sink;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.SectionLevel;

public class DoxiaGeneratorListener implements Listener
{
	private Sink sink;
	
	public DoxiaGeneratorListener(Sink sink)
	{
		this.sink = sink;
	}

	public void beginBold()
	{
		this.sink.bold();
	}

	public void beginDocument()
	{
		this.sink.body();
	}

	public void beginItalic()
	{
		this.sink.italic();
	}

	public void beginList(ListType listType)
	{
		if (listType == ListType.BULLETED) {
			this.sink.list();
		} else {
			// TODO: Handle other numerotations (Roman, etc)
			this.sink.numberedList(Sink.NUMBERING_DECIMAL);
		}
	}

	public void beginListItem()
	{
		this.sink.listItem();
	}

	public void beginMacroMarker(String name, Map<String, String> parameters, String content)
	{
		// Don't do anything since Doxia doesn't have macro markers and anyway we shouldn't
		// do anything.
	}

	public void beginParagraph()
	{
		this.sink.paragraph();
	}

	public void beginSection(SectionLevel level)
	{
		if (level == SectionLevel.LEVEL1) {
			this.sink.section1();
		} else if (level == SectionLevel.LEVEL2) {
			this.sink.section2();
		} else if (level == SectionLevel.LEVEL3) {
			this.sink.section3();
		} else if (level == SectionLevel.LEVEL4) {
			this.sink.section4();
		} else if (level == SectionLevel.LEVEL5) {
			this.sink.section5();
		} else if (level == SectionLevel.LEVEL6) {
			// There's no level 6 in Doxia!
			this.sink.section5();
		}
	}

	public void beginXMLElement(String name, Map<String, String> attributes)
	{
		// TODO: Find out what to do...
	}

	public void endBold()
	{
		this.sink.bold_();
	}

	public void endDocument()
	{
		this.sink.body_();
	}

	public void endItalic()
	{
		this.sink.italic_();
	}

	public void endList(ListType listType)
	{
		if (listType == ListType.BULLETED) {
			this.sink.list_();
		} else {
			this.sink.numberedList_();
		}
	}

	public void endListItem()
	{
		this.sink.listItem_();
	}

	public void endMacroMarker(String name, Map<String, String> parameters, String content)
	{
		// Don't do anything since Doxia doesn't have macro markers and anyway we shouldn't
		// do anything.
	}

	public void endParagraph()
	{
		this.sink.paragraph_();
	}

	public void endSection(SectionLevel level)
	{
		if (level == SectionLevel.LEVEL1) {
			this.sink.section1_();
		} else if (level == SectionLevel.LEVEL2) {
			this.sink.section2_();
		} else if (level == SectionLevel.LEVEL3) {
			this.sink.section3_();
		} else if (level == SectionLevel.LEVEL4) {
			this.sink.section4_();
		} else if (level == SectionLevel.LEVEL5) {
			this.sink.section5_();
		} else if (level == SectionLevel.LEVEL6) {
			// There's no level 6 in Doxia!
			this.sink.section5_();
		}
	}

	public void endXMLElement(String name, Map<String, String> attributes)
	{
		// TODO: Find out what to do...
	}

	public void onEscape(String escapedString)
	{
		// TODO: Doxia doesn't have any escape so we need to find some equivalent...
		this.sink.rawText(escapedString);
	}

	public void onLineBreak()
	{
		this.sink.lineBreak();
	}

	public void onLink(Link link)
	{
		// TODO: Finish the implementation
		this.sink.link(link.getReference());
	}

	public void onMacro(String name, Map<String, String> parameters, String content)
	{
		// Don't do anything since macros have already been transformed so this method 
		// should not be called.
	}

	public void onNewLine()
	{
		// Since there's no On NewLine event in Doxia we simply generate text
		this.sink.rawText("\n");
	}

	public void onSpace()
	{
		// Since there's no On Space event in Doxia we simply generate text
		this.sink.rawText(" ");
	}

	public void onSpecialSymbol(String symbol)
	{
		// Since there's no On Special Symbol event in Doxia we simply generate text
		this.sink.rawText(symbol);
	}

	public void onWord(String word)
	{
		this.sink.rawText(word);
	}
}
