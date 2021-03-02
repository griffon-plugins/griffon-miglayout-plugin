/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014-2021 The author and/or original authors.
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
package org.codehaus.griffon.runtime.javafx.miglayout;

import griffon.annotations.inject.DependsOn;
import griffon.builder.javafx.miglayout.MiglayoutJavaFXBuilderCustomizer;
import griffon.core.injection.Module;
import griffon.util.groovy.BuilderCustomizer;
import org.codehaus.griffon.runtime.core.injection.AbstractModule;
import org.kordamp.jipsy.annotations.ServiceProviderFor;

import javax.inject.Named;

/**
 * @author Andres Almiray
 */
@DependsOn("javafx-groovy")
@Named("miglayout-javafx-groovy")
@ServiceProviderFor(Module.class)
public class MiglayoutJavaFXGroovyModule extends AbstractModule {
    @Override
    protected void doConfigure() {
        // tag::bindings[]
        bind(BuilderCustomizer.class)
            .to(MiglayoutJavaFXBuilderCustomizer.class)
            .asSingleton();
        // end::bindings[]
    }
}