package com.xpn.xwiki.render.filter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.filter.CacheFilter;
import org.radeox.filter.context.FilterContext;
import org.radeox.filter.regex.LocaleRegexTokenFilter;
import org.radeox.regex.MatchResult;

import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;
import com.xpn.xwiki.util.TOCGenerator;
import com.xpn.xwiki.XWikiContext;


/**
 * A customized version of Radeox Heading Filter
 */
public class XWikiHeadingFilter extends LocaleRegexTokenFilter implements CacheFilter {
  private final String TOC_NUMBERED = "tocNumbered";
  private final String TOC_DATA = "tocData";
  
  private MessageFormat formatter;


  protected String getLocaleKey() {
    return "filter.heading";
  }

  public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context) {
    buffer.append(handleMatch(result, context));
  }

  public void setInitialContext(InitialRenderContext context) {
    super.setInitialContext(context);
    String outputTemplate = outputMessages.getString(getLocaleKey()+".print");
    formatter = new MessageFormat("");
    formatter.applyPattern(outputTemplate);
 }

  public String handleMatch(MatchResult result, FilterContext context) {
    String id = null;
    String level = result.group(1);
    String text = result.group(3);
    String numbering = "";
    
    RenderContext rcontext = context.getRenderContext();
    XWikiContext xcontext  = ((XWikiRadeoxRenderEngine) rcontext.getRenderEngine()).getContext();
    
    // generate unique ID of the heading  
    List processedHeadings = (List) rcontext.get("processedHeadings");
    if (processedHeadings == null) {
      processedHeadings = new ArrayList();
      rcontext.set("processedHeadings", processedHeadings);
    }
    int occurence = 0;
    for (Iterator iter = processedHeadings.iterator(); iter.hasNext();) if (iter.next().equals(text)) occurence++;
    id = TOCGenerator.makeHeadingID(text, occurence, xcontext);
    processedHeadings.add(text);
    
    //  add numbering if the flag is set
    if (xcontext.containsKey(TOC_NUMBERED) && ((Boolean)xcontext.get(TOC_NUMBERED)).booleanValue()) {
      if (xcontext.containsKey(TOC_DATA)) {
        Map tocEntry = (Map) ((Map) xcontext.get(TOC_DATA)).get(id);
        if (tocEntry != null) numbering = (String) tocEntry.get(TOCGenerator.TOC_DATA_NUMBERING) + " ";
      }
    }
    
    return formatter.format(new Object[]{id, level.replace('.', '-'), numbering, text});
  } 
}
