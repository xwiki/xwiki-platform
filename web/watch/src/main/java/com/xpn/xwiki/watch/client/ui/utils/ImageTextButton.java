package com.xpn.xwiki.watch.client.ui.utils;

import com.google.gwt.user.client.ui.*;
import com.xpn.xwiki.watch.client.Watch;

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

public class ImageTextButton extends FocusPanel {
        private FlowPanel panel = new FlowPanel();

    public ImageTextButton(String text, String url) {
        super();
        panel.setStyleName("watch-imagetext");
        Image image = new Image(url);
        image.setTitle(text);
        image.setStyleName("watch-imagetext-image");
        panel.add(image);
        HTML html = new HTML(text);
        html.setStyleName("watch-imagetext-text");
        panel.add(html);
        setWidget(panel);
    }
}
