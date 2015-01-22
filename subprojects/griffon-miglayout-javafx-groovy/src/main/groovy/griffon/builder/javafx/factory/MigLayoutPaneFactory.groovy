/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package griffon.builder.javafx.factory

import griffon.javafx.scene.layout.MigLayoutPane
import groovyx.javafx.factory.AbstractNodeFactory

/**
 * @author Andres Almiray
 */
class MigLayoutPaneFactory extends AbstractNodeFactory {
    static final String DELEGATE_PROPERTY_CONSTRAINT = "_delegateProperty:Constraint"
    static final String DEFAULT_DELEGATE_PROPERTY_CONSTRAINT = "constraints"

    MigLayoutPaneFactory() {
        super(MigLayoutPane, false)
    }

    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        builder.context[DELEGATE_PROPERTY_CONSTRAINT] = attributes.remove("constraintsProperty") ?: DEFAULT_DELEGATE_PROPERTY_CONSTRAINT
        super.newInstance(builder, name, value, attributes)
    }

    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (!(child instanceof javafx.scene.Node)) {
            return
        }
        try {
            def constraints = builder.context.constraints
            if (constraints != null) {
                parent.add(child, constraints)
                builder.context.remove('constraints')
            } else {
                parent.add(child, '')
            }
        } catch (MissingPropertyException mpe) {
            parent.add(child, '')
        }
    }

    static void constraintsAttributeDelegate(FactoryBuilderSupport builder, node, Map attributes) {
        String constraintsAttr = builder?.context?.getAt(DELEGATE_PROPERTY_CONSTRAINT) ?: DEFAULT_DELEGATE_PROPERTY_CONSTRAINT
        if (attributes.containsKey(constraintsAttr)) {
            builder.context.constraints = attributes.remove(constraintsAttr)
        }
    }
}