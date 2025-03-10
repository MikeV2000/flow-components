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
package com.vaadin.flow.component.upload.tests;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.Route;

@Route("vaadin-upload/switch-receivers")
public class SwitchReceiversPage extends Div {

    public SwitchReceiversPage() {
        Upload upload = new Upload();

        NativeButton setSingleFileReceiver = new NativeButton(
                "Set single file receiver", event -> {
                    MemoryBuffer buffer = new MemoryBuffer();
                    upload.setReceiver(buffer);
                });
        setSingleFileReceiver.setId("set-single-file-receiver");

        NativeButton setMultiFileReceiver = new NativeButton(
                "Set multi file receiver", event -> {
                    MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
                    upload.setReceiver(buffer);
                });
        setMultiFileReceiver.setId("set-multi-file-receiver");

        NativeButton setMultiFileReceiverAndMaxFiles = new NativeButton(
                "Set multi file receiver and max files", event -> {
                    MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
                    upload.setReceiver(buffer);
                    upload.setMaxFiles(3);
                });
        setMultiFileReceiverAndMaxFiles
                .setId("set-multi-file-receiver-and-max-files");

        add(upload, setSingleFileReceiver, setMultiFileReceiver,
                setMultiFileReceiverAndMaxFiles);
    }

}
