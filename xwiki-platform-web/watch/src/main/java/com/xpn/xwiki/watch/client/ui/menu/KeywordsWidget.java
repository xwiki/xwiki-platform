package com.xpn.xwiki.watch.client.ui.menu;

import com.xpn.xwiki.watch.client.ui.WatchWidget;
import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.data.Keyword;
import com.google.gwt.user.client.ui.*;

import java.util.*;

/**
 * Copyright 2006,XpertNet SARL,and individual contributors as indicated
 * by the contributors.txt.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 *
 * @author ldubost
 */

public class KeywordsWidget extends WatchWidget {
    private Map keywordsLink = new HashMap();
    private FlowPanel keywordsPanel = new FlowPanel();

    public KeywordsWidget() {
        super();
    }

    public String getName() {
        return "keywords";
    }

    public KeywordsWidget(Watch watch) {
        super(watch);
        panel = new FlowPanel();
        panel.add(getTitlePanel());
        panel.add(keywordsPanel);
        initWidget(panel);
        init();
    }

    private Widget getTitlePanel() {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("filter", "keywords-title"));
        HTML titleHTML = new HTML(watch.getTranslation("filter.keywords.title"));
        titleHTML.setStyleName(watch.getStyleName("filter", "title-keywords-text"));
        p.add(titleHTML);
        return p;
    }

    public void resetSelections() {
        Iterator it = keywordsLink.keySet().iterator();
        String keywordactive = watch.getFilterStatus().getKeyword();
        while (it.hasNext()) {
            Keyword keyword  = (Keyword) it.next();
            Hyperlink link = (Hyperlink) keywordsLink.get(keyword);
            if (link!=null) {
                if (keyword.getName().equals(keywordactive)) {
                    link.addStyleName(watch.getStyleName("keyword", "link-active"));
                } else {
                    link.removeStyleName(watch.getStyleName("keyword", "link-active"));
                }
            }
        }
    }

    public void refreshData() {
        keywordsPanel.clear();
        keywordsLink.clear();
        List keywords = watch.getConfig().getKeywords();
        if (keywords!=null) {
            Iterator it = keywords.iterator();
            while (it.hasNext()) {
                final Keyword keyword = (Keyword) it.next();
                if ((keyword.getName()!=null)&&(!keyword.equals(""))) {
                    Hyperlink link = new Hyperlink(keyword.getDisplayName(), "");
                    link.setStyleName(watch.getStyleName("keyword", "link"));
                    keywordsLink.put(keyword, link);
                    link.addClickListener(new ClickListener() {
                        public void onClick(Widget widget) {
                            watch.refreshOnActivateKeyword(keyword);
                        }
                    });
                    keywordsPanel.add(link);
                }
            }
        }
        resetSelections();
    }

    /*
    public void setActiveTags(String[] tags) {
        Iterator it = tagsLink.values().iterator();
        while (it.hasNext()) {
            ((Hyperlink)it.next()).removeStyleName(watch.getStyleName("tagscloud", "active"));
        }
        for (int i=0;i<tags.length;i++) {
            Hyperlink link = (Hyperlink) tagsLink.get(tags[i]);
            if (link!=null)
             link.setStyleName(watch.getStyleName("tagscloud", "active"));
        }
    }
    */

}
