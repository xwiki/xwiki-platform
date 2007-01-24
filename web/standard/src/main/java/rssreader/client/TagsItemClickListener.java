package rssreader.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 22 janv. 2007
 * Time: 00:42:28
 * To change this template use File | Settings | File Templates.
 */
public class TagsItemClickListener implements ClickListener {
    RSSReader reader;
    String tag;

    public TagsItemClickListener(RSSReader rssReader, String tag) {
        this.reader = rssReader;
        this.tag = tag;
    }

    public void onClick(Widget widget) {
        reader.onActivateTag(tag);
    }
}
