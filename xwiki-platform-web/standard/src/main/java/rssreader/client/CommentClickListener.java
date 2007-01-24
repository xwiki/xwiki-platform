package rssreader.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 20 janv. 2007
 * Time: 15:52:40
 * To change this template use File | Settings | File Templates.
 */
public class CommentClickListener implements ClickListener {
    RSSReader reader;
    String page;

    public CommentClickListener(RSSReader reader, String page) {
        this.reader = reader;
        this.page = page;
    }


    public void onClick(Widget widget) {
        reader.showCommentForm(page, widget.getAbsoluteLeft(), widget.getAbsoluteTop());
    }
}
