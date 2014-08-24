/*
 * @(#)GridBagLayout.java       1.37 00/02/02
 *
 * Copyright 1995-2000 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

// This is a modified version of java.awt.GridBagLayout; the difference is the
// behavior when the available size is less than the preferred size.
// The original GridBagLayout will size components to their minimum size when
// there is not enough room to display them at their preferred size; in some
// cases, however, the minimum size is too small to be useful (e.g. JTextField
// has a minimum width of 0).
// This modified version will shrink components more gently, so layouts can be
// squeezed much more before becoming unusable.

//package java.awt;
package jdbcnav.util;
import java.awt.*;

import java.util.Hashtable;

class MyGridBagLayoutInfo implements java.io.Serializable {
  int width, height;            /* number of cells horizontally, vertically */
  int startx, starty;           /* starting point for layout */
  int minWidth[];               /* largest minWidth in each column */
  int minHeight[];              /* largest minHeight in each row */
  int prefWidth[];              /* largest prefWidth in each column */
  int prefHeight[];             /* largest prefHeight in each row */
  double weightX[];             /* largest weight in each column */
  double weightY[];             /* largest weight in each row */

  MyGridBagLayoutInfo () {
    minWidth = new int[MyGridBagLayout.MAXGRIDSIZE];
    minHeight = new int[MyGridBagLayout.MAXGRIDSIZE];
    prefWidth = new int[MyGridBagLayout.MAXGRIDSIZE];
    prefHeight = new int[MyGridBagLayout.MAXGRIDSIZE];
    weightX = new double[MyGridBagLayout.MAXGRIDSIZE];
    weightY = new double[MyGridBagLayout.MAXGRIDSIZE];
  }
}

