/*
 * Copyright 2014 the original author or authors.
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
package griffon.javafx.scene.layout;

import javafx.beans.DefaultProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import net.miginfocom.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * MigLayout container for JavaFX.<br/>
 * Based on {@code MigLayoutPane} originally from Tom Eugelink.
 *
 * @author Andres Almiray
 */
@DefaultProperty(value = "children") // for FXML integration
public class MigLayoutPane extends Pane {
    // ============================================================================================================
    // CONSTRUCTOR

    /**
     *
     */
    public MigLayoutPane() {
        this("", "", "");
    }

    /**
     * use the class layout constraints
     */
    public MigLayoutPane(LC layoutConstraints) {
        this(layoutConstraints, null, null);
    }

    /**
     * use the class layout constraints
     */
    public MigLayoutPane(LC layoutConstraints, AC colConstraints) {
        this(layoutConstraints, colConstraints, null);
    }

    /**
     * use the class layout constraints
     */
    public MigLayoutPane(LC layoutConstraints, AC colConstraints, AC rowConstraints) {
        setLayoutConstraints(layoutConstraints);
        setColumnConstraints(colConstraints);
        setRowConstraints(rowConstraints);
        construct();
    }

    /**
     * use the string layout constraints
     */
    public MigLayoutPane(String layoutConstraints) {
        this(layoutConstraints, "", "");
    }

    /**
     * use the string layout constraints
     */
    public MigLayoutPane(String layoutConstraints, String colConstraints) {
        this(layoutConstraints, colConstraints, "");
    }

    /**
     * use the string layout constraints
     */
    public MigLayoutPane(String layoutConstraints, String colConstraints, String rowConstraints) {
        setLayoutConstraints(layoutConstraints);
        setColumnConstraints(colConstraints);
        setRowConstraints(rowConstraints);
        construct();
    }

    /**
     *
     */
    private void construct() {
        // defaults
        if (getLayoutConstraints() == null) setLayoutConstraints(new LC());
        if (getRowConstraints() == null) setRowConstraints(new AC());
        if (getColumnConstraints() == null) setColumnConstraints(new AC());

        // the container wrapper
        this.fx2ContainerWrapper = new FX2ContainerWrapper(this);

        // just in case when someone sneekly removes a child the JavaFX's way; prevent memory leaking
        getChildren().addListener(new ListChangeListener<Node>() {
            // as of JDK 1.6: @Override
            public void onChanged(Change<? extends Node> c) {
                while (c.next()) {
                    for (Node node : c.getRemoved()) {
                        // debug rectangles are not handled by miglayout
                        if (node instanceof DebugRectangle) continue;

                        // clean up
                        FX2ComponentWrapper lFX2ComponentWrapper = MigLayoutPane.this.nodeToComponentWrapperMap.remove(node);
                        componentWrapperList.remove(lFX2ComponentWrapper);
                        fx2ComponentWrapperToCCMap.remove(lFX2ComponentWrapper);

                        // grid is invalid
                        invalidateMigLayoutGrid();
                    }

                    for (Node node : c.getAddedSubList()) {
                        // debug rectangles are not handled by miglayout
                        if (node instanceof DebugRectangle) continue;

                        // get cc or use default
                        CC cc = cNodeToCC.remove(node);
                        if (cc == null) cc = new CC();

                        // create wrapper information
                        FX2ComponentWrapper lFX2ComponentWrapper = new FX2ComponentWrapper(node);
                        MigLayoutPane.this.componentWrapperList.add(lFX2ComponentWrapper);
                        MigLayoutPane.this.nodeToComponentWrapperMap.put(node, lFX2ComponentWrapper);
                        MigLayoutPane.this.fx2ComponentWrapperToCCMap.put(lFX2ComponentWrapper, cc);

                        // grid is invalid
                        invalidateMigLayoutGrid();
                    }
                }
                ;
            }
        });

        // create the initial grid so it won't be null
        createMigLayoutGrid();
    }

    private FX2ContainerWrapper fx2ContainerWrapper;
    final static protected Map<Node, CC> cNodeToCC = new WeakHashMap<Node, CC>();


    /**
     * Hold the serializable text representation of the constraints.
     */
    private Object layoutConstraints = "", colConstraints = "", rowConstraints = "";    // Should never be null!
    private transient LC lc = null;
    private transient AC colSpecs = null, rowSpecs = null;

