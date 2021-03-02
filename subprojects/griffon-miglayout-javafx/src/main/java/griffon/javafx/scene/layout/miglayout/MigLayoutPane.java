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
package griffon.javafx.scene.layout.miglayout;

import javafx.beans.DefaultProperty;
import javafx.scene.Node;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.ConstraintParser;
import net.miginfocom.layout.LC;
import org.tbee.javafx.scene.layout.MigPane;

import static net.miginfocom.layout.ConstraintParser.parseColumnConstraints;
import static net.miginfocom.layout.ConstraintParser.parseLayoutConstraint;
import static net.miginfocom.layout.ConstraintParser.parseRowConstraints;

/**
 * MigLayout container for JavaFX.<br/>
 * Based on {@code org.tbee.javafx.scene.layout.MigPane} originally from Tom Eugelink.
 *
 * @author Andres Almiray
 */
@DefaultProperty(value = "children") // for FXML integration
public class MigLayoutPane extends MigPane {
    public MigLayoutPane() {
    }

    public MigLayoutPane(LC layoutConstraints) {
        super(layoutConstraints);
    }

    public MigLayoutPane(LC layoutConstraints, AC colConstraints) {
        super(layoutConstraints, colConstraints);
    }

    public MigLayoutPane(LC layoutConstraints, AC colConstraints, AC rowConstraints) {
        super(layoutConstraints, colConstraints, rowConstraints);
    }

    public MigLayoutPane(String layoutConstraints) {
        super(layoutConstraints);
    }

    public MigLayoutPane(String layoutConstraints, String colConstraints) {
        super(layoutConstraints, colConstraints);
    }

    public MigLayoutPane(String layoutConstraints, String colConstraints, String rowConstraints) {
        super(layoutConstraints, colConstraints, rowConstraints);
    }

    // ============================================================================================================

    public void setLayoutConstraints(String value) {
        setLayout(value);
    }

    public void setColumnConstraints(String value) {
        setCols(value);
    }

    public void setRowConstraints(String value) {
        setRows(value);
    }

    // ============================================================================================================
    // FXML Integration

    /**
     * layout called in FXML on MigPane itself
     */
    public void setLayout(String value) {
        this.fxmLayoutConstraints = value;
        setLayoutConstraints(parseLayoutConstraint(ConstraintParser.prepare(value)));
    }

    public String getLayout() {
        return fxmLayoutConstraints;
    }

    private String fxmLayoutConstraints;

    /**
     * cols called in FXML on MigPane itself
     */
    public void setCols(String value) {
        this.fxmlColumConstraints = value;
        setColumnConstraints(parseColumnConstraints(ConstraintParser.prepare(value)));
    }

    public String getCols() {
        return fxmlColumConstraints;
    }

    private String fxmlColumConstraints;

    /**
     * rows called in FXML on MigPane itself
     */
    public void setRows(String value) {
        this.fxmlRowConstraints = value;
        setRowConstraints(parseRowConstraints(ConstraintParser.prepare(value)));
    }

    public String getRows() {
        return fxmlRowConstraints;
    }

    private String fxmlRowConstraints;

    /**
     * called from the subnodes in FXML via MigPane.cc="..."
     */
    public static void setCc(Node node, CC cc) {
        node.getProperties().put(FXML_CC_KEY, cc);
    }

    public static void setCc(Node node, String cc) {
        CC lCC = ConstraintParser.parseComponentConstraint(ConstraintParser.prepare(cc));
        setCc(node, lCC);
    }
}