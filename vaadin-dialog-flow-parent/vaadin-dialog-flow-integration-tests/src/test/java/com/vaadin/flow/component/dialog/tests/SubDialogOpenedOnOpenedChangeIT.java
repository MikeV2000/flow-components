/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.component.dialog.tests;

import com.vaadin.flow.testutil.TestPath;
import com.vaadin.tests.AbstractComponentIT;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@TestPath("vaadin-dialog/sub-dialog-opened-on-opened-change")
public class SubDialogOpenedOnOpenedChangeIT extends AbstractComponentIT {

    @Before
    public void init() {
        open();
    }

    @Test
    public void openMainDialog_openSubDialog_mainDialogGetsDetached() {
        waitForElementPresent(By.id("open-main-dialog"));
        findElement(By.id("open-main-dialog")).click();

        waitForElementPresent(By.id("close-main-dialog-and-open-sub-dialog"));
        findElement(By.id("close-main-dialog-and-open-sub-dialog")).click();

        WebElement output = findElement(By.id("output"));
        Assert.assertEquals("Detached", output.getText());
    }
}