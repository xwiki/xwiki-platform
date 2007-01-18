package rssreader.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import api.client.XObject;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 9 janv. 2007
 * Time: 22:41:46
 * To change this template use File | Settings | File Templates.
 */
public class FlagClickListener implements ClickListener {
    private RSSReader reader;
    private String feedname;
    private XObject feedentry;
    private Image link;

    public FlagClickListener(RSSReader reader, XObject feedentry, String feedname, Image link) {
        this.reader = reader;
        this.feedentry = feedentry;
        this.feedname = feedname;
        this.link = link;
    }

    public void onClick(Widget sender) {
        reader.flagFeed(feedname, feedentry, link);
    }
}
