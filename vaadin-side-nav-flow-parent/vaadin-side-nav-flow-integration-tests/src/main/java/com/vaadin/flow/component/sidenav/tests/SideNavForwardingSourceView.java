/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.component.sidenav.tests;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/**
 * View used as a forwarding source when testing navigation in
 * {@link SideNavPage}.
 */
@Route("vaadin-side-nav/side-nav-forwarding-source-view")
public class SideNavForwardingSourceView extends Div
        implements BeforeEnterObserver {

    public SideNavForwardingSourceView() {
        add(new Span(
                "View to test forwarding to a page that contains vaadin-side-nav component"));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo(SideNavPage.class);
    }
}