/**
 * The <code>GridBagLayout</code> class is a flexible layout
 * manager that aligns components vertically and horizontally,
 * without requiring that the components be of the same size.
 * Each <code>GridBagLayout</code> object maintains a dynamic
 * rectangular grid of cells, with each component occupying
 * one or more cells, called its <em>display area</em>.
 * <p>
 * Each component managed by a grid bag layout is associated
 * with an instance of
 * {@link GridBagConstraints}
 * that specifies how the component is laid out within its display area.
 * <p>
 * How a <code>GridBagLayout</code> object places a set of components
 * depends on the <code>GridBagConstraints</code> object associated
 * with each component, and on the minimum size
 * and the preferred size of the components' containers.
 * <p>
 * To use a grid bag layout effectively, you must customize one or more
 * of the <code>GridBagConstraints</code> objects that are associated
 * with its components. You customize a <code>GridBagConstraints</code>
 * object by setting one or more of its instance variables:
 * <p>
 * <dl>
 * <dt>{@link GridBagConstraints#gridx},
 * {@link GridBagConstraints#gridy}
 * <dd>Specifies the cell at the upper left of the component's display area,
 * where the upper-left-most cell has address
 * <code>gridx&nbsp;=&nbsp;0</code>,
 * <code>gridy&nbsp;=&nbsp;0</code>.
 * Use <code>GridBagConstraints.RELATIVE</code> (the default value)
 * to specify that the component be just placed
 * just to the right of (for <code>gridx</code>)
 * or just below (for <code>gridy</code>)
 * the component that was added to the container
 * just before this component was added.
 * <dt>{@link GridBagConstraints#gridwidth},
 * {@link GridBagConstraints#gridheight}
 * <dd>Specifies the number of cells in a row (for <code>gridwidth</code>)
 * or column (for <code>gridheight</code>)
 * in the component's display area.
 * The default value is 1.
 * Use <code>GridBagConstraints.REMAINDER</code> to specify
 * that the component be the last one in its row (for <code>gridwidth</code>)
 * or column (for <code>gridheight</code>).
 * Use <code>GridBagConstraints.RELATIVE</code> to specify
 * that the component be the next to last one
 * in its row (for <code>gridwidth</code>)
 * or column (for <code>gridheight</code>).
 * <dt>{@link GridBagConstraints#fill}
 * <dd>Used when the component's display area
 * is larger than the component's requested size
 * to determine whether (and how) to resize the component.
 * Possible values are
 * <code>GridBagConstraints.NONE</code> (the default),
 * <code>GridBagConstraints.HORIZONTAL</code>
 * (make the component wide enough to fill its display area
 * horizontally, but don't change its height),
 * <code>GridBagConstraints.VERTICAL</code>
 * (make the component tall enough to fill its display area
 * vertically, but don't change its width), and
 * <code>GridBagConstraints.BOTH</code>
 * (make the component fill its display area entirely).
 * <dt>{@link GridBagConstraints#ipadx},
 * {@link GridBagConstraints#ipady}
 * <dd>Specifies the component's internal padding within the layout,
 * how much to add to the minimum size of the component.
 * The width of the component will be at least its minimum width
 * plus <code>(ipadx&nbsp;*&nbsp;2)</code> pixels (since the padding
 * applies to both sides of the component). Similarly, the height of
 * the component will be at least the minimum height plus
 * <code>(ipady&nbsp;*&nbsp;2)</code> pixels.
 * <dt>{@link GridBagConstraints#insets}
 * <dd>Specifies the component's external padding, the minimum
 * amount of space between the component and the edges of its display area.
 * <dt>{@link GridBagConstraints#anchor}
 * <dd>Used when the component is smaller than its display area
 * to determine where (within the display area) to place the component.
 * Valid values are
 * <code>GridBagConstraints.CENTER</code> (the default),
 * <code>GridBagConstraints.NORTH</code>,
 * <code>GridBagConstraints.NORTHEAST</code>,
 * <code>GridBagConstraints.EAST</code>,
 * <code>GridBagConstraints.SOUTHEAST</code>,
 * <code>GridBagConstraints.SOUTH</code>,
 * <code>GridBagConstraints.SOUTHWEST</code>,
 * <code>GridBagConstraints.WEST</code>, and
 * <code>GridBagConstraints.NORTHWEST</code>.
 * <dt>{@link GridBagConstraints#weightx},
 * {@link GridBagConstraints#weighty}
 * <dd>Used to determine how to distribute space, which is
 * important for specifying resizing behavior.
 * Unless you specify a weight for at least one component
 * in a row (<code>weightx</code>) and column (<code>weighty</code>),
 * all the components clump together in the center of their container.
 * This is because when the weight is zero (the default),
 * the <code>GridBagLayout</code> object puts any extra space
 * between its grid of cells and the edges of the container.
 * </dl>
 * <p>
 * The following figure shows ten components (all buttons)
 * managed by a grid bag layout:
 * <p>
 * <img src="doc-files/GridBagLayout-1.gif"
 * ALIGN=center HSPACE=10 VSPACE=7>
 * <p>
 * Each of the ten components has the <code>fill</code> field
 * of its associated <code>GridBagConstraints</code> object
 * set to <code>GridBagConstraints.BOTH</code>.
 * In addition, the components have the following non-default constraints:
 * <p>
 * <ul>
 * <li>Button1, Button2, Button3: <code>weightx&nbsp;=&nbsp;1.0</code>
 * <li>Button4: <code>weightx&nbsp;=&nbsp;1.0</code>,
 * <code>gridwidth&nbsp;=&nbsp;GridBagConstraints.REMAINDER</code>
 * <li>Button5: <code>gridwidth&nbsp;=&nbsp;GridBagConstraints.REMAINDER</code>
 * <li>Button6: <code>gridwidth&nbsp;=&nbsp;GridBagConstraints.RELATIVE</code>
 * <li>Button7: <code>gridwidth&nbsp;=&nbsp;GridBagConstraints.REMAINDER</code>
 * <li>Button8: <code>gridheight&nbsp;=&nbsp;2</code>,
 * <code>weighty&nbsp;=&nbsp;1.0</code>
 * <li>Button9, Button 10:
 * <code>gridwidth&nbsp;=&nbsp;GridBagConstraints.REMAINDER</code>
 * </ul>
 * <p>
 * Here is the code that implements the example shown above:
 * <p>
 * <hr><blockquote><pre>
 * import java.awt.*;
 * import java.util.*;
 * import java.applet.Applet;
 *
 * public class GridBagEx1 extends Applet {
 *
 *     protected void makebutton(String name,
 *                               GridBagLayout gridbag,
 *                               GridBagConstraints c) {
 *         Button button = new Button(name);
 *         gridbag.setConstraints(button, c);
 *         add(button);
 *     }
 *
 *     public void init() {
 *         GridBagLayout gridbag = new GridBagLayout();
 *         GridBagConstraints c = new GridBagConstraints();
 *
 *         setFont(new Font("Helvetica", Font.PLAIN, 14));
 *         setLayout(gridbag);
 *
 *         c.fill = GridBagConstraints.BOTH;
 *         c.weightx = 1.0;
 *         makebutton("Button1", gridbag, c);
 *         makebutton("Button2", gridbag, c);
 *         makebutton("Button3", gridbag, c);
 *
 *         c.gridwidth = GridBagConstraints.REMAINDER; //end row
 *         makebutton("Button4", gridbag, c);
 *
 *         c.weightx = 0.0;                //reset to the default
 *         makebutton("Button5", gridbag, c); //another row
 *
 *         c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last in row
 *         makebutton("Button6", gridbag, c);
 *
 *         c.gridwidth = GridBagConstraints.REMAINDER; //end row
 *         makebutton("Button7", gridbag, c);
 *
 *         c.gridwidth = 1;                //reset to the default
 *         c.gridheight = 2;
 *         c.weighty = 1.0;
 *         makebutton("Button8", gridbag, c);
 *
 *         c.weighty = 0.0;                //reset to the default
 *         c.gridwidth = GridBagConstraints.REMAINDER; //end row
 *         c.gridheight = 1;               //reset to the default
 *         makebutton("Button9", gridbag, c);
 *         makebutton("Button10", gridbag, c);
 *
 *         setSize(300, 100);
 *     }
 *
 *     public static void main(String args[]) {
 *         Frame f = new Frame("GridBag Layout Example");
 *         GridBagEx1 ex1 = new GridBagEx1();
 *
 *         ex1.init();
 *
 *         f.add("Center", ex1);
 *         f.pack();
 *         f.setSize(f.getPreferredSize());
 *         f.show();
 *     }
 * }
 * </pre></blockquote><hr>
 * <p>
 * @version 1.5, 16 Nov 1995
 * @author Doug Stein
 * @see       java.awt.GridBagConstraints
 * @since     JDK1.0
 */
