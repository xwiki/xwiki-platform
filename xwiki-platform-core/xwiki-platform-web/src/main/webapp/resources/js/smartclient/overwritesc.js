/*
 * Overrides the isc.Element.getOffsetLeft() function set in ISC_Core with this version, fixed to work around 
 * the bug described by http://forums.smartclient.com/showthread.php?t=3097 . 
 * TODO: remove when we'll upgrade SmartClient to the 7.0 version, which is mentioned to fix the bug.
 */

isc.Element.addClassMethods({
// Element.getOffsetLeft()
//  Takes 'element'
//  element should be a pointer to a DOM element or the ID for a DOM element
//  (To get the offsetLeft for a widget use use widget.getOffsetLeft() instead)
//  Returns the true offsetLeft - the absolute left coordinate with respect to whatever is
//  reported by the DOM as the offsetParent of the element.
getOffsetLeft : function (element) {
 
    // Note: This method is used by the canvas instance 'getOffsetLeft()' method to calculate
    // the offset position.
    // We work with coordinates / sizes relative to the outside of any margins around our 
    // widgets - do the same with this method.
    
    if (element == null) {
        this.logWarn("getOffsetLeft: passed null element");
        return 0;
    }
    
    // IE and Moz both return somewhat unreliable values for element.offsetLeft by default.
    // Paper over these bugs and differences.
    var left = element.offsetLeft;
    // --- caching code:
    // If we've already calculated a value (based on a reported offsetLeft value), and
    // the reported value has not changed, return the previously calculated value.
    // This caching is safe except for cases where an indirect parent's styling changes in a
    // way that would affect this element's true offsetLeft.
    if (element._cachedReportedOffsetLeft == left) {
        return element._cachedCalculatedOffsetLeft;
    } else {
        // debug message for sanity checking coordinate caching
        //this.logWarn(element.getAttribute("eventProxy") + ": new DOM value for offsetLeft");
    }

    // always subtract the left margin (if there is one) to get the position OUTSIDE the
    // margins.
    // Note: for a negative margin, the reported offsetLeft does not need to be adjusted by the
    // specified margin size - it represents the position of the element - and in this case there
    // is no margin outside the element (rather the specified margin shifts the element to the 
    // left / up)
    var leftMargin = parseInt(isc.Element.getComputedStyleAttribute(element, "marginLeft"));
    if (isc.isA.Number(leftMargin) && leftMargin > 0) {
        left -= leftMargin;
    }
    
    
    var documentBody = this.getDocumentBody(),
        parentStyle,
        px = "px",
        // determine whether the element is absolutely / relatively / etc. positioned    
        elementPosition = element.style.position;

    // Workarounds for Moz        
    if (isc.Browser.isMoz) {
        // In moz we get some unexpected results
        
        if (element.offsetParent == null) return left;
        
        if (element.offsetParent != documentBody) {

            parentStyle = 
                this.ns.Element.getComputedStyle(element.offsetParent, ["borderLeftWidth", "overflow"]);       

            // The behavior changes with different releses of Moz / Firefox
            var geckoVersion = isc.Browser.geckoVersion,
            
                
                scrollingAdjustment = (parentStyle.overflow != "visible") &&
                                      (geckoVersion >= 20051111 || 
                                      (elementPosition == isc.Canvas.ABSOLUTE && parentStyle.overflow != "hidden")),
                
                accountForBorderBox = (geckoVersion > 20020826 &&
                                        (element.offsetParent.style.MozBoxSizing == "border-box"));

            
            if (accountForBorderBox != scrollingAdjustment) {

                
                if (accountForBorderBox) {
                    left -= (isc.isA.Number(parseInt(parentStyle.borderLeftWidth)) ?
                                            parseInt(parentStyle.borderLeftWidth) : 0);
                                            
                } 
                
                if (scrollingAdjustment) {
                    left += (isc.isA.Number(parseInt(parentStyle.borderLeftWidth)) ?
                                            parseInt(parentStyle.borderLeftWidth) : 0);
                                            
                }
            }
                  
        }
    }
    
    // Workarounds for IE
     
    if (isc.Browser.isIE && !isc.Browser.isIE8Strict) {
    
        

        var currentParent = element.offsetParent,
            parentStyle;
        // I bet parentStyle!=documentBody should not have been like this here, but I'll avoid changing it since I haven't got a better idea
        // and make sure currentParent is not null at this point otherwise the next access will fail
        if (currentParent && parentStyle != documentBody) parentStyle = currentParent.currentStyle; 
        
        
        var hasSpecifiedSize = (element.currentStyle.height != isc.Canvas.AUTO ||
                                element.currentStyle.width != isc.Canvas.AUTO);

        
        var continueDeductingBorders = true;                                
        
        // iterate up the offsetParents till we reach the doc. body
        // Better comparison with the document.body than with documentBody which differs from strict to quirks mode
        // and make sure currentParent is not null
        // This whole loops seems very unsafe wrt currentParent.offsetParent which can be null
        while (currentParent && currentParent != document.body) {

            
            
            
            if (parentStyle.position == isc.Canvas.ABSOLUTE) continueDeductingBorders = false;
            
            
            if (parentStyle.width == isc.Canvas.AUTO && 
                parentStyle.height == isc.Canvas.AUTO &&
                parentStyle.position == isc.Canvas.RELATIVE) {
                
                
                if (continueDeductingBorders &&
                    isc.isA.String(parentStyle.borderLeftWidth) && 
                    parentStyle.borderLeftWidth.contains(px)        ) {
                        left -= parseInt(parentStyle.borderLeftWidth);
                }    
                
                
                if (hasSpecifiedSize) {
                
                    if (isc.isA.String(parentStyle.marginLeft) && 
                        parentStyle.marginLeft.contains(px)) 
                    {
                        var parentMarginLeft = parseInt(parentStyle.marginLeft);
                        if (parentMarginLeft > 0) left -= parentMarginLeft;
                    }                           
                
                    
                    if (currentParent.offsetParent != documentBody) {
                        
                        var superPadding = currentParent.offsetParent.currentStyle.padding;
                        if (isc.isA.String(superPadding) && superPadding.contains(px)) {
                            left -= parseInt(superPadding);
                        }
                    } else {    
                        
                        left -= (documentBody.leftMargin ? parseInt(documentBody.leftMargin) : 0);                
                    }
                } 
                
            } // end of if

            
            elementPosition = currentParent.style.position;
            currentParent = currentParent.offsetParent;
            // make sure the currentParent is not null again at this point
            if (currentParent && currentParent != document.body) {
                parentStyle = currentParent.currentStyle;
            }
            
        }   // End of while loop
        
    }        
    
    // Workarounds for Safari
    if (isc.Browser.isSafari && isc.Browser.safariVersion < 525.271) {
        // In some versions of Safari, if the offsetParent has a border, the offsetLeft / top
        // reported is relative to the outside of that border, rather than the inside, so deduct
        // that value
        // No longer the case in Safari 3.2.1 (525.27.1)
        if (element.offsetParent != null && element.offsetParent != documentBody) {
            var parentBorder = 
                this.ns.Element.getComputedStyle(element.offsetParent, ["borderLeftWidth"]).borderLeftWidth;
            if (parentBorder != null) parentBorder = parseInt(parentBorder);
            if (isc.isA.Number(parentBorder)) left -= parentBorder;
        }
    }
    // --- cacheing code:
    // Cache the calculated and reported value, by saving it as attributes on the DOM element
    element._cachedReportedOffsetLeft = element.offsetLeft;
    element._cachedCalculatedOffsetLeft = left;
    
    return left;
}
});