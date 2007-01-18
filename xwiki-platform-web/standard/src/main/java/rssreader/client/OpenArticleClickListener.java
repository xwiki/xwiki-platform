package rssreader.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Window;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 14 janv. 2007
 * Time: 18:41:32
 * To change this template use File | Settings | File Templates.
 */
public class OpenArticleClickListener implements ClickListener {
    String url;

    public OpenArticleClickListener(String url) {
       this.url = url;
    }

    public void onClick(Widget sender) {
        Window.open(url, "_blank", "");
    }

}