    // ============================================================================================================
    // SCENE

    /**
     * Returns layout constraints either as a <code>String</code> or {@link net.miginfocom.layout.LC} depending what was sent in
     * to the constructor or set with {@link #setLayoutConstraints(Object)}.
     *
     * @return The layout constraints either as a <code>String</code> or {@link net.miginfocom.layout.LC} depending what was sent in
     *         to the constructor or set with {@link #setLayoutConstraints(Object)}. Never <code>null</code>.
     */
    public Object getLayoutConstraints() {
        return layoutConstraints;
    }

    /**
     * Sets the layout constraints for the layout manager instance as a String.
     * <p/>
     * See the class JavaDocs for information on how this string is formatted.
     *
     * @param constr The layout constraints as a String pr {@link net.miginfocom.layout.LC} representation. <code>null</code> is converted to <code>""</code> for storage.
     * @throws RuntimeException if the constraint was not valid.
     */
    public void setLayoutConstraints(Object constr) {
        if (constr == null || constr instanceof String) {
            constr = ConstraintParser.prepare((String) constr);
            lc = ConstraintParser.parseLayoutConstraint((String) constr);
        } else if (constr instanceof LC) {
            lc = (LC) constr;
        } else {
            throw new IllegalArgumentException("Illegal constraint type: " + constr.getClass().toString());
        }
        layoutConstraints = constr;
        // if debug is set, do it
        if (lc != null) iDebug = lc.getDebugMillis() > 0;
    }

    /**
     * Returns the column layout constraints either as a <code>String</code> or {@link net.miginfocom.layout.AC}.
     *
     * @return The column constraints either as a <code>String</code> or {@link net.miginfocom.layout.AC} depending what was sent in
     *         to the constructor or set with {@link #setColumnConstraints(Object)}. Never <code>null</code>.
     */
    public Object getColumnConstraints() {
        return colConstraints;
    }

    /**
     * Sets the column layout constraints for the layout manager instance as a String.
     * <p/>
     * See the class JavaDocs for information on how this string is formatted.
     *
     * @param constr The column layout constraints as a String or {@link net.miginfocom.layout.AC} representation. <code>null</code> is converted to <code>""</code> for storage.
     * @throws RuntimeException if the constraint was not valid.
     */
    public void setColumnConstraints(Object constr) {
        if (constr == null || constr instanceof String) {
            constr = ConstraintParser.prepare((String) constr);
            colSpecs = ConstraintParser.parseColumnConstraints((String) constr);
        } else if (constr instanceof AC) {
            colSpecs = (AC) constr;
        } else {
            throw new IllegalArgumentException("Illegal constraint type: " + constr.getClass().toString());
        }
        colConstraints = constr;
    }

    /**
     * Returns the row layout constraints either as a <code>String</code> or {@link net.miginfocom.layout.AC}.
     *
     * @return The row constraints either as a <code>String</code> or {@link net.miginfocom.layout.AC} depending what was sent in
     *         to the constructor or set with {@link #setRowConstraints(Object)}. Never <code>null</code>.
     */
    public Object getRowConstraints() {
        return rowConstraints;
    }

    /**
     * Sets the row layout constraints for the layout manager instance as a String.
     * <p/>
     * See the class JavaDocs for information on how this string is formatted.
     *
     * @param constr The row layout constraints as a String or {@link net.miginfocom.layout.AC} representation. <code>null</code> is converted to <code>""</code> for storage.
     * @throws RuntimeException if the constraint was not valid.
     */
    public void setRowConstraints(Object constr) {
        if (constr == null || constr instanceof String) {
            constr = ConstraintParser.prepare((String) constr);
            rowSpecs = ConstraintParser.parseRowConstraints((String) constr);
        } else if (constr instanceof AC) {
            rowSpecs = (AC) constr;
        } else {
            throw new IllegalArgumentException("Illegal constraint type: " + constr.getClass().toString());
        }
        rowConstraints = constr;
    }

    // ============================================================================================================
    // SCENE

    /**
     * @param node
     * @param cc
     */
    public void add(Node node, CC cc) {
        cNodeToCC.put(node, cc);
        getChildren().add(node);
    }

    /**
     * @param node
     */
    public void add(Node node) {
        add(node, new CC());
    }

