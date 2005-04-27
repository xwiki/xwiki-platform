package com.xpn.xwiki.util;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.ListOrderedMap;


public class TOCGenerator {
  public static final String TOC_DATA_NUMBERING = "numbering";
  public static final String TOC_DATA_LEVEL = "level";
  public static final String TOC_DATA_TEXT = "text";
  
  public static void main(String args[]) {
    TOCGenerator.testLevel1();
    TOCGenerator.testLevel2();
    TOCGenerator.testLevel3();
  }
  
  private static void testLevel1() {
    String content = "1.1 a\n1.1 b\n1.1.1 c\n1.1 d\n1 a\n1.1.1.1 f\n1.1.1.1 g\n1.1 h\n1.1 i";
    Map result = TOCGenerator.generateTOC(content, 1, 6, true);
    System.out.println(result);
    assert (((Map) result.get("a")).get(TOC_DATA_NUMBERING)).equals("1.1");
    assert ((Map) result.get("b")).get(TOC_DATA_NUMBERING).equals("1.2");
    assert ((Map) result.get("c")).get(TOC_DATA_NUMBERING).equals("1.2.1");
    assert ((Map) result.get("d")).get(TOC_DATA_NUMBERING).equals("1.3");
    assert ((Map) result.get("a-1")).get(TOC_DATA_NUMBERING).equals("2");
    assert ((Map) result.get("f")).get(TOC_DATA_NUMBERING).equals("2.1.1.1");
    assert ((Map) result.get("g")).get(TOC_DATA_NUMBERING).equals("2.1.1.2");
    assert ((Map) result.get("h")).get(TOC_DATA_NUMBERING).equals("2.2");
    assert (((Map) result.get("i")).get(TOC_DATA_NUMBERING)).equals("2.3");
  }
  
  private static void testLevel2() {
    String content = "1.1 a\n1.1 b\n1.1.1 c\n1.1 d\n1 e\n1.1.1.1 f\n1.1.1.1 g\n1.1 h\n1.1 i";
    Map result = TOCGenerator.generateTOC(content, 2, 6, true);
    System.out.println(result);
    assert (((Map) result.get("a")).get(TOC_DATA_NUMBERING)).equals("1");
    assert ((Map) result.get("b")).get(TOC_DATA_NUMBERING).equals("2");
    assert ((Map) result.get("c")).get(TOC_DATA_NUMBERING).equals("2.1");
    assert ((Map) result.get("d")).get(TOC_DATA_NUMBERING).equals("3");
    assert result.get("a-1") == null; 
    assert ((Map) result.get("f")).get(TOC_DATA_NUMBERING).equals("3.1.1");
    assert ((Map) result.get("g")).get(TOC_DATA_NUMBERING).equals("3.1.2");
    assert ((Map) result.get("h")).get(TOC_DATA_NUMBERING).equals("4");
    assert (((Map) result.get("i")).get(TOC_DATA_NUMBERING)).equals("5");
  }
  
  private static void testLevel3() {
    String content = "1.1 a\n1.1 b\n1.1.1 c\n1.1 d\n1 a\n1.1.1.1 f\n1.1.1.1 g\n1.1 h\n1.1 i";
    Map result = TOCGenerator.generateTOC(content, 1, 3, true);
    System.out.println(result);
    assert (((Map) result.get("a")).get(TOC_DATA_NUMBERING)).equals("1.1");
    assert ((Map) result.get("b")).get(TOC_DATA_NUMBERING).equals("1.2");
    assert ((Map) result.get("c")).get(TOC_DATA_NUMBERING).equals("1.2.1");
    assert ((Map) result.get("d")).get(TOC_DATA_NUMBERING).equals("1.3");
    assert ((Map) result.get("a-1")).get(TOC_DATA_NUMBERING).equals("2");
    assert result.get("f") == null;
    assert result.get("g") == null;
    assert ((Map) result.get("h")).get(TOC_DATA_NUMBERING).equals("2.1");
    assert (((Map) result.get("i")).get(TOC_DATA_NUMBERING)).equals("2.2");
  }
  
  public static Map generateTOC(String content, int init, int max, boolean numbered) {
    int last = 3; 
    OrderedMap tocData = ListOrderedMap.decorate(new HashMap());
    List processedHeadings = new ArrayList();
    int previousNumbers[] = { 0, 0, 0, 0, 0, 0 };

    Pattern pattern = Pattern.compile("^[\\p{Space}]*(1(\\.1)*)[\\p{Space}]+(.*?)$", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      int level = (matcher.group(1).lastIndexOf("1") + 2) / 2;
      String text = matcher.group(3);
      
      int occurence = 0;
      for (Iterator iter = processedHeadings.iterator(); iter.hasNext();) if (iter.next().equals(text)) occurence++;

      String id = makeHeadingID (text, occurence);
      
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

  public static String makeHeadingID (String text, int occurence) {
    if (occurence > 0) {
      return text + "-" + occurence;
    } else {
      return text;
    }
  }
  
  
}