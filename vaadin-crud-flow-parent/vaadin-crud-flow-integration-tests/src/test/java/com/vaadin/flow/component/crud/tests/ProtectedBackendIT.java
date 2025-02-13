/**
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>} for the full
 * license.
 */
package com.vaadin.flow.component.crud.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.component.confirmdialog.testbench.ConfirmDialogElement;
import com.vaadin.flow.component.crud.testbench.CrudElement;
import com.vaadin.flow.component.grid.testbench.GridElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.flow.testutil.TestPath;
import com.vaadin.tests.AbstractComponentIT;

@TestPath("vaadin-crud/protectedbackend")
public class ProtectedBackendIT extends AbstractComponentIT {

    @Before
    public void init() {
        open();
    }

    @Test
    public void tryDelete() {
        CrudElement crud = $(CrudElement.class).waitForFirst();

        GridElement grid = crud.getGrid();
        Assert.assertEquals(2, grid.getRowCount());

        crud.openRowForEditing(0);

        Assert.assertTrue(crud.isEditorOpen());

        crud.getEditorDeleteButton().click();
        crud.getConfirmDeleteDialog().getConfirmButton().click();

        Assert.assertTrue("Editor should stay opened if exception happened",
                crud.isEditorOpen());

        Assert.assertEquals(2, grid.getRowCount());
    }

    @Test
    public void tryCancel() {
        CrudElement crud = $(CrudElement.class).waitForFirst();
        crud.openRowForEditing(0);
        Assert.assertTrue(crud.isEditorOpen());
        crud.getEditorCancelButton().click();
        Assert.assertTrue("Editor should stay opened if exception happened",
                crud.isEditorOpen());
    }

    @Test
    public void tryModify() {
        CrudElement crud = $(CrudElement.class).waitForFirst();

        crud.openRowForEditing(0);

        modify(crud, "Other", false);

        crud.openRowForEditing(1);
        // A click in another row when editor is dirty opens confirmCancel
        // dialog
        ConfirmDialogElement confirmCancel = crud.getConfirmCancelDialog();
        Assert.assertEquals("Discard changes", confirmCancel.getHeaderText());

        confirmCancel.getConfirmButton().click();
        modify(crud, "Other", true);

        crud.openRowForEditing(1);
        modify(crud, "Oth", false);
    }

    private void modify(CrudElement crud, String newValue,
            boolean isModifyAllowed) {
        Assert.assertTrue(crud.isEditorOpen());

        TextFieldElement lastNameField = crud.getEditor()
                .$(TextFieldElement.class).last();

        lastNameField.setValue(newValue);
        crud.getEditorSaveButton().click();

        if (!isModifyAllowed) {
            Assert.assertTrue("Editor should stay opened if exception happened",
                    crud.isEditorOpen());
        }

        GridElement grid = crud.getGrid();
        try {
            grid.getCell(newValue);
            Assert.assertTrue(
                    "Modify was not allowed, but the value in grid was changed",
                    isModifyAllowed);
        } catch (NoSuchElementException | TimeoutException e) {
            Assert.assertFalse(
                    "Modify was allowed, but the value in grid was not changed",
                    isModifyAllowed);
        }
    }
}
