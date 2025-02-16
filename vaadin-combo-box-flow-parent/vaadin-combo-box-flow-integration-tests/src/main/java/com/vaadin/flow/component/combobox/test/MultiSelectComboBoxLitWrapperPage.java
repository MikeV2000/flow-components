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
package com.vaadin.flow.component.combobox.test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;

@Route("vaadin-multi-select-combo-box/lit-wrapper")
public class MultiSelectComboBoxLitWrapperPage extends Div {
    public MultiSelectComboBoxLitWrapperPage() {
        add(new MultiSelectComboBoxLitWrapper());
    }

    @JsModule("./src/multi-select-combo-box-lit-wrapper.ts")
    @Tag("multi-select-combo-box-lit-wrapper")
    public static class MultiSelectComboBoxLitWrapper extends LitTemplate {
        @Id("combo-box")
        private MultiSelectComboBox<String> comboBox;

        public MultiSelectComboBoxLitWrapper() {
            List<String> items = IntStream.range(0, 100)
                    .mapToObj(i -> "Item " + (i + 1))
                    .collect(Collectors.toList());
            comboBox.setItems(items);
        }
    }
}
