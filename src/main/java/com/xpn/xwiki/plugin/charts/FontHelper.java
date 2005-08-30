package com.xpn.xwiki.plugin.charts;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

import org.jfree.chart.JFreeChart;

public class FontHelper {
  public FontHelper(){
  }

  public String getDefaultTitleFont(){
      return JFreeChart.DEFAULT_TITLE_FONT.getFamily();
  }

  public String getDefaultTitleSize(){
      return JFreeChart.DEFAULT_TITLE_FONT.getSize() + "";
  }

  public String getDefaultTitleStyle(){
      int style = JFreeChart.DEFAULT_TITLE_FONT.getStyle();
      if(style == Font.BOLD) return "bold";
      if(style == Font.ITALIC) return "italic";
      if(style == Font.BOLD + Font.ITALIC) return "bold+italic";
      return "plain";
  }

  public String[] getAvailableFonts(){
      return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
  }
}
