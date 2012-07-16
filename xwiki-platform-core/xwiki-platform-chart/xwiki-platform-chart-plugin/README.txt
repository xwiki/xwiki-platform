To use this plugin:
* Drop it in WEB-INF/lib
* Edit xwiki.cfg to add the plugin
* Edit struts-config.xml and add:

        <action path="/charting/"
                type="com.xpn.xwiki.plugin.charts.actions.ChartingAction"
                name="charting"
                scope="request">
        </action>
        <action path="/charting/"
                type="com.xpn.xwiki.plugin.charts.actions.ChartingAction"
                name="charting"
                scope="request">
        </action>