    /**
     * @param node
     * @param cc
     */
    public void add(Node node, String cc) {
        // parse as CC
        CC lCC = ConstraintParser.parseComponentConstraint(ConstraintParser.prepare(cc));

        // do regular add
        add(node, lCC);
    }


    // ============================================================================================================
    // LAYOUT

    // store of constraints
    final private List<FX2ComponentWrapper> componentWrapperList = new ArrayList<FX2ComponentWrapper>();
    final private Map<Node, FX2ComponentWrapper> nodeToComponentWrapperMap = new WeakHashMap<Node, FX2ComponentWrapper>();
    final private Map<ComponentWrapper, CC> fx2ComponentWrapperToCCMap = new WeakHashMap<ComponentWrapper, CC>();
    final private Map<Node, Integer> nodeToHashcodeMap = new WeakHashMap<Node, Integer>();
    private volatile boolean iDebug = false;

    /**
     * This is where the actual layout happens
     */
    protected void layoutChildren() {
        //System.out.println("layoutChildren");
        super.layoutChildren();

        // validate if the grid should be recreated
        validateMigLayoutGrid();

        // here the actual layout happens
        // this will use FX2ComponentWrapper.setBounds to actually place the components
        int[] lBounds = new int[]{0, 0, (int) Math.ceil(getWidth()), (int) Math.ceil(getHeight())};
        this.grid.layout(lBounds, lc.getAlignX(), lc.getAlignY(), iDebug, true);

        // paint debug
        if (iDebug) {
            clearDebug();
            this.grid.paintDebug();
        }
    }

    /*
      * 
      */
    private void createMigLayoutGrid() {
        //System.out.println("createMigLayoutGrid");
        this.grid = new Grid(fx2ContainerWrapper, lc, rowSpecs, colSpecs, fx2ComponentWrapperToCCMap, null);
        this.valid = true;

        // -----------------------------------------
        // set MigLayout's own size
        setMinWidth(LayoutUtil.getSizeSafe(grid.getWidth(), LayoutUtil.MIN));
        setPrefWidth(LayoutUtil.getSizeSafe(grid.getWidth(), LayoutUtil.PREF));
        setMaxWidth(LayoutUtil.getSizeSafe(grid.getWidth(), LayoutUtil.MAX));

        setMinHeight(LayoutUtil.getSizeSafe(grid.getHeight(), LayoutUtil.MIN));
        setPrefHeight(LayoutUtil.getSizeSafe(grid.getHeight(), LayoutUtil.PREF));
        setMaxHeight(LayoutUtil.getSizeSafe(grid.getHeight(), LayoutUtil.MAX));
        // -----------------------------------------
    }

    volatile private Grid grid;

    /*
      * the grid is valid if all hashcodes are unchanged
      */
    private void validateMigLayoutGrid() {

        // only needed if the grid is valid
        if (isMiglayoutGridValid()) {

            // scan all childeren
            for (Node lChild : getChildren()) {

                // if this child is managed by MigLayout
                if (nodeToComponentWrapperMap.containsKey(lChild)) {

                    // get its previous hashcode
                    Integer lPreviousHashcode = nodeToHashcodeMap.get(lChild);

                    // calculate its current hashcode
                    Integer lCurrentHashcode = calculateHashcode(lChild);

                    // if it is not the same
                    if (lPreviousHashcode == null || !lPreviousHashcode.equals(lCurrentHashcode)) {

                        // invalidate the grid
                        invalidateMigLayoutGrid();

                        // remember the new hashcode
                        nodeToHashcodeMap.put(lChild, Integer.valueOf(lCurrentHashcode));
                    }
                }
            }
        }

        // if invalid, create
        if (!isMiglayoutGridValid()) {
            createMigLayoutGrid();
        }
    }

    /*
      * mark the grid as invalid
      */
    private void invalidateMigLayoutGrid() {
        this.valid = false;
    }

    /*
    * @returns true if the grid is valid.
    */
    private boolean isMiglayoutGridValid() {
        return this.valid;
    }

    volatile boolean valid = false;

