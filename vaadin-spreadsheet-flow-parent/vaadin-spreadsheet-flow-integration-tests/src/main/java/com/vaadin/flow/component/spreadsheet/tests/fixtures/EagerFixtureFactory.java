/**
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>} for the full
 * license.
 */
package com.vaadin.flow.component.spreadsheet.tests.fixtures;

public class EagerFixtureFactory implements SpreadsheetFixtureFactory {

    private SpreadsheetFixture fixture;

    public EagerFixtureFactory(SpreadsheetFixture fixture) {
        this.fixture = fixture;
    }

    @Override
    public SpreadsheetFixture create() {
        return fixture;
    }
}
