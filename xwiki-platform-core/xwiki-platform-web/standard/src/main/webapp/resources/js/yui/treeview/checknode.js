if (typeof ychecknode=="undefined") {
    ychecknode = 1;
/**
 * The check box marks an item selected.  It is a simulated form field
 * with three states ...
 * 0=unchecked, 1=checked with some children, 2=checked without children checked
 * When a task is clicked, the state of the nodes and parent and children
 * are updated, and this behavior cascades.
 *
 * @extends YAHOO.widget.TextNode
 * @constructor
 * @param oData    {object}  A string or object containing the data that will
 *                           be used to render this node.
 * @param oParent  {Node}    This node's parent node
 * @param expanded {boolean} The initial expanded/collapsed state
 * @param checked  {boolean} The initial checked/unchecked state
 */
YAHOO.widget.CheckNode = function(oId, oData, oParent, expanded, checked) {

    if (oData) {
        this.init(oId, oParent, expanded);
        this.setUpLabel(oData);
        if (checked && checked === true) {
            this.check();
        }

        /*
        if (!this.tree.checkClickEvent) {
            this.tree.checkClickEvent =
                    new YAHOO.util.CustomEvent("checkclick", this.tree);
        }
        */
    }

    // this.logger = new YAHOO.widget.LogWriter(this.toString());
};

YAHOO.widget.CheckNode.prototype = new YAHOO.widget.TextNode();

/**
 * True if checkstate is 1 (some children checked) or 2 (all children checked),
 * false if 0.
 * @type boolean
 */
YAHOO.widget.CheckNode.prototype.checked = false;

/**
 * checkState
 * 0=unchecked, 1=some children checked, 2=all children checked
 * @type int
 */
YAHOO.widget.CheckNode.prototype.checkState = 0;

/**
 * The id of the check element
 * @type string
 */
YAHOO.widget.CheckNode.prototype.getCheckElId = function() {
    return "ygtvcheck" + this.index;
};

/**
 * Returns the check box element
 * @return the check html element (img)
 */
YAHOO.widget.CheckNode.prototype.getCheckEl = function() {
    return document.getElementById(this.getCheckElId());
};

/**
 * The style of the check element, derived from its current state
 * @return {string} the css style for the current check state
 */
YAHOO.widget.CheckNode.prototype.getCheckStyle = function() {
    return "ygtvcheck" + this.checkState;
};

/**
 * Returns the link that will invoke this node's check toggle
 * @return {string} returns the link required to adjust the checkbox state
 */
YAHOO.widget.CheckNode.prototype.getCheckLink = function() {
    return "YAHOO.widget.TreeView.getNode(\'" + this.tree.id + "\'," +
        this.index + ").checkClick()";
};

/**
 * Invoked when the user clicks the check box
 */
YAHOO.widget.CheckNode.prototype.checkClick = function() {
    // this.logger.log("previous checkstate: " + this.checkState);
    if (this.checkState === 0) {
        this.check();
    } else {
        this.uncheck();
    }

    // this.tree.checkClickEvent.fire(this);

    this.onCheckClick();
};

/**
 * Override to get the check click event
 */
YAHOO.widget.CheckNode.prototype.onCheckClick = function() {
    // this.logger.log("check was clicked");
}

/**
 * Refresh the state of this node's parent, and cascade up.
 */
YAHOO.widget.CheckNode.prototype.updateParent = function() {
    var p = this.parent;

    if (!p || !p.updateParent) {
        // this.logger.log("Abort udpate parent: " + this.index);
        return;
    }

    var somethingChecked = false;
    var somethingNotChecked = false;

    for (var i=0;i< p.children.length;++i) {
        if (p.children[i].checked) {
            somethingChecked = true;
            // checkState will be 1 if the child node has unchecked children
            if (p.children[i].checkState == 1) {
                somethingNotChecked = true;
            }
        } else {
            somethingNotChecked = true;
        }
    }

    if (somethingChecked) {
        p.setCheckState(2);
    } else {
        // p.setCheckState(0);
    }

    p.updateCheckHtml();
    p.updateParent();
};

/**
 * If the node has been rendered, update the html to reflect the current
 * state of the node.
 */
YAHOO.widget.CheckNode.prototype.updateCheckHtml = function() {
    if (this.parent && this.parent.childrenRendered) {
        this.getCheckEl().className = this.getCheckStyle();
    }
};

/**
 * Updates the state.  The checked property is true if the state is 1 or 2
 *
 * @param the new check state
 */
YAHOO.widget.CheckNode.prototype.setCheckState = function(state) {
    this.checkState = state;
    this.checked = (state > 0);
};

/**
 * Check this node
 */
YAHOO.widget.CheckNode.prototype.check = function() {
    // this.logger.log("check");
    this.setCheckState(2);
    for (var i=0; i<this.children.length; ++i) {
        // this.children[i].check();
    }
    this.updateCheckHtml();
    this.updateParent();
};

/**
 * Uncheck this node
 */
YAHOO.widget.CheckNode.prototype.uncheck = function() {
    this.setCheckState(0);
    for (var i=0; i<this.children.length; ++i) {
        this.children[i].uncheck();
    }
    this.updateCheckHtml();
    this.updateParent();
};

// Overrides YAHOO.widget.TextNode
YAHOO.widget.CheckNode.prototype.getNodeHtml = function() {
    // this.logger.log("Generating html");
    var sb = new Array();

    sb[sb.length] = '<table border="0" cellpadding="0" cellspacing="0">';
    sb[sb.length] = '<tr>';

    for (i=0;i<this.depth;++i) {
        sb[sb.length] = '<td class="' + this.getDepthStyle(i) + '">&#160;</td>';
    }

    sb[sb.length] = '<td';
    sb[sb.length] = ' id="' + this.getToggleElId() + '"';
    sb[sb.length] = ' class="' + this.getStyle() + '"';
    if (this.hasChildren(true)) {
        sb[sb.length] = ' onmouseover="this.className=';
        sb[sb.length] = 'YAHOO.widget.TreeView.getNode(\'';
        sb[sb.length] = this.tree.id + '\',' + this.index +  ').getHoverStyle()"';
        sb[sb.length] = ' onmouseout="this.className=';
        sb[sb.length] = 'YAHOO.widget.TreeView.getNode(\'';
        sb[sb.length] = this.tree.id + '\',' + this.index +  ').getStyle()"';
    }
    sb[sb.length] = ' onclick="javascript:' + this.getToggleLink() + '">&#160;';
    sb[sb.length] = '</td>';

    // check box
    sb[sb.length] = '<td';
    sb[sb.length] = ' id="' + this.getCheckElId() + '"';
    sb[sb.length] = ' class="' + this.getCheckStyle() + '"';
    sb[sb.length] = ' onclick="javascript:' + this.getCheckLink() + '">';
    sb[sb.length] = '&#160;</td>';


    sb[sb.length] = '<td>';
    sb[sb.length] = '<a';
    sb[sb.length] = ' id="' + this.labelElId + '"';
    sb[sb.length] = ' class="' + this.labelStyle + '"';
    sb[sb.length] = ' href="' + this.href + '"';
    sb[sb.length] = ' target="' + this.target + '"';
    if (this.hasChildren(true)) {
        sb[sb.length] = ' onmouseover="document.getElementById(\'';
        sb[sb.length] = this.getToggleElId() + '\').className=';
        sb[sb.length] = 'YAHOO.widget.TreeView.getNode(\'';
        sb[sb.length] = this.tree.id + '\',' + this.index +  ').getHoverStyle()"';
        sb[sb.length] = ' onmouseout="document.getElementById(\'';
        sb[sb.length] = this.getToggleElId() + '\').className=';
        sb[sb.length] = 'YAHOO.widget.TreeView.getNode(\'';
        sb[sb.length] = this.tree.id + '\',' + this.index +  ').getStyle()"';
    }
    sb[sb.length] = ' >';
    sb[sb.length] = this.label;
    sb[sb.length] = '</a>';
    sb[sb.length] = '</td>';
    sb[sb.length] = '</tr>';
    sb[sb.length] = '</table>';

    return sb.join("");

};

YAHOO.widget.CheckNode.prototype.toString = function() {
    return "CheckNode (" + this.index + ") " + this.label;
};

}