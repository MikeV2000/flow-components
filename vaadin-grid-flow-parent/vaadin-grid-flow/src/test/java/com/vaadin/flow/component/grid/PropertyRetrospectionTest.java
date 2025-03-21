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
package com.vaadin.flow.component.grid;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.Binder;

public class PropertyRetrospectionTest {

    @SuppressWarnings("unused")
    public static class InnerBean {
        private String innerString;

        public String getInnerString() {
            return innerString;
        }

        public void setInnerString(String innerString) {
            this.innerString = innerString;
        }
    }

    @SuppressWarnings("unused")
    public static class BeanOne {
        private String someString;
        private InnerBean innerBean;

        public String getSomeString() {
            return someString;
        }

        public void setSomeString(String someString) {
            this.someString = someString;
        }

        public InnerBean getInnerBean() {
            return innerBean;
        }

        public void setInnerBean(InnerBean innerBean) {
            this.innerBean = innerBean;
        }
    }

    @SuppressWarnings("unused")
    public static class BeanTwo {
        private String someString;
        private InnerBean innerBean;

        public String getSomeString() {
            return someString;
        }

        public void setSomeString(String someString) {
            this.someString = someString;
        }

        public InnerBean getInnerBean() {
            return innerBean;
        }

        public void setInnerBean(InnerBean innerBean) {
            this.innerBean = innerBean;
        }
    }

    @Test
    public void testGridBeanProperties() {
        Grid<BeanOne> grid1 = new Grid<>(BeanOne.class);
        assertEquals(2,
                BeanPropertySet.get(BeanOne.class).getProperties().count());
        assertEquals(2, grid1.getColumns().size());
        grid1.addColumn("innerBean.innerString");
        assertEquals(3, grid1.getColumns().size());
        assertEquals(2,
                BeanPropertySet.get(BeanOne.class).getProperties().count());

        Grid<BeanOne> grid2 = new Grid<>(BeanOne.class);
        assertEquals(2,
                BeanPropertySet.get(BeanOne.class).getProperties().count());
        assertEquals(2, grid2.getColumns().size());
        grid2.addColumn("innerBean.innerString");
        assertEquals(3, grid2.getColumns().size());
        assertEquals(2,
                BeanPropertySet.get(BeanOne.class).getProperties().count());
    }

    @Test
    public void testBinder() {
        Binder<BeanTwo> binder1 = new Binder<>(BeanTwo.class);
        assertEquals(2,
                BeanPropertySet.get(BeanTwo.class).getProperties().count());
        binder1.forField(new TextField()).bind("innerBean.innerString");
        assertEquals(2,
                BeanPropertySet.get(BeanTwo.class).getProperties().count());

        Binder<BeanTwo> binder2 = new Binder<>(BeanTwo.class);
        assertEquals(2,
                BeanPropertySet.get(BeanTwo.class).getProperties().count());
        binder2.forField(new TextField()).bind("innerBean.innerString");
        assertEquals(2,
                BeanPropertySet.get(BeanTwo.class).getProperties().count());
    }
}
