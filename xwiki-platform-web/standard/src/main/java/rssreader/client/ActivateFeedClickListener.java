package rssreader.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Hyperlink;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 2 déc. 2006
 * Time: 16:24:44
 * To change this template use File | Settings | File Templates.
 */
public class ActivateFeedClickListener implements ClickListener {
    private String name;
    private String url;
    private RSSReader reader;
    private Hyperlink link;

    public ActivateFeedClickListener(RSSReader reader, String name, String url, Hyperlink link) {
        this.name = name;
        this.url = url;
        this.reader = reader;
        this.link = link;
    }

    public void onClick(Widget sender) {
        reader.clearTreeStyles();
        link.setStyleName("treeelementtextactive");
        reader.onActivateFeed(name, url);
    }
}