    /**
     * use all kinds of properties to calculate a hash for the layout
     *
     * @param node
     * @return the calculated hashcode for the given Node.
     */
    private Integer calculateHashcode(Node node) {
        StringBuffer lStringBuffer = new StringBuffer();
        lStringBuffer.append(node.minWidth(-1));
        lStringBuffer.append("x");
        lStringBuffer.append(node.minHeight(-1));
        lStringBuffer.append("/");
        lStringBuffer.append(node.prefWidth(-1));
        lStringBuffer.append("x");
        lStringBuffer.append(node.prefHeight(-1));
        lStringBuffer.append("/");
        lStringBuffer.append(node.maxWidth(-1));
        lStringBuffer.append("x");
        lStringBuffer.append(node.maxHeight(-1));
        lStringBuffer.append("/");
        lStringBuffer.append(node.getLayoutBounds().getWidth());
        lStringBuffer.append("x");
        lStringBuffer.append(node.getLayoutBounds().getHeight());
        lStringBuffer.append("/");
        lStringBuffer.append(node.isVisible());
        return lStringBuffer.toString().hashCode();
    }

    // ============================================================================================================
    // DEBUG

    /**
     *
     */
    public void clearDebug() {
        //System.out.println("clearDebug");
        MigLayoutPane.this.getChildren().removeAll(this.debugRectangles);
        this.debugRectangles.clear();
    }

    final private List<Node> debugRectangles = new ArrayList<Node>();

    /**
     *
     */
    private void addDebugRectangle(double x, double y, double w, double h, DebugRectangleType type) {
        DebugRectangle lRectangle = new DebugRectangle(x, y, w, h);
        if (type == DebugRectangleType.CELL) {
            //System.out.print(getId() + ": " + "paintDebugCell ");
            lRectangle.setStroke(getDebugCellColor());
            lRectangle.getStrokeDashArray().addAll(3d, 3d);
        } else if (type == DebugRectangleType.EXTERNAL) {
            //System.out.print(getId() + ": " + "paintDebugExternal ");
            lRectangle.setStroke(getDebugExternalColor());
            lRectangle.getStrokeDashArray().addAll(5d, 5d);
        } else if (type == DebugRectangleType.OUTLINE) {
            //System.out.print(getId() + ": " + "paintDebugOutline ");
            lRectangle.setStroke(getDebugOutlineColor());
            lRectangle.getStrokeDashArray().addAll(4d, 4d);
        } else if (type == DebugRectangleType.CONTAINER_OUTLINE) {
            //System.out.print(getId() + ": " + "paintDebugContainerOutline ");
            lRectangle.setStroke(getDebugContainerOutlineColor());
            lRectangle.getStrokeDashArray().addAll(7d, 7d);
        } else {
            throw new IllegalStateException("Unknown debug rectangle type");
        }
        //System.out.println(lRectangle.getX() + "," + lRectangle.getY() + "/" + lRectangle.getWidth() + "x" + lRectangle.getHeight());
        //lRectangle.setStrokeWidth(0.5f);
        lRectangle.setFill(null);
        lRectangle.mouseTransparentProperty().set(true); // just to be sure

        // add as child
        MigLayoutPane.this.getChildren().add(lRectangle);
        this.debugRectangles.add(lRectangle);
    }

    enum DebugRectangleType {CELL, OUTLINE, CONTAINER_OUTLINE, EXTERNAL}

    class DebugRectangle extends Rectangle {
        public DebugRectangle(double x, double y, double w, double h) {
            super(x, y, w, h);
        }
    }

    /**
     * debugCellColor
     */
    public Color getDebugCellColor() {
        return this.debugCellColor;
    }

    public void setDebugCellColor(Color value) {
        this.debugCellColor = value;
    }

    private Color debugCellColor = Color.RED;

    /**
     * debugExternalColor
     */
    public Color getDebugExternalColor() {
        return this.debugExternalColor;
    }

    public void setDebugExternalColor(Color value) {
        this.debugExternalColor = value;
    }

    private Color debugExternalColor = Color.BLUE;

    /**
     * debugOutlineColor
     */
    public Color getDebugOutlineColor() {
        return this.debugOutlineColor;
    }

    public void setDebugOutlineColor(Color value) {
        this.debugOutlineColor = value;
    }

    private Color debugOutlineColor = Color.GREEN;

    /**
     * debugContainerOutlineColor
     */
    public Color getDebugContainerOutlineColor() {
        return this.debugContainerOutlineColor;
    }

    public void setDebugContainerOutlineColor(Color value) {
        this.debugContainerOutlineColor = value;
    }

