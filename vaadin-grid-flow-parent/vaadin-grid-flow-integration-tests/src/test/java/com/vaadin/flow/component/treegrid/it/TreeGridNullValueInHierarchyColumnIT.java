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
package com.vaadin.flow.component.treegrid.it;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.TestPath;

@TestPath("vaadin-grid/" + TreeGridBasicFeaturesPage.VIEW)
public class TreeGridNullValueInHierarchyColumnIT extends AbstractTreeGridIT {

    @Test
    public void dataProviderWithNullValues_nullValueShouldBeDisplayedAsEmptyString() {
        open();
        setupTreeGrid();
        findElement(By.id("DataProviderWithNullValues")).click();

        Assert.assertEquals(3, getTreeGrid().getRowCount());
        assertCellTexts(0, 0, new String[] { "", "0 | 0", "0 | 1" });
    }
}
