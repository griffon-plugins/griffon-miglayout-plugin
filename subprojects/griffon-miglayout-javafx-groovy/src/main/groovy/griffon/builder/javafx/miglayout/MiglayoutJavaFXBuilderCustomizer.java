/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014-2020 The author and/or original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package griffon.builder.javafx.miglayout;

import griffon.annotations.inject.DependsOn;
import griffon.builder.javafx.miglayout.factory.MigLayoutPaneFactory;
import groovy.util.Factory;
import org.codehaus.griffon.runtime.groovy.view.AbstractBuilderCustomizer;

import javax.inject.Named;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static griffon.builder.javafx.miglayout.factory.MigLayoutPaneFactory.constraintsAttributeDelegate;

/**
 * @author Andres Almiray
 */
@DependsOn({"javafx"})
@Named("miglayout-javafx")
public class MiglayoutJavaFXBuilderCustomizer extends AbstractBuilderCustomizer {
    public MiglayoutJavaFXBuilderCustomizer() {
        Map<String, Factory> factories = new LinkedHashMap<>();
        factories.put("migLayoutPane", new MigLayoutPaneFactory());
        setFactories(factories);
        setAttributeDelegates(Arrays.asList(constraintsAttributeDelegate()));
    }
}