    private Color debugContainerOutlineColor = Color.PURPLE;

    // ============================================================================================================
    // ContainerWrapper

    /*
      * This class provides the data for MigLayout for the container
      */
    class FX2ContainerWrapper extends FX2ComponentWrapper
        implements net.miginfocom.layout.ContainerWrapper {

        public FX2ContainerWrapper(Node node) {
            super(node);
        }

        // as of JDK 1.6: @Override
        public net.miginfocom.layout.ComponentWrapper[] getComponents() {
            return componentWrapperList.toArray(new FX2ComponentWrapper[]{}); // must be in the order of adding!			  
        }

        // as of JDK 1.6: @Override
        public int getComponentCount() {
            return MigLayoutPane.this.fx2ComponentWrapperToCCMap.size();
        }

        // as of JDK 1.6: @Override
        public java.lang.Object getLayout() {
            return MigLayoutPane.this;
        }

        // as of JDK 1.6: @Override
        public boolean isLeftToRight() {
            return true;
        }

        // as of JDK 1.6: @Override
        public void paintDebugCell(int x, int y, int w, int h) {
            addDebugRectangle((double) x, (double) y, (double) w, (double) h, DebugRectangleType.CELL);
        }

        // as of JDK 1.6: @Override
        public void paintDebugOutline() {
            // to be frank: this is done via trail and error 
            addDebugRectangle(0
                , 0
                , getWidth()
                , getHeight()
                , DebugRectangleType.CONTAINER_OUTLINE
            );
        }
    }

    // ============================================================================================================
    // ComponentWrapper

    /*
      * This class provides the data for MigLayout for a single component
      */
    class FX2ComponentWrapper implements net.miginfocom.layout.ComponentWrapper {

        // wrap this node
        public FX2ComponentWrapper(Node node) {
            this.node = node;
        }

        final protected Node node;

        // get the wrapped node
        // as of JDK 1.6: @Override
        public Object getComponent() {
            return this.node;
        }

        // get the parent
        // as of JDK 1.6: @Override
        public ContainerWrapper getParent() {
            return fx2ContainerWrapper;
        }

        // what type are we wrapping
        // as of JDK 1.6: @Override
        public int getComponetType(boolean arg0) {
            if (node instanceof TextField || node instanceof TextArea) {
                return TYPE_TEXT_FIELD;
            } else if (node instanceof Group) {
                return TYPE_CONTAINER;
            } else {
                return TYPE_UNKNOWN;
            }
        }

        // as of JDK 1.6: @Override
        public void setBounds(int x, int y, int width, int height) {
            // for debugging System.out.println(getComponent() + " setBound x="  + x + ",y=" + y + " / w=" + width + ",h=" + height + " / resizable=" + this.node.isResizable());
            this.node.resizeRelocate((double) x, (double) y, (double) width, (double) height);
        }

        // as of JDK 1.6: @Override
        public int getX() {
            int v = (int) Math.ceil(node.getLayoutBounds().getMinX());
            return v;
        }

        // as of JDK 1.6: @Override
        public int getY() {
            int v = (int) Math.ceil(node.getLayoutBounds().getMinY());
            return v;
        }

        // as of JDK 1.6: @Override
        public int getWidth() {
            // for debugging if (getComponent() instanceof MigLayoutFX2 == false) System.out.println(getComponent() + " getWidth " + node.getLayoutBounds().getWidth());
            int v = (int) Math.ceil(node.getLayoutBounds().getWidth());
            return v;
        }

        // as of JDK 1.6: @Override
        public int getMinimumWidth(int height) {
            int v = (int) Math.ceil(this.node.minWidth(height));
            // for debugging System.out.println(getComponent() + " getMinimumWidth " + v);
            return v;
        }

        // as of JDK 1.6: @Override
        public int getPreferredWidth(int height) {
            int v = (int) Math.ceil(this.node.prefWidth(height));
            // for debugging System.out.println(getComponent() + " getPreferredWidth " + v);			
            return v;
        }