public class MyGridBagLayout implements LayoutManager2,
                                      java.io.Serializable {

    /**
     * The maximum number of grid positions (both horizontally and
     * vertically) that can be laid out by the grid bag layout.
     */
  protected static final int MAXGRIDSIZE = 512;

    /**
     * The smallest grid that can be laid out by the grid bag layout.
     */
  protected static final int MINSIZE = 1;
  protected static final int PREFERREDSIZE = 2;

    /**
     * This hashtable maintains the association between
     * a component and its gridbag constraints.
     * The Keys in comptable are the components and the
     * values are the instances of GridBagConstraints.
     *
     * @serial
     * @see java.awt.GridBagConstraints
     */
  protected Hashtable<Component, MyGridBagConstraints> comptable;

    /**
     * This field holds a gridbag constraints instance
     * containing the default values, so if a component
     * does not have gridbag constraints associated with
     * it, then the component will be assigned a
     * copy of the <code>defaultConstraints</code>.
     *
     * @serial
     * @see #getConstraints(Component)
     * @see #setConstraints(Component, MyGridBagConstraints)
     * @see #lookupConstraints(Component)
     */
  protected MyGridBagConstraints defaultConstraints;

    /**
     * This field holds tha layout information
     * for the gridbag.  The information in this field
     * is based on the most recent validation of the
     * gridbag.
     * If <code>layoutInfo</code> is <code>null</code>
     * this indicates that there are no components in
     * the gridbag or if there are components, they have
     * not yet been validated.
     *
     * @serial
     * @see #GetLayoutInfo(Container)
     */
  protected MyGridBagLayoutInfo layoutInfo;

    /**
     * This field holds the overrides to the column minimum
     * width.  If this field is non-null the values are
     * applied to the gridbag after all of the minimum columns
     * widths have been calculated.
     * If columnWidths has more elements than the number of
     * columns, columns are added to the gridbag to match
     * the number of elements in columnWidth.
     *
     * @serial
     * @see #getLayoutDimensions()
     */
  public int columnWidths[];
  
   /**
     * This field holds the overrides to the row minimum
     * heights.  If this field is non-null the values are
     * applied to the gridbag after all of the minimum row
     * heights have been calculated.
     * If rowHeights has more elements than the number of
     * rows, rowa are added to the gridbag to match
     * the number of elements in rowHeights.
     *
     * @serial
     * @see #getLayoutDimensions()
     */
  public int rowHeights[];

    /**
     * This field holds the overrides to the column weights.
     * If this field is non-null the values are
     * applied to the gridbag after all of the columns
     * weights have been calculated.
     * If columnWeights[i] &gt; weight for column i, then
     * column i is assigned the weight in columnWeights[i].
     * If columnWeights has more elements than the number
     * of columns, the excess elements are ignored - they do
     * not cause more columns to be created.
     *
     * @serial
     */
  public double columnWeights[];

    /**
     * This field holds the overrides to the row weights.
     * If this field is non-null the values are
     * applied to the gridbag after all of the rows
     * weights have been calculated.
     * If rowWeights[i] &gt; weight for row i, then
     * row i is assigned the weight in rowWeights[i].
     * If rowWeights has more elements than the number
     * of rows, the excess elements are ignored - they do
     * not cause more rows to be created.
     *
     * @serial
     */
  public double rowWeights[];

  /**
   * Creates a grid bag layout manager.
   */
  public MyGridBagLayout () {
    comptable = new Hashtable<Component, MyGridBagConstraints>();
    defaultConstraints = new MyGridBagConstraints();
  }

  /**
   * Sets the constraints for the specified component in this layout.
   * @param       comp the component to be modified.
   * @param       constraints the constraints to be applied.
   */
  public void setConstraints(Component comp, MyGridBagConstraints constraints) {
    comptable.put(comp, (MyGridBagConstraints) constraints.clone());
  }

  /**
   * Gets the constraints for the specified component.  A copy of
   * the actual <code>GridBagConstraints</code> object is returned.
   * @param       comp the component to be queried.
   * @return      the constraint for the specified component in this
   *                  grid bag layout; a copy of the actual constraint
   *                  object is returned.
   */
  public MyGridBagConstraints getConstraints(Component comp) {
    MyGridBagConstraints constraints = comptable.get(comp);
    if (constraints == null) {
      setConstraints(comp, defaultConstraints);
      constraints = comptable.get(comp);
    }
    return (MyGridBagConstraints)constraints.clone();
  }

  /**
   * Retrieves the constraints for the specified component.
   * The return value is not a copy, but is the actual
   * <code>GridBagConstraints</code> object used by the layout mechanism.
   * @param       comp the component to be queried
   * @return      the contraints for the specified component.
   */
  protected MyGridBagConstraints lookupConstraints(Component comp) {
    MyGridBagConstraints constraints = comptable.get(comp);
    if (constraints == null) {
      setConstraints(comp, defaultConstraints);
      constraints = comptable.get(comp);
    }
    return constraints;
  }

  /**
   * Removes the constraints for the specified component in this layout
   * @param       comp the component to be modified.
   */
  private void removeConstraints(Component comp) {
    comptable.remove(comp);
  }

    /**
     * Determines the origin of the layout grid.
     * Most applications do not call this method directly.
     * @return     the origin of the cell in the top-left
     *                    corner of the layout grid.
     * @since      JDK1.1
     */
  public Point getLayoutOrigin () {
    Point origin = new Point(0,0);
    if (layoutInfo != null) {
      origin.x = layoutInfo.startx;
      origin.y = layoutInfo.starty;
    }
    return origin;
  }

    /**
     * Determines column widths and row heights for the layout grid.
     * <p>
     * Most applications do not call this method directly.
     * @return     an array of two arrays, containing the widths
     *                       of the layout columns and
     *                       the heights of the layout rows.
     * @since      JDK1.1
     */
  public int [][] getLayoutDimensions () {
    if (layoutInfo == null)
      return new int[2][0];

    int dim[][] = new int [2][];
    dim[0] = new int[layoutInfo.width];
    dim[1] = new int[layoutInfo.height];

    System.arraycopy(layoutInfo.prefWidth, 0, dim[0], 0, layoutInfo.width);
    System.arraycopy(layoutInfo.prefHeight, 0, dim[1], 0, layoutInfo.height);

    return dim;
  }

    /**
     * Determines the weights of the layout grid's columns and rows.
     * Weights are used to calculate how much a given column or row
     * stretches beyond its preferred size, if the layout has extra
     * room to fill.
     * <p>
     * Most applications do not call this method directly.
     * @return      an array of two arrays, representing the
     *                    horizontal weights of the layout columns
     *                    and the vertical weights of the layout rows.
     * @since       JDK1.1
     */
  public double [][] getLayoutWeights () {
    if (layoutInfo == null)
      return new double[2][0];

    double weights[][] = new double [2][];
    weights[0] = new double[layoutInfo.width];
    weights[1] = new double[layoutInfo.height];

    System.arraycopy(layoutInfo.weightX, 0, weights[0], 0, layoutInfo.width);
    System.arraycopy(layoutInfo.weightY, 0, weights[1], 0, layoutInfo.height);

    return weights;
  }

    /**
     * Determines which cell in the layout grid contains the point
     * specified by <code>(x,&nbsp;y)</code>. Each cell is identified
     * by its column index (ranging from 0 to the number of columns
     * minus 1) and its row index (ranging from 0 to the number of
     * rows minus 1).
     * <p>
     * If the <code>(x,&nbsp;y)</code> point lies
     * outside the grid, the following rules are used.
     * The column index is returned as zero if <code>x</code> lies to the
     * left of the layout, and as the number of columns if <code>x</code> lies
     * to the right of the layout. The row index is returned as zero
     * if <code>y</code> lies above the layout,
     * and as the number of rows if <code>y</code> lies
     * below the layout.
     * @param      x    the <i>x</i> coordinate of a point.
     * @param      y    the <i>y</i> coordinate of a point.
     * @return     an ordered pair of indexes that indicate which cell
     *             in the layout grid contains the point
     *             (<i>x</i>,&nbsp;<i>y</i>).
     * @since      JDK1.1
     */
  public Point location(int x, int y) {
    Point loc = new Point(0,0);
    int i, d;

    if (layoutInfo == null)
      return loc;

    d = layoutInfo.startx;
    for (i=0; i<layoutInfo.width; i++) {
      d += layoutInfo.prefWidth[i];
      if (d > x)
        break;
    }
    loc.x = i;

    d = layoutInfo.starty;
    for (i=0; i<layoutInfo.height; i++) {
      d += layoutInfo.prefHeight[i];
      if (d > y)
        break;
    }
    loc.y = i;

    return loc;
  }

  /**
   * Adds the specified component with the specified name to the layout.
   * @param      name         the name of the component.
   * @param      comp         the component to be added.
   */
  public void addLayoutComponent(String name, Component comp) {
  }

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object.
     * @param      comp         the component to be added.
     * @param      constraints  an object that determines how
     *                              the component is added to the layout.
     */
    public void addLayoutComponent(Component comp, Object constraints) {
      if (constraints instanceof MyGridBagConstraints) {
            setConstraints(comp, (MyGridBagConstraints)constraints);
        } else if (constraints != null) {
            throw new IllegalArgumentException("cannot add to layout: constraint must be a MyGridBagConstraint");
        }
    }

  /**
     * Removes the specified component from this layout.
     * <p>
     * Most applications do not call this method directly.
     * @param    comp   the component to be removed.
     * @see      java.awt.Container#remove(java.awt.Component)
     * @see      java.awt.Container#removeAll()
   */
  public void removeLayoutComponent(Component comp) {
    removeConstraints(comp);
  }

  /**
     * Determines the preferred size of the <code>target</code>
     * container using this grid bag layout.
     * <p>
     * Most applications do not call this method directly.
     * @param     parent   the container in which to do the layout.
     * @see       java.awt.Container#getPreferredSize
   */
  public Dimension preferredLayoutSize(Container parent) {
    MyGridBagLayoutInfo info = GetLayoutInfo(parent);
    return GetReqSize(parent, info, PREFERREDSIZE);
  }

  /**
     * Determines the minimum size of the <code>target</code> container
     * using this grid bag layout.
     * <p>
     * Most applications do not call this method directly.
     * @param     parent   the container in which to do the layout.
     * @see       java.awt.Container#doLayout
   */
  public Dimension minimumLayoutSize(Container parent) {
    MyGridBagLayoutInfo info = GetLayoutInfo(parent);
    return GetReqSize(parent, info, MINSIZE);
  }

    /**
     * Returns the maximum dimensions for this layout given the components
     * in the specified target container.
     * @param target the component which needs to be laid out
     * @see Container
     * @see #minimumLayoutSize(Container)
     * @see #preferredLayoutSize(Container)
     */
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }

    /**
     * Invalidates the layout, indicating that if the layout manager
     * has cached information it should be discarded.
     */
    public void invalidateLayout(Container target) {
    }

  /**
   * Lays out the specified container using this grid bag layout.
   * This method reshapes components in the specified container in
   * order to satisfy the contraints of this <code>GridBagLayout</code>
   * object.
   * <p>
   * Most applications do not call this method directly.
   * @param parent the container in which to do the layout.
   * @see java.awt.Container
   * @see java.awt.Container#doLayout
   */
  public void layoutContainer(Container parent) {
    ArrangeGrid(parent);
  }

  /**
     * Returns a string representation of this grid bag layout's values.
     * @return     a string representation of this grid bag layout.
   */
  public String toString() {
    return getClass().getName();
  }

  /**
   * Print the layout information.  Useful for debugging.
   */

  /* DEBUG
   *
   *  protected void DumpLayoutInfo(GridBagLayoutInfo s) {
   *    int x;
   *
   *    System.out.println("Col\tWidth\tWeight");
   *    for (x=0; x<s.width; x++) {
   *      System.out.println(x + "\t" +
   *                     s.prefWidth[x] + "\t" +
   *                     s.weightX[x]);
   *    }
   *    System.out.println("Row\tHeight\tWeight");
   *    for (x=0; x<s.height; x++) {
   *      System.out.println(x + "\t" +
   *                     s.prefHeight[x] + "\t" +
   *                     s.weightY[x]);
   *    }
   *  }
   */

  /**
   * Print the layout constraints.  Useful for debugging.
   */

  /* DEBUG
   *
   *  protected void DumpConstraints(GridBagConstraints constraints) {
   *    System.out.println(
   *                   "wt " +
   *                   constraints.weightx +
   *                   " " +
   *                   constraints.weighty +
   *                   ", " +
   *
   *                   "box " +
   *                   constraints.gridx +
   *                   " " +
   *                   constraints.gridy +
   *                   " " +
   *                   constraints.gridwidth +
   *                   " " +
   *                   constraints.gridheight +
   *                   ", " +
   *
   *                   "min " +
   *                   constraints.prefWidth +
   *                   " " +
   *                   constraints.prefHeight +
   *                   ", " +
   *
   *                   "pad " +
   *                   constraints.insets.bottom +
   *                   " " +
   *                   constraints.insets.left +
   *                   " " +
   *                   constraints.insets.right +
   *                   " " +
   *                   constraints.insets.top +
   *                   " " +
   *                   constraints.ipadx +
   *                   " " +
   *                   constraints.ipady);
   *  }
   */

  /*
   * Fill in an instance of the above structure for the current set
   * of managed children.  This requires three passes through the
   * set of children:
   *
   * 1) Figure out the dimensions of the layout grid
   * 2) Determine which cells the components occupy
   * 3) Distribute the weights and min sizes amoung the rows/columns.
   *
   * This also caches the minsizes for all the children when they are
   * first encountered (so subsequent loops don't need to ask again).
   */

  protected MyGridBagLayoutInfo GetLayoutInfo(Container parent) {
   synchronized (parent.getTreeLock()) {
    MyGridBagLayoutInfo r = new MyGridBagLayoutInfo();
    Component comp;
    MyGridBagConstraints constraints;
    Dimension d;
    Component components[] = parent.getComponents();

    int compindex, i, k, px, py, pixels_diff, nextSize;
    int curX, curY, curWidth, curHeight, curRow, curCol;
    double weight_diff, weight;
    int xMax[], yMax[];

    /*
     * Pass #1
     *
     * Figure out the dimensions of the layout grid (use a value of 1 for
     * zero or negative widths and heights).
     */

    r.width = r.height = 0;
    curRow = curCol = -1;
    xMax = new int[MAXGRIDSIZE];
    yMax = new int[MAXGRIDSIZE];

    for (compindex = 0 ; compindex < components.length ; compindex++) {
      comp = components[compindex];
      if (!comp.isVisible())
        continue;
      constraints = lookupConstraints(comp);

      curX = constraints.gridx;
      curY = constraints.gridy;
      curWidth = constraints.gridwidth;
      if (curWidth <= 0)
        curWidth = 1;
      curHeight = constraints.gridheight;
      if (curHeight <= 0)
        curHeight = 1;

      /* If x or y is negative, then use relative positioning: */
      if (curX < 0 && curY < 0) {
        if (curRow >= 0)
          curY = curRow;
        else if (curCol >= 0)
          curX = curCol;
        else
          curY = 0;
      }
      if (curX < 0) {
        px = 0;
        for (i = curY; i < (curY + curHeight); i++)
          px = Math.max(px, xMax[i]);

        curX = px - curX - 1;
        if(curX < 0)
          curX = 0;
      }
      else if (curY < 0) {
        py = 0;
        for (i = curX; i < (curX + curWidth); i++)
          py = Math.max(py, yMax[i]);

        curY = py - curY - 1;
        if(curY < 0)
          curY = 0;
      }

      /* Adjust the grid width and height */
      for (px = curX + curWidth; r.width < px; r.width++);
      for (py = curY + curHeight; r.height < py; r.height++);

      /* Adjust the xMax and yMax arrays */
      for (i = curX; i < (curX + curWidth); i++) { yMax[i] = py; }
      for (i = curY; i < (curY + curHeight); i++) { xMax[i] = px; }

      /* Cache the current slave's size. */
      d = comp.getPreferredSize();
      constraints.prefWidth = d.width;
      constraints.prefHeight = d.height;
      d = comp.getMinimumSize();
      constraints.minWidth = d.width;
      constraints.minHeight = d.height;

      /* Zero width and height must mean that this is the last item (or
       * else something is wrong). */
      if (constraints.gridheight == 0 && constraints.gridwidth == 0)
        curRow = curCol = -1;

      /* Zero width starts a new row */
      if (constraints.gridheight == 0 && curRow < 0)
        curCol = curX + curWidth;

      /* Zero height starts a new column */
      else if (constraints.gridwidth == 0 && curCol < 0)
        curRow = curY + curHeight;
    }

    /*
     * Apply minimum row/column dimensions
     */
    if (columnWidths != null && r.width < columnWidths.length)
      r.width = columnWidths.length;
    if (rowHeights != null && r.height < rowHeights.length)
      r.height = rowHeights.length;

    /*
     * Pass #2
     *
     * Negative values for gridX are filled in with the current x value.
     * Negative values for gridY are filled in with the current y value.
     * Negative or zero values for gridWidth and gridHeight end the current
     *  row or column, respectively.
     */

    curRow = curCol = -1;
    xMax = new int[MAXGRIDSIZE];
    yMax = new int[MAXGRIDSIZE];

    for (compindex = 0 ; compindex < components.length ; compindex++) {
      comp = components[compindex];
      if (!comp.isVisible())
        continue;
      constraints = lookupConstraints(comp);

      curX = constraints.gridx;
      curY = constraints.gridy;
      curWidth = constraints.gridwidth;
      curHeight = constraints.gridheight;

      /* If x or y is negative, then use relative positioning: */
      if (curX < 0 && curY < 0) {
        if(curRow >= 0)
          curY = curRow;
        else if(curCol >= 0)
          curX = curCol;
        else
          curY = 0;
      }

      if (curX < 0) {
        if (curHeight <= 0) {
          curHeight += r.height - curY;
          if (curHeight < 1)
            curHeight = 1;
        }

        px = 0;
        for (i = curY; i < (curY + curHeight); i++)
          px = Math.max(px, xMax[i]);

        curX = px - curX - 1;
        if(curX < 0)
          curX = 0;
      }
      else if (curY < 0) {
        if (curWidth <= 0) {
          curWidth += r.width - curX;
          if (curWidth < 1)
            curWidth = 1;
        }

        py = 0;
        for (i = curX; i < (curX + curWidth); i++)
          py = Math.max(py, yMax[i]);

        curY = py - curY - 1;
        if(curY < 0)
          curY = 0;
      }

      if (curWidth <= 0) {
        curWidth += r.width - curX;
        if (curWidth < 1)
          curWidth = 1;
      }

      if (curHeight <= 0) {
        curHeight += r.height - curY;
        if (curHeight < 1)
          curHeight = 1;
      }

      px = curX + curWidth;
      py = curY + curHeight;

      for (i = curX; i < (curX + curWidth); i++) { yMax[i] = py; }
      for (i = curY; i < (curY + curHeight); i++) { xMax[i] = px; }

      /* Make negative sizes start a new row/column */
      if (constraints.gridheight == 0 && constraints.gridwidth == 0)
        curRow = curCol = -1;
      if (constraints.gridheight == 0 && curRow < 0)
        curCol = curX + curWidth;
      else if (constraints.gridwidth == 0 && curCol < 0)
        curRow = curY + curHeight;

      /* Assign the new values to the gridbag slave */
      constraints.tempX = curX;
      constraints.tempY = curY;
      constraints.tempWidth = curWidth;
      constraints.tempHeight = curHeight;
    }

    /*
     * Apply minimum row/column dimensions and weights
     */
    if (columnWidths != null)
      System.arraycopy(columnWidths, 0, r.prefWidth, 0, columnWidths.length);
    if (rowHeights != null)
      System.arraycopy(rowHeights, 0, r.prefHeight, 0, rowHeights.length);
    if (columnWeights != null)
      System.arraycopy(columnWeights, 0, r.weightX, 0, columnWeights.length);
    if (rowWeights != null)
      System.arraycopy(rowWeights, 0, r.weightY, 0, rowWeights.length);

    /*
     * Pass #3
     *
     * Distribute the minimun widths and weights:
     */

    nextSize = Integer.MAX_VALUE;

    for (i = 1;
         i != Integer.MAX_VALUE;
         i = nextSize, nextSize = Integer.MAX_VALUE) {
      for (compindex = 0 ; compindex < components.length ; compindex++) {
        comp = components[compindex];
        if (!comp.isVisible())
          continue;
        constraints = lookupConstraints(comp);

        if (constraints.tempWidth == i) {
          px = constraints.tempX + constraints.tempWidth; /* right column */

          /*
           * Figure out if we should use this slave\'s weight.  If the weight
           * is less than the total weight spanned by the width of the cell,
           * then discard the weight.  Otherwise split the difference
           * according to the existing weights.
           */

          weight_diff = constraints.weightx;
          for (k = constraints.tempX; k < px; k++)
            weight_diff -= r.weightX[k];
          if (weight_diff > 0.0) {
            weight = 0.0;
            for (k = constraints.tempX; k < px; k++)
              weight += r.weightX[k];
            for (k = constraints.tempX; weight > 0.0 && k < px; k++) {
              double wt = r.weightX[k];
              double dx = (wt * weight_diff) / weight;
              r.weightX[k] += dx;
              weight_diff -= dx;
              weight -= wt;
            }
            /* Assign the remainder to the rightmost cell */
            r.weightX[px-1] += weight_diff;
          }

          /*
           * Calculate the prefWidth array values.
           * First, figure out how wide the current slave needs to be.
           * Then, see if it will fit within the current prefWidth values.
           * If it will not fit, add the difference according to the
           * weightX array.
           */

          pixels_diff =
            constraints.prefWidth + constraints.ipadx +
            constraints.insets.left + constraints.insets.right;

          for (k = constraints.tempX; k < px; k++)
            pixels_diff -= r.prefWidth[k];
          if (pixels_diff > 0) {
            weight = 0.0;
            for (k = constraints.tempX; k < px; k++)
              weight += r.weightX[k];
            for (k = constraints.tempX; weight > 0.0 && k < px; k++) {
              double wt = r.weightX[k];
              int dx = (int)((wt * ((double)pixels_diff)) / weight);
              r.prefWidth[k] += dx;
              pixels_diff -= dx;
              weight -= wt;
            }
            /* Any leftovers go into the rightmost cell */
            r.prefWidth[px-1] += pixels_diff;
          }
        }
        else if (constraints.tempWidth > i && constraints.tempWidth < nextSize)
          nextSize = constraints.tempWidth;


        if (constraints.tempHeight == i) {
          py = constraints.tempY + constraints.tempHeight; /* bottom row */

          /*
           * Figure out if we should use this slave\'s weight.  If the weight
           * is less than the total weight spanned by the height of the cell,
           * then discard the weight.  Otherwise split it the difference
           * according to the existing weights.
           */

          weight_diff = constraints.weighty;
          for (k = constraints.tempY; k < py; k++)
            weight_diff -= r.weightY[k];
          if (weight_diff > 0.0) {
            weight = 0.0;
            for (k = constraints.tempY; k < py; k++)
              weight += r.weightY[k];
            for (k = constraints.tempY; weight > 0.0 && k < py; k++) {
              double wt = r.weightY[k];
              double dy = (wt * weight_diff) / weight;
              r.weightY[k] += dy;
              weight_diff -= dy;
              weight -= wt;
            }
            /* Assign the remainder to the bottom cell */
            r.weightY[py-1] += weight_diff;
          }

          /*
           * Calculate the prefHeight array values.
           * First, figure out how tall the current slave needs to be.
           * Then, see if it will fit within the current prefHeight values.
           * If it will not fit, add the difference according to the
           * weightY array.
           */

          pixels_diff =
            constraints.prefHeight + constraints.ipady +
            constraints.insets.top + constraints.insets.bottom;
          for (k = constraints.tempY; k < py; k++)
            pixels_diff -= r.prefHeight[k];
          if (pixels_diff > 0) {
            weight = 0.0;
            for (k = constraints.tempY; k < py; k++)
              weight += r.weightY[k];
            for (k = constraints.tempY; weight > 0.0 && k < py; k++) {
              double wt = r.weightY[k];
              int dy = (int)((wt * ((double)pixels_diff)) / weight);
              r.prefHeight[k] += dy;
              pixels_diff -= dy;
              weight -= wt;
            }
            /* Any leftovers go into the bottom cell */
            r.prefHeight[py-1] += pixels_diff;
          }
        }
        else if (constraints.tempHeight > i &&
                 constraints.tempHeight < nextSize)
          nextSize = constraints.tempHeight;
      }
    }

    return r;
   }
  }

  /*
   * Adjusts the x, y, width, and height fields to the correct
   * values depending on the constraint geometry and pads.
   */
  protected void AdjustForGravity(MyGridBagConstraints constraints,
                                  Rectangle r) {
    int diffx, diffy;

    r.x += constraints.insets.left;
    r.width -= (constraints.insets.left + constraints.insets.right);
    r.y += constraints.insets.top;
    r.height -= (constraints.insets.top + constraints.insets.bottom);

    diffx = 0;
    if ((constraints.fill != MyGridBagConstraints.HORIZONTAL &&
         constraints.fill != MyGridBagConstraints.BOTH)
        && (r.width > (constraints.prefWidth + constraints.ipadx))) {
      diffx = r.width - (constraints.prefWidth + constraints.ipadx);
      r.width = constraints.prefWidth + constraints.ipadx;
    }

    diffy = 0;
    if ((constraints.fill != MyGridBagConstraints.VERTICAL &&
         constraints.fill != MyGridBagConstraints.BOTH)
        && (r.height > (constraints.prefHeight + constraints.ipady))) {
      diffy = r.height - (constraints.prefHeight + constraints.ipady);
      r.height = constraints.prefHeight + constraints.ipady;
    }

    switch (constraints.anchor) {
    case MyGridBagConstraints.CENTER:
      r.x += diffx/2;
      r.y += diffy/2;
      break;
    case MyGridBagConstraints.NORTH:
      r.x += diffx/2;
      break;
    case MyGridBagConstraints.NORTHEAST:
      r.x += diffx;
      break;
    case MyGridBagConstraints.EAST:
      r.x += diffx;
      r.y += diffy/2;
      break;
    case MyGridBagConstraints.SOUTHEAST:
      r.x += diffx;
      r.y += diffy;
      break;
    case MyGridBagConstraints.SOUTH:
      r.x += diffx/2;
      r.y += diffy;
      break;
    case MyGridBagConstraints.SOUTHWEST:
      r.y += diffy;
      break;
    case MyGridBagConstraints.WEST:
      r.y += diffy/2;
      break;
    case MyGridBagConstraints.NORTHWEST:
      break;
    default:
      throw new IllegalArgumentException("illegal anchor value");
    }
  }

  /*
   * Figure out the minimum size of the
   * master based on the information from GetLayoutInfo()
   */
  protected Dimension GetReqSize(Container parent, MyGridBagLayoutInfo info,
                                 int sizeflag) {
    Dimension d = new Dimension();
    int i, t;
    Insets insets = parent.getInsets();

    t = 0;
    for(i = 0; i < info.width; i++)
      t += sizeflag == MINSIZE ? info.minWidth[i] : info.prefWidth[i];
    d.width = t + insets.left + insets.right;

    t = 0;
    for(i = 0; i < info.height; i++)
      t += sizeflag == MINSIZE ? info.minHeight[i] : info.prefHeight[i];
    d.height = t + insets.top + insets.bottom;

    return d;
  }

  /*
   * Lay out the grid
   */
  protected void ArrangeGrid(Container parent) {
    Component comp;
    int compindex;
    MyGridBagConstraints constraints;
    Insets insets = parent.getInsets();
    Component components[] = parent.getComponents();
    Dimension d;
    Rectangle r = new Rectangle();
    int i, diffw, diffh;
    double weight;
    MyGridBagLayoutInfo info;

    /*
     * If the parent has no slaves anymore, then don't do anything
     * at all:  just leave the parent's size as-is.
     */
    if (components.length == 0 &&
        (columnWidths == null || columnWidths.length == 0) &&
        (rowHeights == null || rowHeights.length == 0)) {
      return;
    }

    /*
     * Pass #1: scan all the slaves to figure out the total amount
     * of space needed.
     */

    info = GetLayoutInfo(parent);
    d = GetReqSize(parent, info, PREFERREDSIZE);

    layoutInfo = info;
    r.width = d.width;
    r.height = d.height;

    /*
     * DEBUG
     *
     * DumpLayoutInfo(info);
     * for (compindex = 0 ; compindex < components.length ; compindex++) {
     * comp = components[compindex];
     * if (!comp.isVisible())
     *  continue;
     * constraints = lookupConstraints(comp);
     * DumpConstraints(constraints);
     * }
     * System.out.println("minSize " + r.width + " " + r.height);
     */

    /*
     * If the current dimensions of the window don't match the desired
     * dimensions, then adjust the prefWidth and prefHeight arrays
     * according to the weights.
     */

    diffw = parent.getWidth() - r.width;
    if (diffw > 0) {
      weight = 0.0;
      for (i = 0; i < info.width; i++)
        weight += info.weightX[i];
      if (weight > 0.0) {
        for (i = 0; i < info.width; i++) {
          int dx = (int)(( ((double)diffw) * info.weightX[i]) / weight);
          info.prefWidth[i] += dx;
          r.width += dx;
        }
      }
      diffw = parent.getWidth() - r.width;
    }
    else if (diffw < 0) {
      boolean minimized[] = new boolean[info.width];
      boolean incomplete;
      for (i = 0; i < info.width; i++) {
        weight = info.prefWidth[i] - info.minWidth[i];
        if (weight > 0)
          info.weightX[i] = weight;
        else
          minimized[i] = true;
      }
      do {
        incomplete = false;
        weight = 0.0;
        for (i = 0; i < info.width; i++)
          if (!minimized[i])
            weight += info.weightX[i];
        if (weight == 0.0)
          break;
        for (i = 0; i < info.width; i++) {
          if (minimized[i])
            continue;
          int dx = - (int) ((((double) diffw) * info.weightX[i]) / weight);
          int newWidth = info.prefWidth[i] - dx;
          if (newWidth < info.minWidth[i]) {
            minimized[i] = true;
            incomplete = true;
            dx = info.prefWidth[i] - info.minWidth[i];
            info.prefWidth[i] = info.minWidth[i];
          } else
            info.prefWidth[i] -= dx;
          r.width -= dx;
        }
      } while (incomplete);
      diffw = parent.getWidth() - r.width;
    }
    else {
      diffw = 0;
    }

    diffh = parent.getHeight() - r.height;
    if (diffh > 0) {
      weight = 0.0;
      for (i = 0; i < info.height; i++)
        weight += info.weightY[i];
      if (weight > 0.0) {
        for (i = 0; i < info.height; i++) {
          int dy = (int)(( ((double)diffh) * info.weightY[i]) / weight);
          info.prefHeight[i] += dy;
          r.height += dy;
        }
      }
      diffh = parent.getHeight() - r.height;
    }
    else if (diffh < 0) {
      boolean minimized[] = new boolean[info.height];
      boolean incomplete;
      for (i = 0; i < info.height; i++) {
        weight = info.prefHeight[i] - info.minHeight[i];
        if (weight > 0)
          info.weightY[i] = weight;
        else
          minimized[i] = true;
      }
      do {
        incomplete = false;
        weight = 0.0;
        for (i = 0; i < info.height; i++)
          if (!minimized[i])
            weight += info.weightY[i];
        if (weight == 0.0)
            break;
        for (i = 0; i < info.height; i++) {
          if (minimized[i])
            continue;
          int dx = - (int) ((((double) diffh) * info.weightY[i]) / weight);
          int newHeight = info.prefHeight[i] - dx;
          if (newHeight < info.minHeight[i]) {
            minimized[i] = true;
            incomplete = true;
            dx = info.prefHeight[i] - info.minHeight[i];
            info.prefHeight[i] = info.minHeight[i];
          } else
            info.prefHeight[i] -= dx;
          r.height -= dx;
        }
      } while (incomplete);
      diffh = parent.getHeight() - r.height;
    }
    else {
      diffh = 0;
    }

    /*
     * DEBUG
     *
     * System.out.println("Re-adjusted:");
     * DumpLayoutInfo(info);
     */

    /*
     * Now do the actual layout of the slaves using the layout information
     * that has been collected.
     */

    info.startx = diffw/2 + insets.left;
    info.starty = diffh/2 + insets.top;

    for (compindex = 0 ; compindex < components.length ; compindex++) {
      comp = components[compindex];
      if (!comp.isVisible())
        continue;
      constraints = lookupConstraints(comp);

      r.x = info.startx;
      for(i = 0; i < constraints.tempX; i++)
        r.x += info.prefWidth[i];

      r.y = info.starty;
      for(i = 0; i < constraints.tempY; i++)
        r.y += info.prefHeight[i];

      r.width = 0;
      for(i = constraints.tempX;
          i < (constraints.tempX + constraints.tempWidth);
          i++) {
        r.width += info.prefWidth[i];
      }

      r.height = 0;
      for(i = constraints.tempY;
          i < (constraints.tempY + constraints.tempHeight);
          i++) {
        r.height += info.prefHeight[i];
      }

      AdjustForGravity(constraints, r);

      /*
       * If the window is too small to be interesting then
       * unmap it.  Otherwise configure it and then make sure
       * it's mapped.
       */

      if ((r.width <= 0) || (r.height <= 0)) {
        comp.setBounds(0, 0, 0, 0);
      }
      else {
        if (comp.getX() != r.x || comp.getY() != r.y ||
            comp.getWidth() != r.width || comp.getHeight() != r.height) {
          comp.setBounds(r.x, r.y, r.width, r.height);
        }
      }
    }
  }
}
