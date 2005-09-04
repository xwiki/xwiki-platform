package com.xpn.xwiki.plugin.charts.wizard;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

import org.jfree.chart.JFreeChart;

public class FontHelper {
  public FontHelper(){
  }

  public String getFontStyleAsString(Font font){
      int style = font.getStyle();
      if(style == Font.BOLD) return "bold";
      if(style == Font.ITALIC) return "italic";
      if(style == Font.BOLD + Font.ITALIC) return "bold+italic";
      return "plain";
  }

  public String[] getAvailableFonts(){
      return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
  }
}