        // as of JDK 1.6: @Override
        public int getMaximumWidth(int height) {
            int v = (int) Math.ceil(this.node.maxWidth(height));
            if (this.node instanceof Button) {
                v = Integer.MAX_VALUE;
            } // for debugging System.out.println(getComponent() + " forced getMaximumWidth " + v); }
            if (this.node instanceof ToggleButton) {
                v = Integer.MAX_VALUE;
            } // for debugging System.out.println(getComponent() + " forced getMaximumWidth " + v); }
            if (this.node instanceof CheckBox) {
                v = Integer.MAX_VALUE;
            } // for debugging System.out.println(getComponent() + " forced getMaximumWidth " + v); } // is this needed?
            if (this.node instanceof ChoiceBox) {
                v = Integer.MAX_VALUE;
            } // for debugging System.out.println(getComponent() + " forced getMaximumWidth " + v); }
            return v;
        }

        // as of JDK 1.6: @Override
        public int getHeight() {
            int v = (int) Math.ceil(node.getLayoutBounds().getHeight());
            return v;
        }

        // as of JDK 1.6: @Override
        public int getMinimumHeight(int width) {
            int v = (int) Math.ceil(this.node.minHeight(width));
            return v;
        }

        // as of JDK 1.6: @Override
        public int getPreferredHeight(int width) {
            int v = (int) Math.ceil(this.node.prefHeight(width));
            return v;
        }

        // as of JDK 1.6: @Override
        public int getMaximumHeight(int width) {
            int v = (int) Math.ceil(this.node.maxHeight(width));
            return v;
        }

        // as of JDK 1.6: @Override
        public int getBaseline(int width, int height) {
            return -1; // TODO
        }

        // as of JDK 1.6: @Override
        public int getScreenLocationX() {
            // this code is never called?			
            Bounds lBoundsInScenenode = node.localToScene(node.getBoundsInLocal());
            int v = (int) Math.ceil(node.getScene().getX() + node.getScene().getX() + lBoundsInScenenode.getMinX());
            // for debugging System.out.println(getComponent() + " getScreenLocationX =" + v);
            return v;
        }

        // as of JDK 1.6: @Override
        public int getScreenLocationY() {
            // this code is never called?			
            Bounds lBoundsInScenenode = node.localToScene(node.getBoundsInLocal());
            int v = (int) Math.ceil(node.getScene().getY() + node.getScene().getY() + lBoundsInScenenode.getMinY());
            // for debugging System.out.println(getComponent() + " getScreenLocationX =" + v);
            return v;
        }

        // as of JDK 1.6: @Override
        public int getScreenHeight() {
            // this code is never called?			
            int v = (int) Math.ceil(Screen.getPrimary().getBounds().getHeight());
            // for debugging System.out.println(getComponent() + " getScreenHeight=" + v);
            return v;
        }

        // as of JDK 1.6: @Override
        public int getScreenWidth() {
            // this code is never called?			
            int v = (int) Math.ceil(Screen.getPrimary().getBounds().getWidth());
            // for debugging System.out.println(getComponent() + " getScreenWidth=" + v);
            return v;
        }

        // as of JDK 1.6: @Override
        public int[] getVisualPadding() {
            return null;
        }

        // as of JDK 1.6: @Override
        public int getHorizontalScreenDPI() {
            return (int) Math.ceil(Screen.getPrimary().getDpi());
        }

        // as of JDK 1.6: @Override
        public int getVerticalScreenDPI() {
            return (int) Math.ceil(Screen.getPrimary().getDpi());
        }

        // as of JDK 1.6: @Override
        public float getPixelUnitFactor(boolean arg0) {
            return 1.0f; // TODO
        }

        // as of JDK 1.6: @Override
        public int getLayoutHashCode() {
            int lHashCode = 0;
            lHashCode += ((int) this.node.getLayoutBounds().getWidth()) + (((int) this.node.getLayoutBounds().getHeight()) * 32); // << 0, << 5
            if (this.node.isVisible()) {
                lHashCode += 1324511;
            }
            if (this.node.isManaged()) {
                lHashCode += 1324513;
            }
            if (this.node.getId().length() > 0) {
                lHashCode += this.node.getId().hashCode();
            }
            return lHashCode; // 0;
        }

        // as of JDK 1.6: @Override
        public String getLinkId() {
            return node.getId();
        }

        // as of JDK 1.6: @Override
        public boolean hasBaseline() {
            return false;
        }

        // as of JDK 1.6: @Override
        public boolean isVisible() {
            return node.isVisible();
        }

