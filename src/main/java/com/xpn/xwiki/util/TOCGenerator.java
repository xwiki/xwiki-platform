package com.xpn.xwiki.util;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.ListOrderedMap;

import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.XWikiContext;


public class TOCGenerator {
  public static final String TOC_DATA_NUMBERING = "numbering";
  public static final String TOC_DATA_LEVEL = "level";
  public static final String TOC_DATA_TEXT = "text";
  
  
  public static Map generateTOC(String content, int init, int max, boolean numbered, XWikiContext context) {
    OrderedMap tocData = ListOrderedMap.decorate(new HashMap());
    List processedHeadings = new ArrayList();
    int previousNumbers[] = { 0, 0, 0, 0, 0, 0, 0 };

    Pattern pattern = Pattern.compile("^[\\p{Space}]*(1(\\.1)*)[\\p{Space}]+(.*?)$", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      int level = (matcher.group(1).lastIndexOf("1") + 2) / 2;
      String text = matcher.group(3);
      
      int occurence = 0;
      for (Iterator iter = processedHeadings.iterator(); iter.hasNext();) if (iter.next().equals(text)) occurence++;

      String id = makeHeadingID (text, occurence, context);
      
      Map tocEntry = new HashMap();
      tocEntry.put(TOC_DATA_LEVEL, new Integer(level));
      tocEntry.put(TOC_DATA_TEXT, text);
      
      if (level >= init && level <= max) {
        if (numbered) {
          String number = "";
          int currentNumber = 0;
          for (int i = previousNumbers.length-1; i >= init; i--) {
            int num = 0;
            int previousNumber = previousNumbers[i];
            // if there is already a number previously assigned to a level
            if (previousNumber > 0) {
              // copy parent level from previous number
              num = previousNumber;
              if (i == level) {
                // increment the number if there was already previous number on the same leaf level
                num = previousNumber + 1;
              } else if (i > level) {
                //reset numbers of all deeper levels
                previousNumbers[i] = 0;
              }
            } else {
              num = 1;
              // incremet the previous number if there was already a number assigned
              // to any of the depper levels
              if (i < level) previousNumbers[i] = previousNumbers[i] + 1;
            }
  
            // construct the string representation of the number
            if (i <= level) {
              if ((number.length()) == 0) {
                // start new number
                number = num + number;
                currentNumber = num;
              } else {
                // append to the existing number
                number = num + "." + number;
              }
            }
          }
          // remeber the number for this leaf level
          previousNumbers[level] = currentNumber;
  
          tocEntry.put(TOC_DATA_NUMBERING, number);
        }
        tocData.put(id, tocEntry);
        processedHeadings.add(text);
      }
    }
    return tocData;
  }

  public static String makeHeadingID (String text, int occurence, XWikiContext context) {
    // Encode to convert unsafe chars
    text = Utils.encode(text.trim(), context);
        
    if (occurence > 0) {
      return text + "-" + occurence;
    } else {
      return text;
    }
  }
  
  
}