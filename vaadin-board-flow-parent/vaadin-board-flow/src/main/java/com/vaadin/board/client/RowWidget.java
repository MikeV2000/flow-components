package com.vaadin.board.client;

/*
 * #%L
 * Vaadin Board
 * %%
 * Copyright (C) 2017 Vaadin Ltd
 * %%
 * This program is available under Commercial Vaadin Add-On License 3.0
 * (CVALv3).
 * 
 * See the file license.html distributed with this software for more
 * information about licensing.
 * 
 * You should have received a copy of the CVALv3 along with this program.
 * If not, see <http://vaadin.com/license/cval-3>.
 * #L%
 */

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.FlowPanel;

public class RowWidget extends FlowPanel {

    public RowWidget() {
        super("vaadin-board-row");
    }

    public native void redraw(Element elem)/*-{
        elem.redraw();
    }-*/;

}