        // as of JDK 1.6: @Override
        public void paintDebugOutline() {
            ComponentWrapper lComponentWrapper = nodeToComponentWrapperMap.get(node);
            CC lCC = fx2ComponentWrapperToCCMap.get(lComponentWrapper);
            if (lCC != null && lCC.isExternal()) {
                addDebugRectangle(this.node.getLayoutX() + this.node.getLayoutBounds().getMinX(), (double) this.node.getLayoutY() + this.node.getLayoutBounds().getMinY(), getWidth(), getHeight(), DebugRectangleType.EXTERNAL); // always draws node size, even if less is used
            } else {
                addDebugRectangle(this.node.getLayoutX() + this.node.getLayoutBounds().getMinX(), (double) this.node.getLayoutY() + this.node.getLayoutBounds().getMinY(), getWidth(), getHeight(), DebugRectangleType.OUTLINE); // always draws node size, even if less is used
            }
        }

        public int hashCode() {
            return node.hashCode();
        }

        /**
         * This needs to be overridden so that different wrappers that hold the same component compare
         * as equal.  Otherwise, Grid won't be able to layout the components correctly.
         */
        public boolean equals(Object o) {
            if (!(o instanceof FX2ComponentWrapper)) {
                return false;
            }
            return getComponent().equals(((FX2ComponentWrapper) o).getComponent());
        }

    }

    // -----------------------------------------
    // No need to recalculate MigLayout's size when resizing. 
    // This is copied from MigLayout swing's code:
    //      maximumLayoutSize(); minimumLayoutSize(); preferredLayoutSize();

    @Override
    protected double computeMaxHeight(double width) {
        int h = LayoutUtil.getSizeSafe(grid != null ? grid.getHeight() : null, LayoutUtil.MAX);
        return h;
    }

    @Override
    protected double computeMaxWidth(double height) {
        int w = LayoutUtil.getSizeSafe(grid != null ? grid.getWidth() : null, LayoutUtil.MAX);
        return w;
    }

    @Override
    protected double computeMinHeight(double width) {
        int h = LayoutUtil.getSizeSafe(grid != null ? grid.getHeight() : null, LayoutUtil.MIN);
        return h;
    }

    @Override
    protected double computeMinWidth(double height) {
        int w = LayoutUtil.getSizeSafe(grid != null ? grid.getWidth() : null, LayoutUtil.MIN);
        return w;
    }

    @Override
    protected double computePrefHeight(double width) {
        int h = LayoutUtil.getSizeSafe(grid != null ? grid.getHeight() : null, LayoutUtil.PREF);
        return h;
    }

    @Override
    protected double computePrefWidth(double height) {
        int w = LayoutUtil.getSizeSafe(grid != null ? grid.getWidth() : null, LayoutUtil.PREF);
        return w;
    }

    // ============================================================================================================
    // FXML Integration

    /**
     * layout called in FXML on MigLayoutPane itself
     */
    public void setLayout(String value) {
        this.fxmLayoutConstraints = value;
        setLayoutConstraints(ConstraintParser.parseLayoutConstraint(ConstraintParser.prepare(value)));
    }

    public String getLayout() {
        return fxmLayoutConstraints;
    }

    private String fxmLayoutConstraints;

    /**
     * cols called in FXML on MigLayoutPane itself
     */
    public void setCols(String value) {
        this.fxmlColumConstraints = value;
        setColumnConstraints(ConstraintParser.parseColumnConstraints(ConstraintParser.prepare(value)));
    }

    public String getCols() {
        return fxmlColumConstraints;
    }

    private String fxmlColumConstraints;

    /**
     * rows called in FXML on MigLayoutPane itself
     */
    public void setRows(String value) {
        this.fxmlRowConstraints = value;
        setRowConstraints(ConstraintParser.parseRowConstraints(ConstraintParser.prepare(value)));
    }

    public String getRows() {
        return fxmlRowConstraints;
    }

    private String fxmlRowConstraints;

    /**
     * called from the subnodes in FXML via MigLayoutPane.cc="..."
     */
    public static void setCc(Node node, CC cc) {
        // temporarily put it in a map
        cNodeToCC.put(node, cc);
    }

    public static void setCc(Node node, String cc) {
        CC lCC = ConstraintParser.parseComponentConstraint(ConstraintParser.prepare(cc));
        setCc(node, lCC);
    }
}