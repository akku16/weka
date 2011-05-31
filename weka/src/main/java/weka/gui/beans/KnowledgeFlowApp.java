/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    KnowledgeFlowApp.java
 *    Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.Beans;
import java.beans.Customizer;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.beancontext.BeanContextChild;
import java.beans.beancontext.BeanContextSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JWindow;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import weka.core.Copyright;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instances;
import weka.core.Memory;
import weka.core.SerializedObject;
import weka.core.Utils;
import weka.core.xml.KOML;
import weka.core.xml.XStream;
import weka.gui.ExtensionFileFilter;
import weka.gui.GenericObjectEditor;
import weka.gui.GenericPropertiesCreator;
import weka.gui.HierarchyPropertyParser;
import weka.gui.LookAndFeel;
import weka.gui.beans.xml.XMLBeans;
import weka.gui.visualize.PrintablePanel;

/**
 * Main GUI class for the KnowledgeFlow. Modifications to allow interoperability
 * with swt provided by Davide Zerbetto (davide dot zerbetto at eng dot it).
 *
 * @author Mark Hall
 * @version  $Revision$
 * @since 1.0
 * @see JPanel
 * @see PropertyChangeListener
 */
public class KnowledgeFlowApp
  extends JPanel
  implements PropertyChangeListener, BeanCustomizer.ModifyListener {

  /** for serialization */
  private static final long serialVersionUID = -7064906770289728431L;

  /**
   * Location of the property file for the KnowledgeFlowApp
   */
  protected static String PROPERTY_FILE = "weka/gui/beans/Beans.props";

  /** Contains the editor properties */
  protected static Properties BEAN_PROPERTIES;

  private static ArrayList<Properties> BEAN_PLUGINS_PROPERTIES = 
    new ArrayList<Properties>();

  /**
   * Holds the details needed to construct button bars for various supported
   * classes of weka algorithms/tools 
   */
  private static Vector TOOLBARS = new Vector();
  
  public static void addToPluginBeanProps(File beanPropsFile) throws Exception {
    Properties tempP = new Properties();
    
    tempP.load(new FileInputStream(beanPropsFile));
    if (!BEAN_PLUGINS_PROPERTIES.contains(tempP)) {
      BEAN_PLUGINS_PROPERTIES.add(tempP);
    }
  }
  
  public static void removeFromPluginBeanProps(File beanPropsFile) throws Exception {
    Properties tempP = new Properties();
    
    tempP.load(new FileInputStream(beanPropsFile));
    if (BEAN_PLUGINS_PROPERTIES.contains(tempP)) {
      BEAN_PLUGINS_PROPERTIES.remove(tempP);
    }
  }

  /**
   * Loads KnowledgeFlow properties and any plugins (adds jars to
   * the classpath)
   */
  public static void loadProperties() {
    if (BEAN_PROPERTIES == null) {
      weka.core.WekaPackageManager.loadPackages(false);
      System.out.println("[KnowledgeFlow] Loading properties and plugins...");
      /** Loads the configuration property file */
      //  static {
      // Allow a properties file in the current directory to override
      try {
        BEAN_PROPERTIES = Utils.readProperties(PROPERTY_FILE);
        java.util.Enumeration keys =
          (java.util.Enumeration)BEAN_PROPERTIES.propertyNames();
        if (!keys.hasMoreElements()) {
          throw new Exception( "Could not read a configuration file for the bean\n"
                               +"panel. An example file is included with the Weka distribution.\n"
                               +"This file should be named \"" + PROPERTY_FILE + "\" and\n"
                               +"should be placed either in your user home (which is set\n"
                               + "to \"" + System.getProperties().getProperty("user.home") + "\")\n"
                               + "or the directory that java was started from\n");
        }
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(null,
                                      ex.getMessage(),
                                      "KnowledgeFlow",
                                      JOptionPane.ERROR_MESSAGE);
      }


      // try and load any plugin beans properties
      File pluginDir = new File(System.getProperty("user.home")
                                +File.separator+".knowledgeFlow"
                                +File.separator+"plugins");
      /*if (pluginDir.exists() && pluginDir.isDirectory()) {
        BEAN_PLUGINS_PROPERTIES = new ArrayList<Properties>();
        // How many sub-dirs are there?
        File[] contents = pluginDir.listFiles();
        for (int i = 0; i < contents.length; i++) {
          if (contents[i].isDirectory() && 
              contents[i].listFiles().length > 0) {
            try {      
              Properties tempP = new Properties();
              File propFile = new File(contents[i].getPath()
                                       + File.separator
                                       + "Beans.props");
              tempP.load(new FileInputStream(propFile));
              BEAN_PLUGINS_PROPERTIES.add(tempP);

              // Now try and add all jar files in this directory to the classpath
              File anyJars[] = contents[i].listFiles();
              for (int j = 0; j < anyJars.length; j++) {
                if (anyJars[j].getPath().endsWith(".jar")) {
                  System.out.println("[KnowledgeFlow] Plugins: adding "+anyJars[j].getPath()
                                     +" to classpath...");
                  ClassloaderUtil.addFile(anyJars[j].getPath());
                }
              }
            } catch (Exception ex) {
              // Don't make a fuss
              System.err.println("[KnowledgeFlow] Warning: Unable to load bean properties for plugin "
                                 +"directory: " + contents[i].getPath());
            }
          }
          //        BEAN_PLUGINS_PROPERTIES = new Properties();
          //        BEAN_PLUGINS_PROPERTIES.load(new FileInputStream(pluginDir));
        }
      } else {
        // make the plugin directory for the user
        pluginDir.mkdir();
      } */
    }
  }
  
  public static void reInitialize() {
    if (BEAN_PROPERTIES == null) {
      loadProperties();
    }
    init();
  }  

  /**
   * Initializes the temporary files necessary to construct the toolbars
   * from.
   */
  private static void init() {
    System.out.println("[KnowledgeFlow] Initializing KF...");

    try {
      TOOLBARS = new Vector();
      
      TreeMap wrapList = new TreeMap();
      Properties GEOProps = GenericPropertiesCreator.getGlobalOutputProperties();
      
      if (GEOProps == null) {
        GenericPropertiesCreator creator = new GenericPropertiesCreator();


        if (creator.useDynamic()) {
          creator.execute(false);
          /* now process the keys in the GenericObjectEditor.props. For each
           key that has an entry in the Beans.props associating it with a
           bean component a button tool bar will be created */
          GEOProps = creator.getOutputProperties();
        } else {
          // Read the static information from the GenericObjectEditor.props
          GEOProps = Utils.readProperties("weka/gui/GenericObjectEditor.props");
        }
      }
      Enumeration en = GEOProps.propertyNames();
      while (en.hasMoreElements()) {
	String geoKey = (String)en.nextElement();
//	System.err.println("GEOKey " + geoKey);

	// try to match this key with one in the Beans.props file
	String beanCompName = BEAN_PROPERTIES.getProperty(geoKey);
	if (beanCompName != null) {
	  // add details necessary to construct a button bar for this class
	  // of algorithms
	  Vector newV = new Vector();
	  // check for a naming alias for this toolbar
	  String toolBarNameAlias = 
	    BEAN_PROPERTIES.getProperty(geoKey+".alias");
	  String toolBarName = (toolBarNameAlias != null) ?
	    toolBarNameAlias :
	    geoKey.substring(geoKey.lastIndexOf('.')+1, geoKey.length());

          // look for toolbar ordering information for this wrapper type
          String order = 
            BEAN_PROPERTIES.getProperty(geoKey+".order");
          Integer intOrder = (order != null) ?
            new Integer(order) :
            new Integer(0);
            
	  // Name for the toolbar (name of weka algorithm class)
	  newV.addElement(toolBarName);
	  // Name of bean capable of handling this class of algorithm
	  newV.addElement(beanCompName);

	  // add the root package for this key
	  String rootPackage = geoKey.substring(0, geoKey.lastIndexOf('.'));

	  newV.addElement(rootPackage);

	  // All the weka algorithms of this class of algorithm
	  String wekaAlgs = GEOProps.getProperty(geoKey);

          Hashtable roots = GenericObjectEditor.sortClassesByRoot(wekaAlgs);
          Hashtable hpps = new Hashtable();
          Enumeration enm = roots.keys();
          while (enm.hasMoreElements()) {
            String root = (String) enm.nextElement();
            String classes = (String) roots.get(root);
            weka.gui.HierarchyPropertyParser hpp = 
              new weka.gui.HierarchyPropertyParser();
            hpp.build(classes, ", ");
//            System.err.println(hpp.showTree());
            hpps.put(root, hpp);
          }

	  //------ test the HierarchyPropertyParser
          /*  weka.gui.HierarchyPropertyParser hpp = 
	    new weka.gui.HierarchyPropertyParser();
	  hpp.build(wekaAlgs, ", ");

	  System.err.println(hpp.showTree()); */
	  // ----- end test the HierarchyPropertyParser
          //	  newV.addElement(hpp); // add the hierarchical property parser
	  newV.addElement(hpps); // add the hierarchical property parser

	  StringTokenizer st = new StringTokenizer(wekaAlgs, ", ");
	  while (st.hasMoreTokens()) {
	    String current = st.nextToken().trim();
	    newV.addElement(current);
	  }
          wrapList.put(intOrder, newV);
          //	  TOOLBARS.addElement(newV);
	}
      }
      Iterator keysetIt = wrapList.keySet().iterator();
      while (keysetIt.hasNext()) {
        Integer key = (Integer)keysetIt.next();
        Vector newV = (Vector)wrapList.get(key);
        if (newV != null) {
          TOOLBARS.addElement(newV);
        }
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null,
          "Could not read a configuration file for the generic objecte editor"
         +". An example file is included with the Weka distribution.\n"
         +"This file should be named \"GenericObjectEditor.props\" and\n"
         +"should be placed either in your user home (which is set\n"
         + "to \"" + System.getProperties().getProperty("user.home") + "\")\n"
         + "or the directory that java was started from\n",
         "KnowledgeFlow",
         JOptionPane.ERROR_MESSAGE);
    }

    try {
      String standardToolBarNames = 
	BEAN_PROPERTIES.
	getProperty("weka.gui.beans.KnowledgeFlow.standardToolBars");
      StringTokenizer st = new StringTokenizer(standardToolBarNames, ", ");
       while (st.hasMoreTokens()) {
	 String tempBarName = st.nextToken().trim();
	 // construct details for this toolbar
	 Vector newV = new Vector();
	 // add the name of the toolbar
	 newV.addElement(tempBarName);

	 // indicate that this is a standard toolbar (no wrapper bean)
	 newV.addElement("null");
	 String toolBarContents = 
	   BEAN_PROPERTIES.
	   getProperty("weka.gui.beans.KnowledgeFlow."+tempBarName);
	 StringTokenizer st2 = new StringTokenizer(toolBarContents, ", ");
	 while (st2.hasMoreTokens()) {
	   String tempBeanName = st2.nextToken().trim();
	   newV.addElement(tempBeanName);
	 }
	 TOOLBARS.addElement(newV);
       }       
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null,
				    ex.getMessage(),
				    "KnowledgeFlow",
				    JOptionPane.ERROR_MESSAGE);
    }
  } 
  
  protected class BeanIconRenderer extends DefaultTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value, 
        boolean sel, boolean expanded, boolean leaf, int row, 
        boolean hasFocus) {
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, 
          row, hasFocus);

      if (leaf) {
        Object userO = ((DefaultMutableTreeNode)value).getUserObject();
        if (userO instanceof JTreeLeafDetails) {
          Icon i = ((JTreeLeafDetails)userO).getIcon();
          if (i != null) {
            setIcon(i);
          }        
        }
      }
      return this;
    }    
  }
  
  protected class JTreeLeafDetails {
    protected String m_fullyQualifiedCompName = "";
    protected String m_leafLabel = "";
    protected String m_wekaAlgoName = "";
    
    protected Icon m_scaledIcon = null;
    
    protected MetaBean m_metaBean = null;
    
    protected String m_toolTipText = null;
    
    public JTreeLeafDetails(String fullName, Icon icon) {
      this(fullName, "", icon);
    }
    
    public JTreeLeafDetails(String name, MetaBean bean, Icon icon) {
      this(name, "", icon);
      
      m_metaBean = bean;
      m_toolTipText = "Hold down shift and click to remove";
    }
    
    public JTreeLeafDetails(String fullName, String wekaAlgoName, Icon icon) {
      m_fullyQualifiedCompName = fullName;
      m_wekaAlgoName = wekaAlgoName;
      m_leafLabel = (wekaAlgoName.length() > 0) ? wekaAlgoName : m_fullyQualifiedCompName;
      if (m_leafLabel.lastIndexOf('.') > 0) {
        m_leafLabel = m_leafLabel.substring(m_leafLabel.lastIndexOf('.') + 1, 
            m_leafLabel.length());
      }
      m_scaledIcon = icon;
    }
    
    public String getToolTipText() {
      return m_toolTipText;
    }
    
    public String toString() {
      return m_leafLabel;
    }
    
    public Icon getIcon() {
      return m_scaledIcon;
    }
    
    public void setIcon(Icon icon) {
      m_scaledIcon = icon;
    }
    
    public boolean isWrappedAlgorithm() {
      return (m_wekaAlgoName != null && m_wekaAlgoName.length() > 0);
    }
    
    public boolean isMetaBean() {
      return (m_metaBean != null);
    }
    
    public MetaBean getMetaBean() {
      return m_metaBean;
    }
    
    public void instantiateBean() {
      try {        
        if (isMetaBean()) {
          MetaBean copy = copyMetaBean(m_metaBean);
          copy.addPropertyChangeListenersSubFlow(KnowledgeFlowApp.this);
          m_toolBarBean = copy;
        } else {
          m_toolBarBean = Beans.instantiate(KnowledgeFlowApp.this.getClass().getClassLoader(), 
              m_fullyQualifiedCompName);
          if (isWrappedAlgorithm()) {
            Object algo = Beans.instantiate(KnowledgeFlowApp.this.getClass().getClassLoader(),
                m_wekaAlgoName);
            ((WekaWrapper)m_toolBarBean).setWrappedAlgorithm(algo);
          }
        }

        setCursor(Cursor.
            getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        m_mode = ADDING;
        m_pasteB.setEnabled(false);

      } catch (Exception ex) {
        System.err.println("Problem instantiating bean \"" 
            + m_fullyQualifiedCompName + "\" (JTreeLeafDetails.instantiateBean()");
        ex.printStackTrace();
      }      
    }
  }
  
  /**
   * Used for displaying the bean components and their visible
   * connections
   *
   * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
   * @version $Revision$
   * @since 1.0
   * @see PrintablePanel
   */
  protected class BeanLayout
    extends PrintablePanel {

    /** for serialization */
    private static final long serialVersionUID = -146377012429662757L;

    public void paintComponent(Graphics gx) {
      super.paintComponent(gx);
      BeanInstance.paintLabels(gx, m_mainKFPerspective.getCurrentTabIndex());
      BeanConnection.paintConnections(gx, m_mainKFPerspective.getCurrentTabIndex());
      //      BeanInstance.paintConnections(gx);
      if (m_mode == CONNECTING) {
	gx.drawLine(m_startX, m_startY, m_oldX, m_oldY);
      } else if (m_mode == SELECTING) {
        gx.drawRect((m_startX < m_oldX) ? m_startX : m_oldX, 
                    (m_startY < m_oldY) ? m_startY : m_oldY, 
                    Math.abs(m_oldX-m_startX), Math.abs(m_oldY-m_startY));
      }
    }

    public void doLayout() {
      super.doLayout();
      Vector comps = BeanInstance.getBeanInstances(m_mainKFPerspective.getCurrentTabIndex());
      for (int i = 0; i < comps.size(); i++) {
	BeanInstance bi = (BeanInstance)comps.elementAt(i);
	JComponent c = (JComponent)bi.getBean();
	Dimension d = c.getPreferredSize();
	c.setBounds(bi.getX(), bi.getY(), d.width, d.height);
	c.revalidate();
      }
    }
  }
  
  /**
   * Interface for perspectives
   *
   */
  public static interface KFPerspective {
    
    void setInstances(Instances insts);
    
    String getPerspectiveTitle();
    
    String getPerspectiveTipText();
    
    Icon getPerspectiveIcon();
  }
  
  
  /**
   * Main Knowledge Flow perspective
   *
   */
  private class MainKFPerspective extends JPanel implements KFPerspective {
    
    /** Holds the tabs of the perspective */
    protected JTabbedPane m_flowTabs = new JTabbedPane();
    
    /** List of layouts - one for each tab */
    protected List<BeanLayout> m_beanLayouts = new ArrayList<BeanLayout>();
    
    /** List of log panels - one for each tab */
    protected List<LogPanel> m_logPanels = new ArrayList<LogPanel>();
    
    /** List of environment variable settings - one for each tab */
    protected List<Environment> m_environmentSettings = new ArrayList<Environment>();
    
    /** List of flow file paths - one for each tab */
    protected List<File> m_filePaths = new ArrayList<File>();
    
    /** Keeps track of which tabs have been edited but not saved */
    protected List<Boolean> m_editedList = new ArrayList<Boolean>();
    
    /** Keeps track of which tabs have flows that are executing */
    protected List<Boolean> m_executingList = new ArrayList<Boolean>();
    
    /** Keeps track of any highlighted beans on the canvas for a tab */
    protected List<Vector> m_selectedBeans = new ArrayList<Vector>();
    
    /** Keeps track of the undo buffers for each tab */
    protected List<Stack<File>> m_undoBufferList = new ArrayList<Stack<File>>();
    
    public JTabbedPane getTabbedPane() {
      return m_flowTabs;
    }
    
    public synchronized int getNumTabs() {
      return m_flowTabs.getTabCount();
    }
    
    public synchronized int getCurrentTabIndex() {
      return m_flowTabs.getSelectedIndex();
    }
    
    public synchronized LogPanel getCurrentLogPanel() {
      if (getCurrentTabIndex() >= 0) {
        return m_logPanels.get(getCurrentTabIndex());
      }
      return null;
    }
    
    public synchronized LogPanel getLogPanel(int index) {
      if (index >= 0 && index < m_logPanels.size()) {
        return m_logPanels.get(index);
      }      
      return null;
    }
    
    public synchronized BeanLayout getCurrentBeanLayout() {
      if (getCurrentTabIndex() >= 0) {
        return m_beanLayouts.get(getCurrentTabIndex());
      }
      return null;
    }
    
    public synchronized BeanLayout getBeanLayout(int index) {
      if (index >= 0 && index < m_logPanels.size()) {
        return m_beanLayouts.get(getCurrentTabIndex());
      }      
      return null;
    }
    
    public synchronized void setActiveTab(int index) {
      if (index < getNumTabs() && index >= 0) {
        m_flowTabs.setSelectedIndex(index);
        
        System.out.println("Setting tab to : " + index);
        
        // set the log and layout to the ones belonging to this tab
        m_logPanel = m_logPanels.get(index);
        m_beanLayout = m_beanLayouts.get(index);
        m_flowEnvironment = m_environmentSettings.get(index);
        
        m_saveB.setEnabled(!getExecuting());
        m_saveBB.setEnabled(!getExecuting());
        m_playB.setEnabled(!getExecuting());
        m_playBB.setEnabled(!getExecuting());
        m_saveB.setEnabled(!getExecuting());
        m_saveBB.setEnabled(!getExecuting());
        m_cutB.setEnabled(getSelectedBeans().size() > 0 && !getExecuting());
        m_copyB.setEnabled(getSelectedBeans().size() > 0 && !getExecuting());
        m_deleteB.setEnabled(getSelectedBeans().size() > 0 && !getExecuting());
        m_selectAllB.setEnabled(BeanInstance.
            getBeanInstances(getCurrentTabIndex()).size() > 0 && !getExecuting());
        m_pasteB.setEnabled((m_pasteBuffer != null && m_pasteBuffer.length() > 0) 
            && !getExecuting());
        m_stopB.setEnabled(getExecuting());
        m_undoB.setEnabled(!getExecuting() && getUndoBuffer().size() > 0);
      }
    }
    
    public synchronized void setExecuting(boolean executing) {
      if (getNumTabs() > 0) {
        setExecuting(getCurrentTabIndex(), executing);
      }
    }
    
    public synchronized void setExecuting(int index, boolean executing) {
      if (index < getNumTabs() && index >= 0) {
        m_executingList.set(index, new Boolean(executing));
        ((CloseableTabTitle)m_flowTabs.getTabComponentAt(index)).setButtonEnabled(!executing);
        
        m_saveB.setEnabled(!getExecuting());
        m_saveBB.setEnabled(!getExecuting());
        m_playB.setEnabled(!getExecuting());
        m_playBB.setEnabled(!getExecuting());
        m_stopB.setEnabled(getExecuting());
        m_cutB.setEnabled(getSelectedBeans().size() > 0 && !getExecuting());
        m_deleteB.setEnabled(getSelectedBeans().size() > 0 && !getExecuting());
        m_selectAllB.setEnabled(BeanInstance.
            getBeanInstances(getCurrentTabIndex()).size() > 0 && !getExecuting());
        m_copyB.setEnabled(getSelectedBeans().size() > 0 && !getExecuting());
        m_pasteB.setEnabled((m_pasteBuffer != null && m_pasteBuffer.length() > 0) 
            && !getExecuting());
        m_undoB.setEnabled(!getExecuting() && getUndoBuffer().size() > 0);
      }
    }
    
    public synchronized boolean getExecuting() {
      return getExecuting(getCurrentTabIndex());
    }
    
    public synchronized boolean getExecuting(int index) {
      if (index < getNumTabs() && index >= 0) {
        return m_executingList.get(index);
      }
      return false;
    }
    
    public synchronized File getFlowFile() {
      if (getNumTabs() > 0) {
        return getFlowFile(getCurrentTabIndex());
      }
      return null;
    }
    
    public synchronized File getFlowFile(int index) {
      if (index >= 0 && index < getNumTabs()) {
        return m_filePaths.get(index);
      }
      
      return null;
    }
    
    public synchronized void setFlowFile(File flowFile) {
      if (getNumTabs() > 0) {
        setFlowFile(getCurrentTabIndex(), flowFile);
      }
    }
    
    public synchronized void setFlowFile(int index, File flowFile) {
      if (index < getNumTabs() && index >= 0) {
        m_filePaths.set(index, flowFile);
      }
    }
    
    public synchronized void setTabTitle(String title) {
      if (getNumTabs() > 0) {
        setTabTitle(getCurrentTabIndex(), title);
      }
    }
    
    public synchronized void setTabTitle(int index, String title) {
      if (index < getNumTabs() && index >= 0) {
        System.err.println("Setting tab title to " + title);
        m_flowTabs.setTitleAt(index, title);
        ((CloseableTabTitle)m_flowTabs.getTabComponentAt(index)).revalidate();
        
      }
    }
    
    public synchronized void setEditedStatus(boolean status) {
      if (getNumTabs() > 0) {
        int current = getCurrentTabIndex();
        setEditedStatus(current, status);
      }
    }
    
    public synchronized void setEditedStatus(int index, boolean status) {
      if (index < getNumTabs() && index >= 0) {
        Boolean newStatus = new Boolean(status);
        m_editedList.set(index, newStatus);
        ((CloseableTabTitle)m_flowTabs.getTabComponentAt(index)).setBold(status);
      }
    }
    
    /**
     * Get the edited status of the currently selected tab. Returns
     * false if there are no tabs
     * 
     * @return the edited status of the currently selected tab or
     * false if there are no tabs
     */
    public synchronized boolean getEditedStatus() {
      if (getNumTabs() <= 0) {
        return false;
      }
      
      return getEditedStatus(getCurrentTabIndex());
    }
    
    /**
     * Get the edited status of the tab at the supplied index. Returns
     * false if the index is out of bounds or there are no tabs
     * 
     * @param index the index of the tab to check
     * @return the edited status of the tab
     */
    public synchronized boolean getEditedStatus(int index) {
      if (index < getNumTabs() && index >= 0) {
        return m_editedList.get(index);
      }
      return false;
    }
    
    public synchronized void setUndoBuffer(Stack<File> buffer) {
      if (getNumTabs() > 0) {
        setUndoBuffer(getCurrentTabIndex(), buffer);
      }
    }
    
    public synchronized void setUndoBuffer(int index, Stack<File> buffer) {
      if (index < getNumTabs() && index >= 0) {
        m_undoBufferList.set(index, buffer);
      }
    }
    
    public synchronized Stack<File> getUndoBuffer() {
      if (getNumTabs() > 0) {
        return getUndoBuffer(getCurrentTabIndex());
      }
      return null;
    }
    
    public synchronized Stack<File> getUndoBuffer(int index) {
      if (index >= 0 && index < getNumTabs()) {
        return m_undoBufferList.get(index);
      }
      return null;
    }
    
    public synchronized Vector getSelectedBeans() {
      if (getNumTabs() > 0) {
        return getSelectedBeans(getCurrentTabIndex());
      }
      return null;
    }
    
    public synchronized Vector getSelectedBeans(int index) {
      if (index < getNumTabs() && index >= 0) {
        return m_selectedBeans.get(index);
      }
      return null;
    }
    
    public synchronized void setSelectedBeans(Vector beans) {
      if (getNumTabs() > 0) {
        setSelectedBeans(getCurrentTabIndex(), beans);
        m_cutB.setEnabled(getSelectedBeans().size() > 0 && !getExecuting());
        m_copyB.setEnabled(getSelectedBeans().size() > 0 && !getExecuting());
        m_deleteB.setEnabled(getSelectedBeans().size() > 0 && !getExecuting());
      }
    }
    
    public synchronized void setSelectedBeans(int index, Vector beans) {
      if (index < getNumTabs() && index >= 0) {
        // turn turn off any set ones
        for (int i = 0; i < m_selectedBeans.get(index).size(); i++) {
          BeanInstance temp = (BeanInstance)m_selectedBeans.get(index).elementAt(i);
          if (temp.getBean() instanceof Visible) {
            ((Visible)temp.getBean()).getVisual().setDisplayConnectors(false);
          } else if (temp.getBean() instanceof Note) {
            ((Note)temp.getBean()).setHighlighted(false);
          }
        }
        
        m_selectedBeans.set(index, beans);
        
        // highlight any new ones
        for (int i = 0; i < beans.size(); i++) {
          BeanInstance temp = (BeanInstance)beans.elementAt(i);
          if (temp.getBean() instanceof Visible) {
            ((Visible)temp.getBean()).getVisual().setDisplayConnectors(true);
          } else if (temp.getBean() instanceof Note) {
            ((Note)temp.getBean()).setHighlighted(true);
          }
        }
      }
    }
    
    public synchronized Environment getEnvironmentSettings() {
      if (getNumTabs() > 0) {
        return getEnvironmentSettings(getCurrentTabIndex());
      }
      return null;
    }
    
    public synchronized Environment getEnvironmentSettings(int index) {
      if (index < getNumTabs() && index >= 0) {
        return m_environmentSettings.get(index);
      }
      return null;
    }

    @Override
    public void setInstances(Instances insts) {
      // nothing to do as we don't process externally supplied instances
      
    }

    /**
     * Get the title of this perspective
     */
    public String getPerspectiveTitle() {
      return "Data mining processes";
    }

    /**
     * Get the tool tip text for this perspective
     */
    public String getPerspectiveTipText() {
      return "Knowledge Flow processes";
    }

    /**
     * Get the icon for this perspective
     */
    public Icon getPerspectiveIcon() {
      // TODO use the funky blue weka icon (scaled appropriately)
      return null;
    }
    
    public MainKFPerspective() {
      setLayout(new BorderLayout());
      add(m_flowTabs, BorderLayout.CENTER);

      m_flowTabs.addChangeListener(new ChangeListener() {
        // This method is called whenever the selected tab changes
        public void stateChanged(ChangeEvent evt) {

          // Get current tab
          int sel = m_flowTabs.getSelectedIndex();
          setActiveTab(sel);
        }
      });
    }
    
    public synchronized void removeTab(int tabIndex) {
      if (tabIndex < 0 || tabIndex >= getNumTabs()) {
        return;
      }
      
      if (m_editedList.get(tabIndex)) {
        // prompt for save
        String tabTitle = m_flowTabs.getTitleAt(tabIndex);
        String message = "\"" + tabTitle + "\" has been modified. Save changes " +
        		"before closing?";
        int result = JOptionPane.showConfirmDialog(KnowledgeFlowApp.this, message, 
            "Save changes", JOptionPane.YES_NO_CANCEL_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
          saveLayout(tabIndex, false);
        } else if (result == JOptionPane.CANCEL_OPTION) {
          return;
        }
      }
      
      BeanLayout bl = m_beanLayouts.get(tabIndex);           
      BeanInstance.removeBeanInstances(bl, tabIndex);
      BeanConnection.removeConnectionList(tabIndex);
      m_beanLayouts.remove(tabIndex);
      m_logPanels.remove(tabIndex);
      m_editedList.remove(tabIndex);
      m_environmentSettings.remove(tabIndex);
      m_selectedBeans.remove(tabIndex);
      bl = null;
      
      m_flowTabs.remove(tabIndex);
      
      if (getCurrentTabIndex() < 0) {
        m_beanLayout = null;
        m_logPanel = null;
      }
    }
    
    public synchronized void addTab(String tabTitle) {
      // new beans for this tab
      BeanInstance.addBeanInstances(new Vector(), null);
      // new connections for this tab
      BeanConnection.addConnections(new Vector());
      
      JPanel p1 = new JPanel();
      p1.setLayout(new BorderLayout());
      p1.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                             javax.swing.BorderFactory.
                             createTitledBorder("Knowledge Flow Layout"),
                    javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5)
                    ));
      BeanLayout tabBeanLayout = new BeanLayout();
      
      final JScrollPane js = new JScrollPane(tabBeanLayout);
      p1.add(js, BorderLayout.CENTER);
      js.getVerticalScrollBar().setUnitIncrement(m_ScrollBarIncrementLayout);
      js.getHorizontalScrollBar().setUnitIncrement(m_ScrollBarIncrementLayout);
      

      configureBeanLayout(tabBeanLayout);
      m_beanLayouts.add(tabBeanLayout);
      
      tabBeanLayout.setSize(m_FlowWidth, m_FlowHeight);
      Dimension d = tabBeanLayout.getPreferredSize();
      tabBeanLayout.setMinimumSize(d);
      //tabBeanLayout.setMaximumSize(d);
      tabBeanLayout.setPreferredSize(d);
      
      LogPanel tabLogPanel = new LogPanel();
      setUpLogPanel(tabLogPanel);
      Dimension d2 = new Dimension(100, 170);
      tabLogPanel.setPreferredSize(d2);
      tabLogPanel.setMinimumSize(d2);
      m_logPanels.add(tabLogPanel);
      
      m_environmentSettings.add(new Environment());
      m_filePaths.add(new File("-NONE-"));
      
      JSplitPane p2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, p1, tabLogPanel);
      p2.setOneTouchExpandable(true);
      //p2.setDividerLocation(500);
      p2.setDividerLocation(0.7);
      p2.setResizeWeight(1.0);
      JPanel splitHolder = new JPanel();
      splitHolder.setLayout(new BorderLayout());
      splitHolder.add(p2, BorderLayout.CENTER);
      
      //add(splitHolder, BorderLayout.CENTER);
      
      m_editedList.add(new Boolean(false));
      m_executingList.add(new Boolean(false));
      m_selectedBeans.add(new Vector());
      m_undoBufferList.add(new Stack<File>());
      
      m_flowTabs.addTab(tabTitle, splitHolder);
      int tabIndex = getNumTabs() - 1;
      m_flowTabs.setTabComponentAt(tabIndex, new CloseableTabTitle(m_flowTabs));
      setActiveTab(getNumTabs() - 1);      
    }
  }
  
  private class CloseableTabTitle extends JPanel {
    private final JTabbedPane m_enclosingPane;
    
    private JLabel m_tabLabel;
    private TabButton m_tabButton;
    
    public CloseableTabTitle(final JTabbedPane pane) {
      super(new FlowLayout(FlowLayout.LEFT, 0, 0));
      
      m_enclosingPane = pane;
      setOpaque(false);
      setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
      
      // read the title from the JTabbedPane
      m_tabLabel = new JLabel() {
        public String getText() {
          int index = m_enclosingPane.indexOfTabComponent(CloseableTabTitle.this);
          if (index >= 0) {
            return m_enclosingPane.getTitleAt(index);
          }
          return null;
        }
      };
      
      add(m_tabLabel);
      m_tabLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
      m_tabButton = new TabButton();
      add(m_tabButton);

    }
    
    public void setBold(boolean bold) {
      m_tabLabel.setEnabled(bold);
    }
    
    public void setButtonEnabled(boolean enabled) {
      m_tabButton.setEnabled(enabled);
    }
    
    private class TabButton extends JButton implements ActionListener {
      public TabButton() {
        int size = 17;
        setPreferredSize(new Dimension(size, size));
        setToolTipText("close this tab");
        //Make the button looks the same for all Laf's
        setUI(new BasicButtonUI());
        //Make it transparent
        setContentAreaFilled(false);
        //No need to be focusable
        setFocusable(false);
        setBorder(BorderFactory.createEtchedBorder());
        setBorderPainted(false);
        //Making nice rollover effect
        //we use the same listener for all buttons
        addMouseListener(new MouseAdapter() {
          public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();

            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
        });
        setRolloverEnabled(true);
        //Close the proper tab by clicking the button
        addActionListener(this);
      }

      public void actionPerformed(ActionEvent e) {
        int i = m_enclosingPane.indexOfTabComponent(CloseableTabTitle.this);
        if (i >= 0) {
          //m_enclosingPane.remove(i);
          m_mainKFPerspective.removeTab(i);
        }
      }

      //we don't want to update UI for this button
      public void updateUI() {
      }

      //paint the cross
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        //shift the image for pressed buttons
        if (getModel().isPressed()) {
          g2.translate(1, 1);
        }
        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.BLACK);
        if (!isEnabled()) {
          g2.setColor(Color.GRAY);
        }
        if (getModel().isRollover()) {
          g2.setColor(Color.MAGENTA);
        }
        int delta = 6;
        g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
        g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
        g2.dispose();
      }
    }
  }
      
  // Used for measuring and splitting icon labels
  // over multiple lines
  FontMetrics m_fontM;

  // constants for operations in progress
  protected static final int NONE = 0;
  protected static final int MOVING = 1;
  protected static final int CONNECTING = 2;
  protected static final int ADDING = 3;
  protected static final int SELECTING = 4;
  protected static final int PASTING = 5;

  // which operation is in progress
  private int m_mode = NONE;

  /** the extension for the user components, when serialized to XML */
  protected final static String USERCOMPONENTS_XML_EXTENSION = ".xml";
  
  /**
   * Button group to manage all toolbar buttons
   */
  private ButtonGroup m_toolBarGroup = new ButtonGroup();

  /**
   * Holds the selected toolbar bean
   */
  private Object m_toolBarBean;
  
  /** Snap-to-grid spacing */
  private int m_gridSpacing = 40;

  /**
   * The layout area
   */
  private BeanLayout m_beanLayout = null;//new BeanLayout();
  
  /** Whether to allow more than one tab or not */
  private boolean m_allowMultipleTabs = true;
  
  private Vector m_userComponents = new Vector();
  private boolean m_firstUserComponentOpp = true;

  private JButton m_pointerB;
  private JButton m_saveB;
  private JButton m_saveBB;
  private JButton m_loadB;
  private JButton m_stopB;
  private JButton m_playB;
  private JButton m_playBB;
  private JButton m_helpB;
  private JButton m_newB;
  
  private JButton m_cutB;
  private JButton m_copyB;
  private JButton m_pasteB;
  private JButton m_deleteB;
  private JButton m_noteB;
  private JButton m_selectAllB;
  private JButton m_undoB;
  
  private JToggleButton m_snapToGridB;
  // private JButton m_deleteB;

  /**
   * Reference to bean being manipulated
   */
  private BeanInstance m_editElement;

  /**
   * Event set descriptor for the bean being manipulated
   */
  private EventSetDescriptor m_sourceEventSetDescriptor;

  /**
   * Used to record screen coordinates during move, select and connect
   * operations
   */
  private int m_oldX, m_oldY;
  private int m_startX, m_startY;
  
  /** The file chooser for selecting layout files */
  protected JFileChooser m_FileChooser 
    = new JFileChooser(new File(System.getProperty("user.dir")));

  protected LogPanel m_logPanel = null; //new LogPanel();//new LogPanel(null, true);
  
  /** Component that holds the currently visible perspective */
  protected JPanel m_perspectiveHolder;
  
  /** 
   * Holds the list of currently loaded perspectives. Element at 0 is always
   * the main KF data mining flow perspective
   */
  protected List<KFPerspective> m_perspectives = new ArrayList<KFPerspective>();
  
  /** Shortcut to the main perspective */
  protected MainKFPerspective m_mainKFPerspective;

  protected BeanContextSupport m_bcSupport = new BeanContextSupport();

  /** the extension for the serialized setups (Java serialization) */
  public final static String FILE_EXTENSION = ".kf";

  /** the extension for the serialized setups (Java serialization) */
  public final static String FILE_EXTENSION_XML = ".kfml";
  
  /** A filter to ensure only KnowledgeFlow files in binary format get shown in
      the chooser */
  protected FileFilter m_KfFilter = 
    new ExtensionFileFilter(FILE_EXTENSION, 
                            "Binary KnowledgeFlow configuration files (*" 
                            + FILE_EXTENSION + ")");

  /** A filter to ensure only KnowledgeFlow files in KOML format 
      get shown in the chooser */
  protected FileFilter m_KOMLFilter = 
    new ExtensionFileFilter(KOML.FILE_EXTENSION + "kf", 
                            "XML KnowledgeFlow configuration files (*" 
                            + KOML.FILE_EXTENSION + "kf)");

  /** A filter to ensure only KnowledgeFlow files in XStream format 
      get shown in the chooser */
  protected FileFilter m_XStreamFilter = 
    new ExtensionFileFilter(XStream.FILE_EXTENSION + "kf", 
                            "XML KnowledgeFlow configuration files (*" 
                            + XStream.FILE_EXTENSION + "kf)");

  /** A filter to ensure only KnowledgeFlow layout files in XML format get 
      shown in the chooser */
  protected FileFilter m_XMLFilter = 
    new ExtensionFileFilter(FILE_EXTENSION_XML, 
                            "XML KnowledgeFlow layout files (*" 
                            + FILE_EXTENSION_XML + ")");

  /** the scrollbar increment of the layout scrollpane */
  protected int m_ScrollBarIncrementLayout = 20;

  /** the scrollbar increment of the components scrollpane */
  protected int m_ScrollBarIncrementComponents = 50;

  /** the flow layout width */
  protected int m_FlowWidth = 2560;

  /** the flow layout height */
  protected int m_FlowHeight = 1440;

  /** the preferred file extension */
  protected String m_PreferredExtension = FILE_EXTENSION;
  
  /** whether to store the user components in XML or in binary format */
  protected boolean m_UserComponentsInXML = false;
  
  /** Environment variables for the current flow */
  protected Environment m_flowEnvironment = new Environment();
  
  /** Palette of components stored in a JTree */
  protected JTree m_componentTree;
  
  /** The node of the JTree that holds "user" (metabean) components */
  protected DefaultMutableTreeNode m_userCompNode;
  
  /** The clip board */
  protected StringBuffer m_pasteBuffer;
  
  /**
   * Set the environment variables to use. NOTE: loading a new layout
   * resets back to the default set of variables
   * 
   * @param env
   */
  public void setEnvironment(Environment env) {
    m_flowEnvironment = env;
    setEnvironment();
  }
  
  private void setEnvironment() {
    // pass m_flowEnvironment to all components
    // that implement EnvironmentHandler
    Vector beans = BeanInstance.getBeanInstances(m_mainKFPerspective.getCurrentTabIndex());
    for (int i = 0; i < beans.size(); i++) {
      Object temp = ((BeanInstance) beans.elementAt(i)).getBean();

      if (temp instanceof EnvironmentHandler) {
        ((EnvironmentHandler) temp).setEnvironment(m_flowEnvironment);
      }
    }
  }
  
  /**
   * Creates a new <code>KnowledgeFlowApp</code> instance.
   */
  // modifications by Zerbetto
  //public KnowledgeFlowApp() {
  public KnowledgeFlowApp(boolean showFileMenu) {
    if (BEAN_PROPERTIES == null) {
      loadProperties();
      init();
    }

    m_showFileMenu = showFileMenu;

    // end modifications by Zerbetto
    // Grab a fontmetrics object
    JWindow temp = new JWindow();
    temp.setVisible(true);
    temp.getGraphics().setFont(new Font(null, Font.PLAIN, 9));
    m_fontM = temp.getGraphics().getFontMetrics();
    temp.setVisible(false);

    // some GUI defaults
    try {
      m_ScrollBarIncrementLayout = Integer.parseInt(
          BEAN_PROPERTIES.getProperty(
            "ScrollBarIncrementLayout", "" + m_ScrollBarIncrementLayout));
      m_ScrollBarIncrementComponents = Integer.parseInt(
          BEAN_PROPERTIES.getProperty(
            "ScrollBarIncrementComponents", "" + m_ScrollBarIncrementComponents));
      m_FlowWidth = Integer.parseInt(
          BEAN_PROPERTIES.getProperty(
            "FlowWidth", "" + m_FlowWidth));
      m_FlowHeight = Integer.parseInt(
          BEAN_PROPERTIES.getProperty(
            "FlowHeight", "" + m_FlowHeight));
      m_PreferredExtension = BEAN_PROPERTIES.getProperty(
          "PreferredExtension", m_PreferredExtension);
      m_UserComponentsInXML = Boolean.valueOf(
          BEAN_PROPERTIES.getProperty(
            "UserComponentsInXML", "" + m_UserComponentsInXML)).booleanValue();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    // FileChooser
    m_FileChooser.addChoosableFileFilter(m_KfFilter);
    if (KOML.isPresent()) {
      m_FileChooser.addChoosableFileFilter(m_KOMLFilter);
    }
    if (XStream.isPresent()) {
      m_FileChooser.addChoosableFileFilter(m_XStreamFilter);
    }

    m_FileChooser.addChoosableFileFilter(m_XMLFilter);

    if (m_PreferredExtension.equals(FILE_EXTENSION_XML)) {
      m_FileChooser.setFileFilter(m_XMLFilter);
    } else if (KOML.isPresent() && m_PreferredExtension.equals(KOML.FILE_EXTENSION + "kf")) {
      m_FileChooser.setFileFilter(m_KOMLFilter);
    } else if (XStream.isPresent() && m_PreferredExtension.equals(XStream.FILE_EXTENSION + "kf")) {
      m_FileChooser.setFileFilter(m_XStreamFilter);
    } else {
      m_FileChooser.setFileFilter(m_KfFilter);
    }
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    m_bcSupport.setDesignTime(true);
        
     
    // TODO --- 
     //configureBeanLayout(m_beanLayout);
    
     
     JPanel mainPanel = new JPanel();
     m_perspectiveHolder = new JPanel();
     m_perspectiveHolder.setLayout(new BorderLayout());
     
     mainPanel.setLayout(new BorderLayout());
     // TODO North will hold main perspective toggle buttons
     // WEST will hold JTree - perhaps not
     // CENTER will hold perspective
     
     
     
     /*JPanel p2 = new JPanel();
     p2.setLayout(new BorderLayout()); */


     m_mainKFPerspective = new MainKFPerspective();
     
     m_perspectiveHolder.add(m_mainKFPerspective, BorderLayout.CENTER);
     m_perspectives.add(m_mainKFPerspective);
     
     mainPanel.add(m_perspectiveHolder, BorderLayout.CENTER);
     setLayout(new BorderLayout());
     
     add(mainPanel, BorderLayout.CENTER);
     
     // setUpToolBars(p2);
     setUpToolsAndJTree(m_mainKFPerspective);
     
     // ---
     JScrollPane treeView = new JScrollPane(m_componentTree);
     JPanel treeHolder = new JPanel();
     treeHolder.setLayout(new BorderLayout());
     treeHolder.setBorder(BorderFactory.createTitledBorder("Design"));
     treeHolder.add(treeView, BorderLayout.CENTER);
     
     m_perspectiveHolder.add(treeHolder, BorderLayout.WEST);
     Dimension d = treeView.getPreferredSize();
     d = new Dimension((int)(d.getWidth() * 1.5), (int)d.getHeight());
     treeView.setPreferredSize(d);
     
     loadUserComponents();
     clearLayout(); // add an initial "Untitled" tab
  }
  
  private void snapSelectedToGrid() {
    Vector v = m_mainKFPerspective.getSelectedBeans();
    if (v.size() > 0) {
      for (int i = 0; i < v.size(); i++) {
        BeanInstance b = (BeanInstance)v.get(i);
        //if (!(b.getBean() instanceof Note)) {
          int x = b.getX();
          int y = b.getY();
          b.setXY(snapToGrid(x), snapToGrid(y));
        //}
      }
      revalidate();
      m_beanLayout.repaint();
      notifyIsDirty();
      m_mainKFPerspective.setEditedStatus(true);
    }
  }
  
  private int snapToGrid(int val) {
    int r = val % m_gridSpacing; val /= m_gridSpacing;
    if (r > (m_gridSpacing/ 2)) val++;
    val *= m_gridSpacing;
    
    return val;
  }
  
  private void configureBeanLayout(final BeanLayout layout) {
    
    layout.setLayout(null);
    
    // handle mouse events
    layout.addMouseListener(new MouseAdapter() {

        public void mousePressed(MouseEvent me) {
          if (m_toolBarBean == null) {
            if (((me.getModifiers() & InputEvent.BUTTON1_MASK)
                 == InputEvent.BUTTON1_MASK) && m_mode == NONE) {
              BeanInstance bi = BeanInstance.findInstance(me.getPoint(), 
                  m_mainKFPerspective.getCurrentTabIndex());
              JComponent bc = null;
              if (bi != null) {
                bc = (JComponent)(bi.getBean());
              }
              if (bc != null /*&& (bc instanceof Visible) */) {
                m_editElement = bi;
                m_oldX = me.getX();
                m_oldY = me.getY();
                m_mode = MOVING;
              }
              if (m_mode != MOVING) {
                m_mode = SELECTING;
                m_oldX = me.getX();
                m_oldY = me.getY();
                m_startX = m_oldX;
                m_startY = m_oldY;
                Graphics2D gx = (Graphics2D)layout.getGraphics();
                gx.setXORMode(java.awt.Color.white);
                //                gx.drawRect(m_oldX, m_oldY, m_oldX, m_oldY);
                //                gx.drawLine(m_startX, m_startY, m_startX, m_startY);
                gx.dispose();
                m_mode = SELECTING;
              }
            }
          }
        }

        public void mouseReleased(MouseEvent me) {
          if (m_editElement != null && m_mode == MOVING) {
            if (m_snapToGridB.isSelected()) {
              int x = snapToGrid(m_editElement.getX());
              int y = snapToGrid(m_editElement.getY());
              m_editElement.setXY(x, y);
              snapSelectedToGrid();
            }
            
            m_editElement = null;
            revalidate();
            layout.repaint();
            m_mode = NONE;
          }
          if (m_mode == SELECTING) {
            revalidate();
            layout.repaint();
            m_mode = NONE;
                        
            //checkSubFlow(m_startX, m_startY, me.getX(), me.getY());
            highlightSubFlow(m_startX, m_startY, me.getX(), me.getY());
          }
        }

        public void mouseClicked(MouseEvent me) {
          BeanInstance bi = BeanInstance.findInstance(me.getPoint(), 
              m_mainKFPerspective.getCurrentTabIndex());
          if (m_mode == ADDING || m_mode == NONE) {
            // try and popup a context sensitive menu if we have
            // been clicked over a bean.
            if (bi != null) {
              JComponent bc = (JComponent)bi.getBean();
              // if we've been double clicked, then popup customizer
              // as long as we're not a meta bean
              if (me.getClickCount() == 2 && !(bc instanceof MetaBean)) {
                try {
                  Class custClass = 
                    Introspector.getBeanInfo(bc.getClass()).getBeanDescriptor().getCustomizerClass();
                  if (custClass != null) {
                    if (bc instanceof BeanCommon) {
                      if (!((BeanCommon)bc).
                          isBusy()) {
                        popupCustomizer(custClass, bc);
                      }
                    } else {
                      popupCustomizer(custClass, bc);
                    }
                  }
                } catch (IntrospectionException ex) {
                  ex.printStackTrace();
                }
              } else if (((me.getModifiers() & InputEvent.BUTTON1_MASK)
                          != InputEvent.BUTTON1_MASK) || me.isAltDown()) {
                if (!m_mainKFPerspective.getExecuting()) {
                  doPopup(me.getPoint(), bi, me.getX(), me.getY());
                }
                return;
              } else {
                // just select this bean
                Vector v = m_mainKFPerspective.getSelectedBeans();
                if (me.isShiftDown()) {
                } else {
                  v = new Vector();
                }
                v.add(bi);             
                m_mainKFPerspective.setSelectedBeans(v);
                
                return;
              }
            } else {
              if (((me.getModifiers() & InputEvent.BUTTON1_MASK)
                   != InputEvent.BUTTON1_MASK) || me.isAltDown()) {
                
                // find connections if any close to this point
                if (!m_mainKFPerspective.getExecuting()) {
                  rightClickCanvasPopup(me.getX(), me.getY());
                }
                return;
                /*int delta = 10;
                deleteConnectionPopup(BeanConnection.
                      getClosestConnections(new Point(me.getX(), me.getY()), 
                                            delta, m_mainKFPerspective.getCurrentTabIndex()), 
                                            me.getX(), me.getY()); */
              } else if (m_toolBarBean != null) {
                // otherwise, if a toolbar button is active then 
                // add the component
                
                // snap to grid
                int x = me.getX();
                int y = me.getY();
                if (m_snapToGridB.isSelected()) {
                  x = snapToGrid(me.getX()); 
                  y = snapToGrid(me.getY());
                }
                
                addUndoPoint();
                addComponent(x, y);
                m_componentTree.clearSelection();
                m_mainKFPerspective.setEditedStatus(true);
              }
            }
          }
          
          if (m_mode == PASTING && m_pasteBuffer.length() > 0) {
            pasteFromClipboard(me.getX(), me.getY());
            m_mode = NONE;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
          }
        
          if (m_mode == CONNECTING) {
            // turn off connecting points and remove connecting line
            layout.repaint();
            Vector beanInstances = 
              BeanInstance.getBeanInstances(m_mainKFPerspective.getCurrentTabIndex());
            for (int i = 0; i < beanInstances.size(); i++) {
              JComponent bean = 
                (JComponent)((BeanInstance)beanInstances.elementAt(i)).
                getBean();
              if (bean instanceof Visible) {
                ((Visible)bean).getVisual().setDisplayConnectors(false);
              }
            }

            if (bi != null) {
              boolean doConnection = false;
              if (!(bi.getBean() instanceof BeanCommon)) {
                doConnection = true;
              } else {
                // Give the target bean a chance to veto the proposed
                // connection
                if (((BeanCommon)bi.getBean()).
                    //connectionAllowed(m_sourceEventSetDescriptor.getName())) {
                    connectionAllowed(m_sourceEventSetDescriptor)) {
                  doConnection = true;
                }
              }
              if (doConnection) {
                
                addUndoPoint();
                // attempt to connect source and target beans                                
                if (bi.getBean() instanceof MetaBean) {
                  BeanConnection.doMetaConnection(m_editElement, bi,
                                                  m_sourceEventSetDescriptor,
                                                  layout);
                } else {
                  BeanConnection bc = 
                    new BeanConnection(m_editElement, bi, 
                                       m_sourceEventSetDescriptor,
                                       m_mainKFPerspective.getCurrentTabIndex());
                }
                m_mainKFPerspective.setEditedStatus(true);
              }
              layout.repaint();
            }
            m_mode = NONE;
            m_editElement = null;
            m_sourceEventSetDescriptor = null;
          }
          
          if (m_mainKFPerspective.getSelectedBeans().size() > 0) {
            m_mainKFPerspective.setSelectedBeans(new Vector());
          }
        }
      });
    
    layout.addMouseMotionListener(new MouseMotionAdapter() {

        public void mouseDragged(MouseEvent me) {
          if (m_editElement != null && m_mode == MOVING) {

            /*int width = ic.getIconWidth() / 2;
            int height = ic.getIconHeight() / 2; */ 
            /*int width = m_oldX - m_editElement.getX();
            int height = m_oldY - m_editElement.getY(); */
            
            int deltaX = me.getX() - m_oldX;
            int deltaY = me.getY() - m_oldY;

            /*      m_editElement.setX(m_oldX-width);
                    m_editElement.setY(m_oldY-height); */
            //int newX = snapToGrid(m_oldX-width);
            //int newX = m_oldX-width;
            //int newY = snapToGrid(m_oldY-height);
            //int newY = m_oldY-height;            

            m_editElement.setXY(m_editElement.getX() + deltaX, 
                m_editElement.getY() + deltaY);
            
            if (m_mainKFPerspective.getSelectedBeans().size() > 0) {
              Vector v = m_mainKFPerspective.getSelectedBeans();
              for (int i = 0; i < v.size(); i++) {
                BeanInstance b = (BeanInstance)v.get(i);
                if (b != m_editElement) {
                  b.setXY(b.getX() + deltaX, b.getY() + deltaY);
                }                                
              }
            }
            layout.repaint();
            
            // note the new points
            m_oldX = me.getX(); m_oldY = me.getY();
            m_mainKFPerspective.setEditedStatus(true);
          }
          if (m_mode == SELECTING) {
            layout.repaint();
            m_oldX = me.getX(); m_oldY = me.getY();
          }
        }

         public void mouseMoved(MouseEvent e) {
           if (m_mode == CONNECTING) {
             layout.repaint();
             // note the new coordinates
             m_oldX = e.getX(); m_oldY = e.getY();
           }
         }
       });
  }
  
  private void setUpLogPanel(final LogPanel logPanel) {
    String date = (new SimpleDateFormat("EEEE, d MMMM yyyy"))
    .format(new Date());
    logPanel.logMessage("Weka Knowledge Flow was written by Mark Hall");
    logPanel.logMessage("Weka Knowledge Flow");
    logPanel.logMessage("(c) 2002-" + Copyright.getToYear() + " " 
        + Copyright.getOwner() + ", " + Copyright.getAddress());
    logPanel.logMessage("web: " + Copyright.getURL());
    logPanel.logMessage(date);
    logPanel.statusMessage("[KnowledgeFlow]|Welcome to the Weka Knowledge Flow");
    logPanel.getStatusTable().addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (logPanel.getStatusTable().rowAtPoint(e.getPoint()) == 0) {
          if (((e.getModifiers() & InputEvent.BUTTON1_MASK)
              != InputEvent.BUTTON1_MASK) || e.isAltDown()) {
            System.gc();
            Runtime currR = Runtime.getRuntime();
            long freeM = currR.freeMemory();
            long totalM = currR.totalMemory();
            long maxM = currR.maxMemory();
            logPanel.
            logMessage("[KnowledgeFlow] Memory (free/total/max.) in bytes: " 
                + String.format("%,d", freeM) + " / " 
                + String.format("%,d", totalM) + " / " 
                + String.format("%,d", maxM));
            logPanel.statusMessage("[KnowledgeFlow]|Memory (free/total/max.) in bytes: " 
                + String.format("%,d", freeM) + " / " 
                + String.format("%,d", totalM) + " / " 
                + String.format("%,d", maxM)); 
          }
        }
      }
    });
  }
  

  
  private Image loadImage(String path) {
    Image pic = null;
    // Modified by Zerbetto
    //java.net.URL imageURL = ClassLoader.getSystemResource(path);
    java.net.URL imageURL = this.getClass().getClassLoader().getResource(path);

    // end modifications
    if (imageURL == null) {
      //      System.err.println("Warning: unable to load "+path);
    } else {
      pic = Toolkit.getDefaultToolkit().
	getImage(imageURL);
    }
    return pic;
  }
  
  private void setUpToolsAndJTree(JPanel targetP) {
    JPanel toolBarPanel = new JPanel();
    toolBarPanel.setLayout(new BorderLayout());

    // modifications by Zerbetto
    // first construct the toolbar for saving, loading etc
    if (m_showFileMenu) {
      JToolBar fixedTools = new JToolBar();
      fixedTools.setOrientation(JToolBar.HORIZONTAL);
      
      m_cutB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH + 
          "cut.png")));
      m_cutB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_cutB.setToolTipText("Cut selected");
      m_copyB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH + 
        "page_copy.png")));
      m_copyB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_copyB.setToolTipText("Copy selected");
      m_pasteB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH + 
        "paste_plain.png")));
      m_pasteB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_pasteB.setToolTipText("Paste from clipboard");
      m_deleteB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH + 
        "delete.png")));
      m_deleteB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_deleteB.setToolTipText("Delete selected");
      m_snapToGridB = new JToggleButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH + 
          "shape_handles.png")));
      //m_snapToGridB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_snapToGridB.setToolTipText("Snap to grid");
      /*Dimension d = m_snapToGridB.getPreferredSize();
      d = new Dimension((int)d.getWidth() * 8, (int)d.getHeight()*8);
      m_snapToGridB.setPreferredSize(d);*/

      m_saveB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH +
              "disk.png")));      
      m_saveB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_saveB.setToolTipText("Save layout");
      m_saveBB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH +
        "disk_multiple.png")));
      m_saveBB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_saveBB.setToolTipText("Save layout with new name");
      
      m_loadB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH +
              "folder_add.png")));
      m_loadB.setToolTipText("Load layout");
      m_loadB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_newB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH +
              "page_add.png")));
      m_newB.setToolTipText("New layout");
      m_newB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_newB.setEnabled(getAllowMultipleTabs());
      
      m_helpB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH +
        "help.png")));
      m_helpB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_helpB.setToolTipText("Display help");
      
      m_noteB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH +
        "note_add.png")));
      m_noteB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_noteB.setToolTipText("Add a note to the layout");
      
      m_selectAllB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH +
        "shape_group.png")));
      m_selectAllB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_selectAllB.setToolTipText("Select all");
      
      m_undoB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH +
        "arrow_undo.png")));
      m_undoB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
      m_undoB.setToolTipText("Undo");
      

      fixedTools.add(m_selectAllB);
      fixedTools.add(m_cutB);
      fixedTools.add(m_copyB);
      fixedTools.add(m_deleteB);
      fixedTools.add(m_pasteB);
      fixedTools.add(m_undoB);
      fixedTools.add(m_noteB);
      fixedTools.addSeparator();
      fixedTools.add(m_snapToGridB);      
      fixedTools.addSeparator();
      fixedTools.add(m_newB);
      fixedTools.add(m_saveB);
      fixedTools.add(m_saveBB);
      fixedTools.add(m_loadB);
      
      fixedTools.add(m_helpB);

      m_saveB.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            saveLayout(m_mainKFPerspective.getCurrentTabIndex(), false);
          }
        });
      
      m_saveBB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          saveLayout(m_mainKFPerspective.getCurrentTabIndex(), true);
        }
      });

      m_loadB.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            m_flowEnvironment = new Environment();
            loadLayout();
          }
        });

      m_newB.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            clearLayout();
          }
        });
      
      m_selectAllB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (BeanInstance.
              getBeanInstances(m_mainKFPerspective.getCurrentTabIndex()).size() > 0) {
            m_mainKFPerspective.setSelectedBeans(BeanInstance.
                getBeanInstances(m_mainKFPerspective.getCurrentTabIndex()));
          }
        }
      });
      
      m_cutB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // only delete if our copy was successful!
          if (copyToClipboard()) {              
            deleteSelectedBeans();
          }
        }
      });
      
      m_deleteB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          deleteSelectedBeans();
        }
      });
      
      m_copyB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          copyToClipboard();
          m_mainKFPerspective.setSelectedBeans(new Vector());
        }
      });
      
      m_pasteB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          
          setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
          m_mode = PASTING;
          
          /* pasteFromClipboard(10, 10);

          if (m_mainKFPerspective.getSelectedBeans().size() > 0) {
            m_mainKFPerspective.setSelectedBeans(new Vector());
          } */
        }
      });
      
      m_snapToGridB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (m_snapToGridB.isSelected()) {
            snapSelectedToGrid();
          }
        }
      });

      fixedTools.setFloatable(false);
      toolBarPanel.add(fixedTools, BorderLayout.EAST);
    }
    
    m_noteB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Note n = new Note();
        m_toolBarBean = n;
       
        setCursor(Cursor.
            getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        m_mode = ADDING;
      }
    });
    
    m_undoB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Stack<File> undo = m_mainKFPerspective.getUndoBuffer();
        if (undo.size() > 0) {
          File undoF = undo.pop();
          if (undo.size() == 0) {
            m_undoB.setEnabled(false);
          }          
          loadLayout(undoF, false, true);          
        }        
      }
    });
    
    m_playB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH + 
        "resultset_next.png")));
    m_playB.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    m_playB.setToolTipText("Run this flow (all start points launched in parallel)");
    m_playB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        runFlow(false);
      }
    });
    
    m_playBB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH + 
    "resultset_last.png")));
    m_playBB.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    m_playBB.setToolTipText("Run this flow (start points launched sequentially)");
    m_playBB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        runFlow(true);
      }
    });

    m_stopB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH +
            "shape_square.png")));
    m_stopB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    m_stopB.setToolTipText("Stop all execution");

    Image tempI = loadImage(BeanVisual.ICON_PATH + "cursor.png");
    m_pointerB = new JButton(new ImageIcon(tempI));
    m_pointerB.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    m_pointerB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          m_toolBarBean = null;
          m_mode = NONE;
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          m_componentTree.clearSelection();
        }
      });

    //    Dimension dP = m_saveB.getPreferredSize();
    //    Dimension dM = m_saveB.getMaximumSize();
    //    Dimension dP = m_stopB.getPreferredSize();
    //    Dimension dM = m_stopB.getMaximumSize();
    //    m_pointerB.setPreferredSize(dP);
    //    m_pointerB.setMaximumSize(dM);
    m_toolBarGroup.add(m_pointerB);

    JToolBar fixedTools2 = new JToolBar();
    fixedTools2.setOrientation(JToolBar.HORIZONTAL);
    fixedTools2.setFloatable(false);
    fixedTools2.add(m_pointerB);
    fixedTools2.add(m_playB);
    fixedTools2.add(m_playBB);
    fixedTools2.add(m_stopB);
    //    m_helpB.setPreferredSize(dP);
    //    m_helpB.setMaximumSize(dP);
    //m_helpB.setSize(m_pointerB.getSize().width, m_pointerB.getSize().height);
    toolBarPanel.add(fixedTools2, BorderLayout.WEST);
    // end modifications by Zerbetto
    m_stopB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          m_logPanel.statusMessage("[KnowledgeFlow]|Attempting to stop all components...");
          stopFlow();
          m_logPanel.statusMessage("[KnowledgeFlow]|OK.");
        }
      });

    m_helpB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          popupHelp();
        }
      });

    
    
    final int standard_toolset = 0;
    final int wrapper_toolset = 1;

    int toolBarType = standard_toolset;

    DefaultMutableTreeNode jtreeRoot = new DefaultMutableTreeNode("Weka");
    // set up wrapper toolsets
    for (int i = 0; i < TOOLBARS.size(); i++) {
      Vector tempBarSpecs = (Vector) TOOLBARS.elementAt(i);

      // name for the tool bar
      String tempToolSetName = (String) tempBarSpecs.elementAt(0);
      DefaultMutableTreeNode subTreeNode = new DefaultMutableTreeNode(tempToolSetName);
      jtreeRoot.add(subTreeNode);

      // Used for weka leaf packages 
//      Box singletonHolderPanel = null;

      // name of the bean component to handle this class of weka algorithms
      String tempBeanCompName = (String) tempBarSpecs.elementAt(1);

      // a JPanel holding an instantiated bean + label ready to be added
      // to the current toolbar
      JPanel tempBean;

      // the root package for weka algorithms
      String rootPackage = "";
      weka.gui.HierarchyPropertyParser hpp = null;
      Hashtable hpps = null;

      // Is this a wrapper toolbar?
      if (tempBeanCompName.compareTo("null") != 0) {
        tempBean = null;
        toolBarType = wrapper_toolset;
        rootPackage = (String) tempBarSpecs.elementAt(2);
        //      hpp = (weka.gui.HierarchyPropertyParser)tempBarSpecs.elementAt(3);
        hpps = (Hashtable) tempBarSpecs.elementAt(3);

        try {
          // modifications by Zerbetto
          // Beans.instantiate(null, tempBeanCompName);
          Beans.instantiate(this.getClass().getClassLoader(), tempBeanCompName);

          // end modifications by Zerbetto
        } catch (Exception ex) {
          // ignore
          System.err.println("[KnowledgeFlow] Failed to instantiate: " + tempBeanCompName);

          break;
        }
      } else {
        toolBarType = standard_toolset;
      }
      
      // a toolbar to hold buttons---one for each algorithm
//      JToolBar tempToolBar = new JToolBar();

      //      System.err.println(tempToolBar.getLayout());
      //      tempToolBar.setLayout(new FlowLayout());
      int z = 2;

      if (toolBarType == wrapper_toolset) {
        Enumeration enm = hpps.keys();

        while (enm.hasMoreElements()) {
          String root = (String) enm.nextElement();
          String userPrefix = "";
          hpp = (HierarchyPropertyParser) hpps.get(root);

          if (!hpp.goTo(rootPackage)) {
            System.out.println("[KnowledgeFlow] Processing user package... ");
            //            System.exit(1);
            userPrefix = root + ".";
          }

          String[] primaryPackages = hpp.childrenValues();

          for (int kk = 0; kk < primaryPackages.length; kk++) {
            
            hpp.goToChild(primaryPackages[kk]);

            // check to see if this is a leaf - if so then there are no
            // sub packages
            if (hpp.isLeafReached()) {
/*              if (singletonHolderPanel == null) {
                singletonHolderPanel = Box.createHorizontalBox();
                singletonHolderPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(
                    tempToolSetName));
              } */

              String algName = hpp.fullValue();
              // -- tempBean = instantiateToolBarBean(true, tempBeanCompName, algName);
              Object visibleCheck = instantiateBean((toolBarType == wrapper_toolset), 
                  tempBeanCompName, algName);

              //if (tempBean != null) {
              if (visibleCheck != null) {
                // tempToolBar.add(tempBean);
//                singletonHolderPanel.add(tempBean);
                
                
                /*Object visibleCheck = instantiateBean((toolBarType == wrapper_toolset), 
                    tempBeanCompName, algName); */
                if (visibleCheck instanceof BeanContextChild) {
                  m_bcSupport.add(visibleCheck);
                }
                ImageIcon scaledForTree = null;
                if (visibleCheck instanceof Visible) {
                  BeanVisual bv = ((Visible)visibleCheck).getVisual();
                  if (bv != null) {
                    scaledForTree = new ImageIcon(bv.scale(0.33));
                    // m_iconLookup.put(algName, scaledForTree);
                  }
                }
                JTreeLeafDetails leafData = new JTreeLeafDetails(tempBeanCompName, algName, 
                    scaledForTree);
                DefaultMutableTreeNode leafAlgo = 
                  new DefaultMutableTreeNode(leafData);
                subTreeNode.add(leafAlgo);             
              }

              hpp.goToParent();
            } else {
              // make a titledborder JPanel to hold all the schemes in this
              // package
              //            JPanel holderPanel = new JPanel();
              /* Box holderPanel = Box.createHorizontalBox();
              holderPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(userPrefix +
                  primaryPackages[kk])); */
              
              DefaultMutableTreeNode firstLevelOfMainAlgoType = 
                new DefaultMutableTreeNode(primaryPackages[kk]);
              subTreeNode.add(firstLevelOfMainAlgoType);
              
              //processPackage(holderPanel, tempBeanCompName, hpp, firstLevelOfMainAlgoType);
              processPackage(tempBeanCompName, hpp, firstLevelOfMainAlgoType);
//              tempToolBar.add(holderPanel);
            }
          }

/*          if (singletonHolderPanel != null) {
            tempToolBar.add(singletonHolderPanel);
            singletonHolderPanel = null;
          } */
        }
      } else {
/*        Box holderPanel = Box.createHorizontalBox();
        holderPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(
            tempToolSetName)); */

        for (int j = z; j < tempBarSpecs.size(); j++) {
          tempBean = null;
          tempBeanCompName = (String) tempBarSpecs.elementAt(j);
          Object visibleCheck = instantiateBean((toolBarType == wrapper_toolset), 
              tempBeanCompName, "");
          
/* --         tempBean = instantiateToolBarBean((toolBarType == wrapper_toolset),
              tempBeanCompName, ""); */

          //if (tempBean != null) {
          if (visibleCheck != null) {
            // set tool tip text (if any)
            // setToolTipText(tempBean)
//            holderPanel.add(tempBean);

            String treeName = tempBeanCompName;
            if (treeName.lastIndexOf('.') > 0) {
              treeName = treeName.substring(treeName.lastIndexOf('.') + 1, 
                  treeName.length());
            }
            
            /*Object visibleCheck = instantiateBean((toolBarType == wrapper_toolset), 
                tempBeanCompName, ""); */
            if (visibleCheck instanceof BeanContextChild) {
              m_bcSupport.add(visibleCheck);
            }
            ImageIcon scaledForTree = null;
            
            if (visibleCheck instanceof Visible) {
              BeanVisual bv = ((Visible)visibleCheck).getVisual();
              if (bv != null) {
                scaledForTree = new ImageIcon(bv.scale(0.33));

                //m_iconLookup.put(treeName, scaledForTree);
              }
            }
            JTreeLeafDetails leafData = new JTreeLeafDetails(tempBeanCompName, "", 
                scaledForTree);
            DefaultMutableTreeNode fixedLeafNode = new DefaultMutableTreeNode(leafData);
            subTreeNode.add(fixedLeafNode);            
          }
        }

  //      tempToolBar.add(holderPanel);
      }

//      JScrollPane tempJScrollPane = createScrollPaneForToolBar(tempToolBar);
      // ok, now create tabbed pane to hold this toolbar
//      m_toolBars.addTab(tempToolSetName, null, tempJScrollPane, tempToolSetName);
    }
    
    /// ----
    
    // Any plugin components to process?
    if (BEAN_PLUGINS_PROPERTIES != null && 
        BEAN_PLUGINS_PROPERTIES.size() > 0) {
      
      DefaultMutableTreeNode userSubTree = new DefaultMutableTreeNode("Plugins");
      jtreeRoot.add(userSubTree);
      
      for (int i = 0; i < BEAN_PLUGINS_PROPERTIES.size(); i++) {
        Properties tempP = BEAN_PLUGINS_PROPERTIES.get(i);
        JPanel tempBean = null;
        String components = 
        tempP.getProperty("weka.gui.beans.KnowledgeFlow.Plugins");
        StringTokenizer st2 = new StringTokenizer(components, ", ");

        while (st2.hasMoreTokens()) {
          String tempBeanCompName = st2.nextToken().trim();
          
          String treeName = tempBeanCompName;
          if (treeName.lastIndexOf('.') > 0) {
            treeName = treeName.substring(treeName.lastIndexOf('.') + 1, 
                treeName.length());
          }
          
//          tempBean = instantiateToolBarBean(false, tempBeanCompName, "");
/*          if (m_pluginsToolBar == null) {
            // need to create the plugins tab and toolbar
            setUpPluginsToolBar();
          }
          m_pluginsBoxPanel.add(tempBean); */
          
          Object visibleCheck = instantiateBean((toolBarType == wrapper_toolset), 
              tempBeanCompName, "");
          if (visibleCheck instanceof BeanContextChild) {
            m_bcSupport.add(visibleCheck);
          }
          ImageIcon scaledForTree = null;
          if (visibleCheck instanceof Visible) {
            BeanVisual bv = ((Visible)visibleCheck).getVisual();
            if (bv != null) {
              scaledForTree = new ImageIcon(bv.scale(0.33));
              //m_iconLookup.put(tempBeanCompName, scaledForTree);
            }
          }
          JTreeLeafDetails leafData = new JTreeLeafDetails(tempBeanCompName, "", 
              scaledForTree);
          DefaultMutableTreeNode pluginLeaf = new DefaultMutableTreeNode(leafData);
          userSubTree.add(pluginLeaf);
        }
      }
    }

//    toolBarPanel.add(m_toolBars, BorderLayout.CENTER);

    //    add(m_toolBars, BorderLayout.NORTH);
    targetP.add(toolBarPanel, BorderLayout.NORTH);
    
    DefaultTreeModel model = new DefaultTreeModel(jtreeRoot);
    
    // subclass JTree so that tool tips can be displayed for leaves (if necessary)
    m_componentTree = new JTree(model) {
      public String getToolTipText(MouseEvent e) {
        if ((getRowForLocation(e.getX(), e.getY())) == -1) {
          return null;
        }
        TreePath currPath = getPathForLocation(e.getX(), e.getY());
        if (currPath.getLastPathComponent() instanceof DefaultMutableTreeNode) {
          DefaultMutableTreeNode node = 
            (DefaultMutableTreeNode)currPath.getLastPathComponent();
          if (node.isLeaf()) {
            JTreeLeafDetails leaf = (JTreeLeafDetails)node.getUserObject();
            return leaf.getToolTipText();
          }
        }
        return null;
      }      
    };
    
    m_componentTree.setEnabled(true);
    m_componentTree.setToolTipText("");
    m_componentTree.setRootVisible(false);
    m_componentTree.setShowsRootHandles(true);
    m_componentTree.setCellRenderer(new BeanIconRenderer());
    DefaultTreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
    selectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    m_componentTree.setSelectionModel(selectionModel);
    
    m_componentTree.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        
        if (((e.getModifiers() & InputEvent.BUTTON1_MASK)
          != InputEvent.BUTTON1_MASK) || e.isAltDown()) {
            boolean clearSelection = true;
            /*TreePath path = 
              m_componentTree.getPathForLocation(e.getX(), e.getY());
            if (path != null) {
              DefaultMutableTreeNode tNode = 
                (DefaultMutableTreeNode)path.getLastPathComponent();
              
              if (tNode.isLeaf()) {
                System.out.println("Rick click selection");
               
//                m_componentTree.setSelectionPath(path);
              }
            }*/
            
            if (clearSelection) {
              // right click cancels selected component
              m_toolBarBean = null;
              m_mode = NONE;
              setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
              m_componentTree.clearSelection();
            }
        }


        TreePath p = m_componentTree.getSelectionPath();
        if (p != null) {
          if (p.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode tNode = (DefaultMutableTreeNode)p.getLastPathComponent();

            if (tNode.isLeaf()) {
              System.err.println("Selected : " + tNode.getUserObject().toString());
              Object userObject = tNode.getUserObject();
              if (userObject instanceof JTreeLeafDetails) {

                if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0 &&
                    ((JTreeLeafDetails)userObject).isMetaBean()) {
                  if (m_firstUserComponentOpp) {
                    installWindowListenerForSavingUserBeans();
                    m_firstUserComponentOpp = false;
                  }

                  MetaBean toRemove = ((JTreeLeafDetails)userObject).getMetaBean();
                  DefaultTreeModel model = (DefaultTreeModel) m_componentTree.getModel();
                  MutableTreeNode userRoot = (MutableTreeNode)tNode.getParent(); // The "User" folder
                  model.removeNodeFromParent(tNode);
                  m_userComponents.remove(toRemove);
                  
                  if (m_userComponents.size() == 0) {
                    model.removeNodeFromParent(userRoot);
                    m_userCompNode = null;
                  }

                } else {
                  ((JTreeLeafDetails)userObject).instantiateBean();
                }
              }                
            }
          }
        }
      }
    });
    
  }
  
  /**
   * Run all start-points in a layout in parallel or sequentially. Order
   * of execution is arbitrary if the user has not prefixed the names of
   * the start points with "<num> :" in order to specify the order. In both
   * parallel and sequential mode, a start point can be ommitted from exectution
   * by prefixing its name with "! :".
   * 
   * @param sequential true if the flow layout is to have its start points run
   * sequentially rather than in parallel
   * 
   */
  private void runFlow(final boolean sequential) {
    if (m_mainKFPerspective.getNumTabs() > 0) {      
      Thread runThread = new Thread() {
        
        int m_flowIndex;
        
        public void run() {
          m_flowIndex = m_mainKFPerspective.getCurrentTabIndex();
          m_mainKFPerspective.setExecuting(true);
          
          FlowRunner runner = new FlowRunner(false, false);
          runner.setStartSequentially(sequential);
          runner.setEnvironment(m_flowEnvironment);
          runner.setLog(m_logPanel);
          Vector comps = BeanInstance.getBeanInstances(m_mainKFPerspective.getCurrentTabIndex());

          runner.setFlows(comps);
          try {
            runner.run();
            runner.waitUntilFinished();
          } catch (Exception ex) {
            m_logPanel.logMessage("An error occurred while running the flow: " +
                ex.getMessage());
          } finally {
            m_mainKFPerspective.setExecuting(m_flowIndex, false);
          }
        }
      };
      
      runThread.start();
    }    
  }
  
  private void stopFlow() {
    if (m_mainKFPerspective.getCurrentTabIndex() >= 0) {
      Vector components = 
        BeanInstance.getBeanInstances(m_mainKFPerspective.getCurrentTabIndex());

      if (components != null) {
        for (int i = 0; i < components.size(); i++) {
          Object temp = ((BeanInstance) components.elementAt(i)).getBean();

          if (temp instanceof BeanCommon) {
            ((BeanCommon) temp).stop();
          }
        }
      }
    }
  }

  private void processPackage(String tempBeanCompName,
			      weka.gui.HierarchyPropertyParser hpp,
			      DefaultMutableTreeNode parentNode) {
    if (hpp.isLeafReached()) {
      // instantiate a bean and add it to the holderPanel
      //      System.err.println("Would add "+hpp.fullValue());
      /*String algName = hpp.fullValue();
      JPanel tempBean = 
	instantiateToolBarBean(true, tempBeanCompName, algName);
      if (tempBean != null && holderPanel != null) {
        holderPanel.add(tempBean);
      }*/
      
      hpp.goToParent();
      return;
    }
    String [] children = hpp.childrenValues();
    for (int i = 0; i < children.length; i++) {
      hpp.goToChild(children[i]);
      DefaultMutableTreeNode child = null;
      
      if (hpp.isLeafReached()) {
        String algName = hpp.fullValue();
        
        Object visibleCheck = instantiateBean(true, 
            tempBeanCompName, algName);
        if (visibleCheck instanceof BeanContextChild) {
          m_bcSupport.add(visibleCheck);
        }
        ImageIcon scaledForTree = null;
        if (visibleCheck instanceof Visible) {
          BeanVisual bv = ((Visible)visibleCheck).getVisual();
          if (bv != null) {
            scaledForTree = new ImageIcon(bv.scale(0.33));
            // m_iconLookup.put(algName, scaledForTree);
          }
        }
        JTreeLeafDetails leafData = new JTreeLeafDetails(tempBeanCompName, algName, 
            scaledForTree);
        child = new DefaultMutableTreeNode(leafData);        
      } else {
        child = new DefaultMutableTreeNode(children[i]);
      }
      parentNode.add(child);
      
      processPackage(tempBeanCompName, hpp, child);
    }
    hpp.goToParent();
  }
  
  private Object instantiateBean(boolean wekawrapper, String tempBeanCompName,
      String algName) {
    Object tempBean;
    if (wekawrapper) {
      try {
        // modifications by Zerbetto
        //tempBean = Beans.instantiate(null, tempBeanCompName);
        tempBean = Beans.instantiate(this.getClass().getClassLoader(),
                                     tempBeanCompName);
        
        // end modifications by Zerbetto
      } catch (Exception ex) {
        System.err.println("[KnowledgeFlow] Failed to instantiate :"+tempBeanCompName
                           +"KnowledgeFlowApp.instantiateBean()");
        return null;
      }
      if (tempBean instanceof WekaWrapper) {
        //      algName = (String)tempBarSpecs.elementAt(j);
        Class c = null;
        try {
          c = Class.forName(algName);
        } catch (Exception ex) {
          System.err.println("[KnowledgeFlow] Can't find class called: "+algName);
          return null;
        }
        try {
          Object o = c.newInstance();
          ((WekaWrapper)tempBean).setWrappedAlgorithm(o);
        } catch (Exception ex) {
          System.err.println("[KnowledgeFlow] Failed to configure "+tempBeanCompName
                             +" with "+algName);
          return null;
        }
      }
    } else {
      try {
        // modifications by Zerbetto
        //tempBean = Beans.instantiate(null, tempBeanCompName);
        tempBean = Beans.instantiate(this.getClass().getClassLoader(),
            tempBeanCompName);

        // end modifications
      } catch (Exception ex) {
        ex.printStackTrace();
        System.err.println("[KnowledgeFlow] Failed to instantiate :"+tempBeanCompName
                           +"KnowledgeFlowApp.instantiateBean()");
        return null;
      }
    }
    return tempBean;
  }

  /**
   * Pop up a help window
   */
  private void popupHelp() {
    final JButton tempB = m_helpB;
    try {
      tempB.setEnabled(false);
      // Modified by Zerbetto
      //InputStream inR = 
      //	ClassLoader.
      //        getSystemResourceAsStream("weka/gui/beans/README_KnowledgeFlow");
      InputStream inR = this.getClass().getClassLoader()
                            .getResourceAsStream("weka/gui/beans/README_KnowledgeFlow");

      // end modifications
      StringBuffer helpHolder = new StringBuffer();
      LineNumberReader lnr = new LineNumberReader(new InputStreamReader(inR));
      
      String line;
      
      while ((line = lnr.readLine()) != null) {
	helpHolder.append(line+"\n");
      }
      
      lnr.close();
      final javax.swing.JFrame jf = new javax.swing.JFrame();
      jf.getContentPane().setLayout(new java.awt.BorderLayout());
      final JTextArea ta = new JTextArea(helpHolder.toString());
      ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
      ta.setEditable(false);
      final JScrollPane sp = new JScrollPane(ta);
      jf.getContentPane().add(sp, java.awt.BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
	  tempB.setEnabled(true);
          jf.dispose();
        }
      });
      jf.setSize(600,600);
      jf.setVisible(true);
      
    } catch (Exception ex) {
      tempB.setEnabled(true);
    }
  }

  public void clearLayout() {
    stopFlow(); // try and stop any running components
    
    if (m_mainKFPerspective.getNumTabs() == 0 || getAllowMultipleTabs()) {
      m_mainKFPerspective.addTab("Untitled");
    }
    
    if (!getAllowMultipleTabs()) {
      BeanConnection.setConnections(new Vector(), 
          m_mainKFPerspective.getCurrentTabIndex());
      BeanInstance.setBeanInstances(new Vector(), 
          m_mainKFPerspective.getBeanLayout(m_mainKFPerspective.getCurrentTabIndex()), 
          m_mainKFPerspective.getCurrentTabIndex());
    }
    
    /*BeanInstance.reset(m_beanLayout);
    BeanConnection.reset();
    m_beanLayout.revalidate();
    m_beanLayout.repaint();
    m_logPanel.clearStatus();
    m_logPanel.statusMessage("[KnowledgeFlow]|Welcome to the Weka Knowledge Flow"); */
  }
  
  /**
   * Popup a context sensitive menu for the bean component
   *
   * @param pt holds the panel coordinates for the component
   * @param bi the bean component over which the user right clicked the mouse
   * @param x the x coordinate at which to popup the menu
   * @param y the y coordinate at which to popup the menu
   *
   * Modified by Zerbetto: javax.swing.JPopupMenu transformed into java.awt.PopupMenu
   *
   */
  private void doPopup(Point pt, final BeanInstance bi, int x, int y) {
    final JComponent bc = (JComponent) bi.getBean();
    final int xx = x;
    final int yy = y;
    int menuItemCount = 0;

    // modifications by Zerbetto
    PopupMenu beanContextMenu = new PopupMenu();

    //JPopupMenu beanContextMenu = new JPopupMenu();

    //    beanContextMenu.insert(new JLabel("Edit", 
    //				      SwingConstants.CENTER), 
    //			   menuItemCount);
    MenuItem edit = new MenuItem("Edit:");
    edit.setEnabled(false);
    beanContextMenu.insert(edit, menuItemCount);
    menuItemCount++;
    
    if (m_mainKFPerspective.getSelectedBeans().size() > 0) {
      MenuItem copyItem = new MenuItem("Copy");
      copyItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          copyToClipboard();
          m_mainKFPerspective.setSelectedBeans(new Vector());
        }
      });
      beanContextMenu.add(copyItem);
      menuItemCount++;
    }
    

    if (bc instanceof MetaBean) {
      //JMenuItem ungroupItem = new JMenuItem("Ungroup");
      MenuItem ungroupItem = new MenuItem("Ungroup");
      ungroupItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // ungroup
            bi.removeBean(m_beanLayout, m_mainKFPerspective.getCurrentTabIndex());

            Vector group = ((MetaBean) bc).getBeansInSubFlow();
            Vector associatedConnections = ((MetaBean) bc).getAssociatedConnections();
            ((MetaBean) bc).restoreBeans();

            for (int i = 0; i < group.size(); i++) {
              BeanInstance tbi = (BeanInstance) group.elementAt(i);
              addComponent(tbi, false);
              tbi.addBean(m_beanLayout, m_mainKFPerspective.getCurrentTabIndex());
            }

            for (int i = 0; i < associatedConnections.size(); i++) {
              BeanConnection tbc = (BeanConnection) associatedConnections.elementAt(i);
              tbc.setHidden(false);
            }

            m_beanLayout.repaint();
            m_mainKFPerspective.setEditedStatus(true);
            notifyIsDirty();            
          }
        });
      beanContextMenu.add(ungroupItem);
      menuItemCount++;

      // Add to user tab
      //JMenuItem addToUserTabItem = new JMenuItem("Add to user tab");
      MenuItem addToUserTabItem = new MenuItem("Add to user tab");
      addToUserTabItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            //addToUserToolBar((MetaBean) bi.getBean(), true);
            addToUserTreeNode((MetaBean) bi.getBean(), true);
            notifyIsDirty();
          }
        });
      beanContextMenu.add(addToUserTabItem);
      menuItemCount++;
    }

    //JMenuItem deleteItem = new JMenuItem("Delete");
    MenuItem deleteItem = new MenuItem("Delete");
    deleteItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          BeanConnection.removeConnections(bi, m_mainKFPerspective.getCurrentTabIndex());
          bi.removeBean(m_beanLayout, m_mainKFPerspective.getCurrentTabIndex());
          if (bc instanceof BeanCommon) {            
            String key = ((BeanCommon)bc).getCustomName()
              + "$" + bc.hashCode();
            m_logPanel.statusMessage(key + "|remove");
          }
          
          // delete any that have been actively selected
          if (m_mainKFPerspective.getSelectedBeans().size() > 0) {
            deleteSelectedBeans();
          }
          
          revalidate();
          m_mainKFPerspective.setEditedStatus(true);
          notifyIsDirty();
          m_selectAllB.setEnabled(BeanInstance.
              getBeanInstances(m_mainKFPerspective.getCurrentTabIndex()).size() > 0);
        }
      });
    if (bc instanceof BeanCommon) {
      if (((BeanCommon)bc).isBusy()) {
        deleteItem.setEnabled(false);
      }
    }
    beanContextMenu.add(deleteItem);
    menuItemCount++;

    if (bc instanceof BeanCommon) {
      MenuItem nameItem = new MenuItem("Set name");
      nameItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String oldName = ((BeanCommon)bc).getCustomName();
            String name = JOptionPane.showInputDialog(KnowledgeFlowApp.this,
                                                      "Enter a name for this component",
                                                      oldName);
            if (name != null) {
              ((BeanCommon)bc).setCustomName(name);
              m_mainKFPerspective.setEditedStatus(true);
            }
          }
        });
      if (bc instanceof BeanCommon) {
        if (((BeanCommon)bc).isBusy()) {
          nameItem.setEnabled(false);
        }
      }
      beanContextMenu.add(nameItem);
      menuItemCount++;
    }

    try {
      //BeanInfo [] compInfo = null;
      //JComponent [] associatedBeans = null;
      Vector compInfo = new Vector(1);
      Vector associatedBeans = null;
      Vector outputBeans = null;
      Vector compInfoOutputs = null;

      if (bc instanceof MetaBean) {
        compInfo = ((MetaBean) bc).getBeanInfoSubFlow();
        associatedBeans = ((MetaBean) bc).getBeansInSubFlow();

        outputBeans = ((MetaBean) bc).getBeansInOutputs();
        compInfoOutputs = ((MetaBean) bc).getBeanInfoOutputs();
      } else {
        compInfo.add(Introspector.getBeanInfo(bc.getClass()));
        compInfoOutputs = compInfo;
      }

      final Vector tempAssociatedBeans = associatedBeans;

      if (compInfo == null) {
        System.err.println("[KnowledgeFlow] Error in doPopup()");
      } else {
        //	System.err.println("Got bean info");
        for (int zz = 0; zz < compInfo.size(); zz++) {
          final int tt = zz;
          final Class custClass = ((BeanInfo) compInfo.elementAt(zz)).getBeanDescriptor()
            .getCustomizerClass();

          if (custClass != null) {
            //	  System.err.println("Got customizer class");
            //	  popupCustomizer(custClass, bc);
            //JMenuItem custItem = null;
            MenuItem custItem = null;
            boolean customizationEnabled = true;

            if (!(bc instanceof MetaBean)) {
              //custItem = new JMenuItem("Configure...");
              custItem = new MenuItem("Configure...");
              if (bc instanceof BeanCommon) {
                customizationEnabled = 
                  !((BeanCommon)bc).isBusy();
              }
            } else {
              String custName = custClass.getName();
              BeanInstance tbi = (BeanInstance) associatedBeans.elementAt(zz);
              if (tbi.getBean() instanceof BeanCommon) {
                custName = ((BeanCommon)tbi.getBean()).getCustomName();
              } else {
                if (tbi.getBean() instanceof WekaWrapper) {
                  custName = ((WekaWrapper) tbi.getBean()).getWrappedAlgorithm()
                  .getClass().getName();
                } else {
                  custName = custName.substring(0, custName.indexOf("Customizer"));
                }

                custName = custName.substring(custName.lastIndexOf('.') + 1,              
                                            custName.length());
              }
              //custItem = new JMenuItem("Configure: "+ custName);
              custItem = new MenuItem("Configure: " + custName);
              if (tbi.getBean() instanceof BeanCommon) {
                customizationEnabled = 
                  !((BeanCommon)tbi.getBean()).isBusy();
              }
            }

            custItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                  if (bc instanceof MetaBean) {
                    popupCustomizer(custClass,
                      (JComponent) ((BeanInstance) tempAssociatedBeans.
                                    elementAt(tt)).getBean());
                  } else {
                    popupCustomizer(custClass, bc);
                  }

                  notifyIsDirty();
                }
              });
            custItem.setEnabled(customizationEnabled);
            beanContextMenu.add(custItem);
            menuItemCount++;
          } else {
            System.err.println("[KnowledgeFlow] No customizer class");
          }
        }

        Vector esdV = new Vector();

        //for (int i = 0; i < compInfoOutputs.size(); i++) {
        for (int i = 0; i < compInfo.size(); i++) {
          EventSetDescriptor[] temp = 
          //  ((BeanInfo) compInfoOutputs.elementAt(i)).getEventSetDescriptors();
          ((BeanInfo) compInfo.elementAt(i)).getEventSetDescriptors();

          if ((temp != null) && (temp.length > 0)) {
            esdV.add(temp);
          }
        }

        //        EventSetDescriptor [] esds = compInfo.getEventSetDescriptors();
        //        if (esds != null && esds.length > 0) {
        if (esdV.size() > 0) {
          //          beanContextMenu.insert(new JLabel("Connections", 
          //                                            SwingConstants.CENTER), 
          //                                 menuItemCount);
          MenuItem connections = new MenuItem("Connections:");
          connections.setEnabled(false);
          beanContextMenu.insert(connections, menuItemCount);
          menuItemCount++;
        }

        //final Vector finalOutputs = outputBeans;
        final Vector finalOutputs = associatedBeans;

        for (int j = 0; j < esdV.size(); j++) {
          final int fj = j;
          String sourceBeanName = "";

          if (bc instanceof MetaBean) {
            //Object sourceBean = ((BeanInstance) outputBeans.elementAt(j)).getBean();
            Object sourceBean = ((BeanInstance) associatedBeans.elementAt(j)).getBean();
            if (sourceBean instanceof BeanCommon) {
              sourceBeanName = ((BeanCommon)sourceBean).getCustomName();
            } else {
              if (sourceBean instanceof WekaWrapper) {
                sourceBeanName = ((WekaWrapper) sourceBean).getWrappedAlgorithm()
                .getClass().getName();
              } else {
                sourceBeanName = sourceBean.getClass().getName();
              }

              sourceBeanName = 
                sourceBeanName.substring(sourceBeanName.lastIndexOf('.') + 1, 
                    sourceBeanName.length());
            }
            sourceBeanName += ": ";
          }

          EventSetDescriptor[] esds = (EventSetDescriptor[]) esdV.elementAt(j);

          for (int i = 0; i < esds.length; i++) {
            //	  System.err.println(esds[i].getName());
            // add each event name to the menu
            //            JMenuItem evntItem = new JMenuItem(sourceBeanName
            //                                               +esds[i].getName());
            MenuItem evntItem = new MenuItem(sourceBeanName +
                                             esds[i].getName());
            final EventSetDescriptor esd = esds[i];

            // Check EventConstraints (if any) here
            boolean ok = true;

            if (bc instanceof EventConstraints) {
              ok = ((EventConstraints) bc).eventGeneratable(esd.getName());
            }

            if (ok) {
              evntItem.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                    connectComponents(esd,
                                      (bc instanceof MetaBean)
                                      ? ((BeanInstance) finalOutputs.elementAt(fj)) : bi, xx, yy);
                    notifyIsDirty();
                  }
                });
            } else {
              evntItem.setEnabled(false);
            }

            beanContextMenu.add(evntItem);
            menuItemCount++;
          }
        }
      }
    } catch (IntrospectionException ie) {
      ie.printStackTrace();
    }

    //    System.err.println("Just before look for other options");
    // now look for other options for this bean
    if (bc instanceof UserRequestAcceptor || bc instanceof Startable) {
      Enumeration req = null;
      
      if (bc instanceof UserRequestAcceptor) {
        req = ((UserRequestAcceptor) bc).enumerateRequests();
      }

      if (/*(bc instanceof Startable) ||*/ (req !=null && req.hasMoreElements())) {
        //	beanContextMenu.insert(new JLabel("Actions", 
        //					  SwingConstants.CENTER), 
        //			       menuItemCount);
        MenuItem actions = new MenuItem("Actions:");
        actions.setEnabled(false);
        beanContextMenu.insert(actions, menuItemCount);
        menuItemCount++;
      }

      /*if (bc instanceof Startable) {
        String tempS = ((Startable)bc).getStartMessage();
        insertUserOrStartableMenuItem(bc, true, tempS, beanContextMenu);
      }*/
      
      while (req != null && req.hasMoreElements()) {
        String tempS = (String) req.nextElement();
        insertUserOrStartableMenuItem(bc, false, tempS, beanContextMenu);
        menuItemCount++;
      }
    }

    //    System.err.println("Just before showing menu");
    // popup the menu
    if (menuItemCount > 0) {
      //beanContextMenu.show(m_beanLayout, x, y);
      m_beanLayout.add(beanContextMenu);
      beanContextMenu.show(m_beanLayout, x, y);
    }
  }
  
  private void insertUserOrStartableMenuItem(final JComponent bc, 
      final boolean startable, String tempS, PopupMenu beanContextMenu) {

    boolean disabled = false;
    boolean confirmRequest = false;

    // check to see if this item is currently disabled
    if (tempS.charAt(0) == '$') {
      tempS = tempS.substring(1, tempS.length());
      disabled = true;
    }
    
    // check to see if this item requires confirmation
    if (tempS.charAt(0) == '?') {
      tempS = tempS.substring(1, tempS.length());
      confirmRequest = true;
    }

    final String tempS2 = tempS;

    //      JMenuItem custItem = new JMenuItem(tempS2);
    MenuItem custItem = new MenuItem(tempS2);
    if (confirmRequest) {
      custItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // 
          int result = JOptionPane.showConfirmDialog(KnowledgeFlowApp.this,
              tempS2,
              "Confirm action",
              JOptionPane.YES_NO_OPTION);
          if (result == JOptionPane.YES_OPTION) {
            Thread startPointThread = new Thread() {
              public void run() {
                try {
                  if (startable) {
                    ((Startable)bc).start();                    
                  } else if (bc instanceof UserRequestAcceptor) {
                    ((UserRequestAcceptor) bc).performRequest(tempS2);
                  }
                  notifyIsDirty();
                } catch (Exception ex) {
                  ex.printStackTrace();
                }
              }
            };
            startPointThread.setPriority(Thread.MIN_PRIORITY);
            startPointThread.start();
          }
        }
      });
    } else {
      custItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Thread startPointThread = new Thread() {
            public void run() {
              try {
                if (startable) {
                  ((Startable)bc).start();                  
                } else if (bc instanceof UserRequestAcceptor) {
                  ((UserRequestAcceptor) bc).performRequest(tempS2);
                }
                notifyIsDirty();
              } catch (Exception ex) {
                ex.printStackTrace();
              }
            }
          };
          startPointThread.setPriority(Thread.MIN_PRIORITY);
          startPointThread.start();
        }
      });
    }

    if (disabled) {
      custItem.setEnabled(false);
    }

    beanContextMenu.add(custItem); 
  }
  
  /**
   * Tells us about the modified status of a particular object - typically
   * a customizer that is editing a flow component. Allows us to set
   * the modified flag for the current flow.
   */
  public void setModifiedStatus(Object source, boolean modified) {
    if (source instanceof BeanCustomizer && modified) {
      m_mainKFPerspective.setEditedStatus(modified);
    }
  }

  /**
   * Popup the customizer for this bean
   *
   * @param custClass the class of the customizer
   * @param bc the bean to be customized
   */
  private void popupCustomizer(Class custClass, JComponent bc) {
    try {
      // instantiate
      final Object customizer = custClass.newInstance();
      // set environment **before** setting object!!
      if (customizer instanceof EnvironmentHandler) {
        ((EnvironmentHandler)customizer).setEnvironment(m_flowEnvironment);
      }
      
      if (customizer instanceof BeanCustomizer) {
        ((BeanCustomizer)customizer).setModifiedListener(this);
      }
      
      ((Customizer)customizer).setObject(bc);
      // final javax.swing.JFrame jf = new javax.swing.JFrame();
      final JDialog d = new JDialog((JFrame)KnowledgeFlowApp.this.getTopLevelAncestor(), true);
      d.setLayout(new BorderLayout());
      d.getContentPane().add((JComponent)customizer, BorderLayout.CENTER);
      
//      jf.getContentPane().setLayout(new BorderLayout());
  //    jf.getContentPane().add((JComponent)customizer, BorderLayout.CENTER);
      if (customizer instanceof CustomizerCloseRequester) {
	((CustomizerCloseRequester)customizer).setParentWindow(d);
      }
      d.addWindowListener(new java.awt.event.WindowAdapter() {
	  public void windowClosing(java.awt.event.WindowEvent e) {
	    if (customizer instanceof CustomizerClosingListener) {
	      ((CustomizerClosingListener)customizer).customizerClosing();
	    }
	    d.dispose();
	  }
	});
//      jf.pack();
  //    jf.setVisible(true);
      d.pack();
      d.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  
  private MetaBean copyMetaBean(MetaBean bean) {
    // copy the bean via serialization
    ((Visible)bean).getVisual().removePropertyChangeListener(this);
    bean.removePropertyChangeListenersSubFlow(this);
    Object copy = null;
    try {
      SerializedObject so = new SerializedObject(bean);
      copy = (MetaBean)so.getObject();
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
    ((Visible)bean).getVisual().addPropertyChangeListener(this);
    bean.addPropertyChangeListenersSubFlow(this);    
    
    return (MetaBean)copy;
  }

  private void addToUserTreeNode(MetaBean bean, boolean installListener) {
    DefaultTreeModel model = (DefaultTreeModel) m_componentTree.getModel();
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
    if (m_userCompNode == null) {            
      m_userCompNode = new DefaultMutableTreeNode("User");
      model.insertNodeInto(m_userCompNode, root, 0);
    }
    
    // Disconnect any beans connected to the inputs or outputs
    // of this MetaBean (prevents serialization of the entire
    // KnowledgeFlow!!)
    Vector tempRemovedConnections = new Vector();
    Vector allConnections = 
      BeanConnection.getConnections(m_mainKFPerspective.getCurrentTabIndex());
    Vector inputs = bean.getInputs();
    Vector outputs = bean.getOutputs();
    Vector allComps = bean.getSubFlow();
        
    for (int i = 0; i < inputs.size(); i++) {
      BeanInstance temp = (BeanInstance)inputs.elementAt(i);
      // is this input a target for some event?
      for (int j = 0; j < allConnections.size(); j++) {
        BeanConnection tempC = (BeanConnection)allConnections.elementAt(j);
        if (tempC.getTarget() == temp) {
          tempRemovedConnections.add(tempC);
        }
        
        // also check to see if this input is a source for
        // some target that is *not* in the subFlow
        if (tempC.getSource() == temp && !bean.subFlowContains(tempC.getTarget())) {
          tempRemovedConnections.add(tempC);
        }
      }
    }

    for (int i = 0; i < outputs.size(); i++) {
      BeanInstance temp = (BeanInstance)outputs.elementAt(i);
      // is this output a source for some target?
      for (int j = 0; j < allConnections.size(); j++) {
        BeanConnection tempC = (BeanConnection)allConnections.elementAt(j);
        if (tempC.getSource() == temp) {
          tempRemovedConnections.add(tempC);
        }
      }
    }
    
    
    for (int i = 0; i < tempRemovedConnections.size(); i++) {
      BeanConnection temp = 
        (BeanConnection)tempRemovedConnections.elementAt(i);
      temp.remove(m_mainKFPerspective.getCurrentTabIndex());
    }
    
    MetaBean copy = copyMetaBean(bean);
    String displayName ="";
    ImageIcon scaledIcon = null;
    //
    if (copy instanceof Visible) {
      //((Visible)copy).getVisual().scale(3);
      scaledIcon = new ImageIcon(((Visible)copy).getVisual().scale(0.33));
      displayName = ((Visible)copy).getVisual().getText();
    }
    
    JTreeLeafDetails metaLeaf = new JTreeLeafDetails(displayName, copy, scaledIcon);
    DefaultMutableTreeNode newUserComp = new DefaultMutableTreeNode(metaLeaf);
    model.insertNodeInto(newUserComp, m_userCompNode, 0);
    
    // add to the list of user components
    m_userComponents.add(copy);
    
    if (installListener && m_firstUserComponentOpp) {
      try {
        installWindowListenerForSavingUserBeans();
        m_firstUserComponentOpp = false;
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    
    // Now reinstate any deleted connections to the original MetaBean
    for (int i = 0; i < tempRemovedConnections.size(); i++) {
      BeanConnection temp = 
        (BeanConnection)tempRemovedConnections.elementAt(i);
      BeanConnection newC = 
        new BeanConnection(temp.getSource(), temp.getTarget(),
                           temp.getSourceEventSetDescriptor(),
                           m_mainKFPerspective.getCurrentTabIndex());
    }    
  }
  
  private boolean copyToClipboard() {
    Vector selectedBeans = m_mainKFPerspective.getSelectedBeans();
    if (selectedBeans == null || selectedBeans.size() == 0) {
      return false;
    }
    
    Vector associatedConnections = 
      BeanConnection.associatedConnections(selectedBeans, 
          m_mainKFPerspective.getCurrentTabIndex());
    
    // xml serialize to a string and store in the
    // clipboard variable
    Vector v = new Vector();
    v.setSize(2);
    v.set(XMLBeans.INDEX_BEANINSTANCES, selectedBeans);
    v.set(XMLBeans.INDEX_BEANCONNECTIONS, associatedConnections);
    try {
      XMLBeans xml = new XMLBeans(m_beanLayout, m_bcSupport, 
          m_mainKFPerspective.getCurrentTabIndex());
      java.io.StringWriter sw = new java.io.StringWriter();
      xml.write(sw, v);
      m_pasteBuffer = sw.getBuffer();
      //System.out.println(m_pasteBuffer.toString());
    } catch (Exception e1) {
      m_logPanel.logMessage("[KnowledgeFlow] problem copying beans: " 
          + e1.getMessage());
      e1.printStackTrace();
      return false;
    }
    
    m_pasteB.setEnabled(true);
    return true;
  }
  
  private boolean pasteFromClipboard(int x, int y) {
    addUndoPoint();
    
    java.io.StringReader sr = 
      new java.io.StringReader(m_pasteBuffer.toString());
    try {
      XMLBeans xml = new XMLBeans(m_beanLayout, m_bcSupport, 
          m_mainKFPerspective.getCurrentTabIndex());
      Vector v = (Vector)xml.read(sr);
      Vector beans = (Vector)v.get(XMLBeans.INDEX_BEANINSTANCES);
      Vector connections = (Vector)v.get(XMLBeans.INDEX_BEANCONNECTIONS);
      
      // TODO adjust beans coords with respect to x, y. Look for 
      // the smallest x and the smallest y (top left corner of the bounding)
      // box.
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      boolean adjust = false;
      for (int i = 0; i < beans.size(); i++) {
        BeanInstance b = (BeanInstance)beans.get(i);
        if (b.getX() < minX) {
          minX = b.getX();
          adjust = true;
        }
        if (b.getY() < minY) {
          minY = b.getY();
          adjust = true;
        }
      }
      if (adjust) {
        int deltaX = x - minX;
        int deltaY = y - minY;
        for (int i = 0; i < beans.size(); i++) {
          BeanInstance b = (BeanInstance)beans.get(i);
          b.setX(b.getX() + deltaX);
          b.setY(b.getY() + deltaY);
        }
      }            
      
      // TODO integrate these beans
      integrateFlow(beans, connections, false, false);
      setEnvironment();
      notifyIsDirty();
      m_mainKFPerspective.setSelectedBeans(beans);
    } catch (Exception e) {
      m_logPanel.logMessage("[KnowledgeFlow] problem pasting beans: " 
          + e.getMessage());
      e.printStackTrace();
    }
    
    revalidate();
    notifyIsDirty();
    
    return true;
  }
  
  private void deleteSelectedBeans() {
    
    Vector v = m_mainKFPerspective.getSelectedBeans();
    if (v.size() > 0) {
      m_mainKFPerspective.setSelectedBeans(new Vector());
    }
    addUndoPoint();
    
    for (int i = 0; i < v.size(); i++) {
      BeanInstance b = (BeanInstance)v.get(i);

      BeanConnection.removeConnections(b, m_mainKFPerspective.getCurrentTabIndex());
      b.removeBean(m_beanLayout, m_mainKFPerspective.getCurrentTabIndex());
      if (b instanceof BeanCommon) {            
        String key = ((BeanCommon)b).getCustomName()
        + "$" + b.hashCode();
        m_logPanel.statusMessage(key + "|remove");
      }
    }
    m_mainKFPerspective.setSelectedBeans(new Vector());
    revalidate();
    notifyIsDirty();
    
    m_selectAllB.setEnabled(BeanInstance.
        getBeanInstances(m_mainKFPerspective.getCurrentTabIndex()).size() > 0);
  }
  
  private void addUndoPoint() {
    try {
      Stack undo = m_mainKFPerspective.getUndoBuffer();
      File tempFile = File.createTempFile("knowledgeFlow", FILE_EXTENSION);
      tempFile.deleteOnExit();

      if(saveLayout(tempFile, m_mainKFPerspective.getCurrentTabIndex(), true)) {
        undo.push(tempFile);
        
        // keep no more than 20 undo points
        if (undo.size() > 20) {
          undo.remove(0);
        }
        m_undoB.setEnabled(true);
      }
      
    } catch (Exception ex) {
      m_logPanel.logMessage("[KnowledgeFlow] a problem occurred while trying to " +
      		"create a undo point : " + ex.getMessage());
    }    
  }
  
  // right click over empty canvas (not on a bean)
  private void rightClickCanvasPopup(final int x, final int y) {
    
    Vector closestConnections = 
      BeanConnection.getClosestConnections(new Point(x, y), 
                            10, m_mainKFPerspective.getCurrentTabIndex());

    PopupMenu rightClickMenu = new PopupMenu();
    int menuItemCount = 0;
    if (m_mainKFPerspective.getSelectedBeans().size() > 0 ||
        closestConnections.size() > 0 || 
        (m_pasteBuffer != null && m_pasteBuffer.length() > 0)) {
      
      if (m_mainKFPerspective.getSelectedBeans().size() > 0) {
        
        MenuItem snapItem = new MenuItem("Snap selected to grid");
        snapItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            snapSelectedToGrid();
          }
        });
        rightClickMenu.add(snapItem);
        menuItemCount++;
        
        MenuItem copyItem = new MenuItem("Copy selected");
        copyItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            
            copyToClipboard();
            m_mainKFPerspective.setSelectedBeans(new Vector());
          }
        });
        rightClickMenu.add(copyItem);
        menuItemCount++;
        
        MenuItem cutItem = new MenuItem("Cut selected");
        cutItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // only delete if our copy was successful!
            if (copyToClipboard()) {              
              deleteSelectedBeans();
            }
          }
        });
        rightClickMenu.add(cutItem);
        menuItemCount++;
        
        MenuItem deleteSelected = new MenuItem("Delete selected");
        deleteSelected.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            
            deleteSelectedBeans();
          }
        });
        rightClickMenu.add(deleteSelected);
        menuItemCount++;
        
        // Able to group selected subflow?
        boolean groupable = true;
        final Vector selected = m_mainKFPerspective.getSelectedBeans();
        // check if sub flow is valid
        final Vector inputs = BeanConnection.inputs(selected, 
            m_mainKFPerspective.getCurrentTabIndex());
        final Vector outputs = BeanConnection.outputs(selected, 
            m_mainKFPerspective.getCurrentTabIndex());
        
        // screen the inputs and outputs
        if (inputs.size() == 0 || outputs.size() == 0) {
          groupable = false;
        }

        // dissallow MetaBeans in the selected set (for the
        // time being).
        if (groupable) {
          for (int i = 0; i < selected.size(); i++) {
            BeanInstance temp = (BeanInstance)selected.elementAt(i);
            if (temp.getBean() instanceof MetaBean) {
              groupable = false;
              break;
            }
          }
        }

        if (groupable) {
          // show connector dots for input beans
          for (int i = 0; i < inputs.size(); i++) {
            BeanInstance temp = (BeanInstance)inputs.elementAt(i);
            if (temp.getBean() instanceof Visible) {
              ((Visible)temp.getBean()).getVisual().
              setDisplayConnectors(true, java.awt.Color.red);
            }
          }

          // show connector dots for output beans
          for (int i = 0; i < outputs.size(); i++) {
            BeanInstance temp = (BeanInstance)outputs.elementAt(i);
            if (temp.getBean() instanceof Visible) {
              ((Visible)temp.getBean()).getVisual().
              setDisplayConnectors(true, java.awt.Color.green);
            }
          }
          
          MenuItem groupItem = new MenuItem("Group selected");
          groupItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              groupSubFlow(selected, inputs, outputs);
            }
          });
          rightClickMenu.add(groupItem);
          menuItemCount++;
        }                        
      }      
      
      if (m_pasteBuffer != null && m_pasteBuffer.length() > 0) {
        rightClickMenu.addSeparator();
        menuItemCount++;
        
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // deserialize, integerate and
            // position at x, y

            pasteFromClipboard(x, y);
          }        
        });
        rightClickMenu.add(pasteItem);
        menuItemCount++;
      }
      
      
      if (closestConnections.size() > 0) {
        rightClickMenu.addSeparator();
        menuItemCount++;
        
        MenuItem deleteConnection = new MenuItem("Delete Connection:");
        deleteConnection.setEnabled(false);
        rightClickMenu.insert(deleteConnection, menuItemCount);
        menuItemCount++;

        for (int i = 0; i < closestConnections.size(); i++) {
          final BeanConnection bc = (BeanConnection) closestConnections.elementAt(i);
          String connName = bc.getSourceEventSetDescriptor().getName();

          //JMenuItem deleteItem = new JMenuItem(connName);
          String targetName = "";
          if (bc.getTarget().getBean() instanceof BeanCommon) {
            targetName = ((BeanCommon)bc.getTarget().getBean()).getCustomName();
          } else {
            targetName = bc.getTarget().getBean().getClass().getName();
            targetName = targetName.substring(targetName.lastIndexOf('.')+1, targetName.length());
          }
          MenuItem deleteItem = new MenuItem(connName + "-->" + targetName);
          deleteItem.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                addUndoPoint();
                
                bc.remove(m_mainKFPerspective.getCurrentTabIndex());
                m_beanLayout.revalidate();
                m_beanLayout.repaint();
                m_mainKFPerspective.setEditedStatus(true);
                if (m_mainKFPerspective.getSelectedBeans().size() > 0) {
                  m_mainKFPerspective.setSelectedBeans(new Vector());
                }                
                notifyIsDirty();
              }
            });
          rightClickMenu.add(deleteItem);
          menuItemCount++;
        }
      }            
    }
    
    if (menuItemCount > 0) {
      rightClickMenu.addSeparator();
      menuItemCount++;
    }
    
    MenuItem noteItem = new MenuItem("New note");
    noteItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        
        Note n = new Note();
        m_toolBarBean = n;
       
        setCursor(Cursor.
            getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        m_mode = ADDING;
      }
    });
    rightClickMenu.add(noteItem);
    menuItemCount++;
    
    m_beanLayout.add(rightClickMenu);
    rightClickMenu.show(m_beanLayout, x, y);
  }
                                
  /**
   * Popup a menu giving choices for connections to delete (if any)
   *
   * @param closestConnections a vector containing 0 or more BeanConnections
   * @param x the x coordinate at which to popup the menu
   * @param y the y coordinate at which to popup the menu
   *
   * Modified by Zerbetto: javax.swing.JPopupMenu transformed into java.awt.PopupMenu
   */
  private void deleteConnectionPopup(Vector closestConnections, int x, int y) {
    if (closestConnections.size() > 0) {
      int menuItemCount = 0;

      // modifications by Zerbetto
      //JPopupMenu deleteConnectionMenu = new JPopupMenu();
      PopupMenu deleteConnectionMenu = new PopupMenu();

      //      deleteConnectionMenu.insert(new JLabel("Delete Connection", 
      //					     SwingConstants.CENTER), 
      //				  menuItemCount);
      MenuItem deleteConnection = new MenuItem("Delete Connection:");
      deleteConnection.setEnabled(false);
      deleteConnectionMenu.insert(deleteConnection, menuItemCount);
      menuItemCount++;

      for (int i = 0; i < closestConnections.size(); i++) {
        final BeanConnection bc = (BeanConnection) closestConnections.elementAt(i);
        String connName = bc.getSourceEventSetDescriptor().getName();

        //JMenuItem deleteItem = new JMenuItem(connName);
        String targetName = "";
        if (bc.getTarget().getBean() instanceof BeanCommon) {
          targetName = ((BeanCommon)bc.getTarget().getBean()).getCustomName();
        } else {
          targetName = bc.getTarget().getBean().getClass().getName();
          targetName = targetName.substring(targetName.lastIndexOf('.')+1, targetName.length());
        }
        MenuItem deleteItem = new MenuItem(connName + "-->" + targetName);
        deleteItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              bc.remove(m_mainKFPerspective.getCurrentTabIndex());
              m_beanLayout.revalidate();
              m_beanLayout.repaint();
              m_mainKFPerspective.setEditedStatus(true);
              notifyIsDirty();
            }
          });
        deleteConnectionMenu.add(deleteItem);
        menuItemCount++;
      }

      //deleteConnectionMenu.show(m_beanLayout, x, y);
      m_beanLayout.add(deleteConnectionMenu);
      deleteConnectionMenu.show(m_beanLayout, x, y);
    }
  }

  /**
   * Initiates the connection process for two beans
   *
   * @param esd the EventSetDescriptor for the source bean
   * @param bi the source bean
   * @param x the x coordinate to start connecting from
   * @param y the y coordinate to start connecting from
   */
  private void connectComponents(EventSetDescriptor esd, 
				 BeanInstance bi,
				 int x,
				 int y) {
    // record the event set descriptior for this event
    m_sourceEventSetDescriptor = esd;

    Class listenerClass = esd.getListenerType(); // class of the listener
    JComponent source = (JComponent)bi.getBean();
    // now determine which (if any) of the other beans implement this
    // listener
    int targetCount = 0;
    Vector beanInstances = 
      BeanInstance.getBeanInstances(m_mainKFPerspective.getCurrentTabIndex());
    for (int i = 0; i < beanInstances.size(); i++) {
      JComponent bean = 
	(JComponent)((BeanInstance)beanInstances.elementAt(i)).getBean();
      boolean connectable = false;
      boolean canContinue = false;
      if (bean != source) {
        if (bean instanceof MetaBean) {
          if (((MetaBean)bean).canAcceptConnection(listenerClass)) {
            canContinue = true;
          }
        } else if (listenerClass.isInstance(bean) && bean != source) {
          canContinue = true;
        }
      }
      if (canContinue) {
	if (!(bean instanceof BeanCommon)) {
	  connectable = true; // assume this bean is happy to receive a connection
	} else {
	  // give this bean a chance to veto any proposed connection via
	  // the listener interface
	  if (((BeanCommon)bean).
	      //connectionAllowed(esd.getName())) {
	      connectionAllowed(esd)) {
	    connectable = true;
	  }
	}
	if (connectable) {
	  if (bean instanceof Visible) {
	    targetCount++;
	    ((Visible)bean).getVisual().setDisplayConnectors(true);
	  }
	}
      }
    }
    
    // have some possible beans to connect to?
    if (targetCount > 0) {
      //      System.err.println("target count "+targetCount);
      if (source instanceof Visible) {
        ((Visible)source).getVisual().setDisplayConnectors(true);


        m_editElement = bi;
        Point closest = ((Visible)source).getVisual().
        getClosestConnectorPoint(new Point(x, y));

        m_startX = (int)closest.getX();
        m_startY = (int)closest.getY();
        m_oldX = m_startX;
        m_oldY = m_startY;

        Graphics2D gx = (Graphics2D)m_beanLayout.getGraphics();
        gx.setXORMode(java.awt.Color.white);
        gx.drawLine(m_startX, m_startY, m_startX, m_startY);
        gx.dispose();
        m_mode = CONNECTING;
      }
    }
  }

  private void addComponent(BeanInstance comp, boolean repaint) {
    if (comp.getBean() instanceof Visible) {
      ((Visible)comp.getBean()).getVisual().addPropertyChangeListener(this);
    }
    if (comp.getBean() instanceof BeanCommon) {
      ((BeanCommon)comp.getBean()).setLog(m_logPanel);
    }
    if (comp.getBean() instanceof MetaBean) {
      // re-align sub-beans
      Vector list;
      
      list = ((MetaBean) comp.getBean()).getInputs();
      for (int i = 0; i < list.size(); i++) {
        ((BeanInstance) list.get(i)).setX(comp.getX());
        ((BeanInstance) list.get(i)).setY(comp.getY());
      }

      list = ((MetaBean) comp.getBean()).getOutputs();
      for (int i = 0; i < list.size(); i++) {
        ((BeanInstance) list.get(i)).setX(comp.getX());
        ((BeanInstance) list.get(i)).setY(comp.getY());
      }
    }
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    if (repaint) {
      m_beanLayout.repaint();
    }
    m_pointerB.setSelected(true);
    m_mode = NONE;
    
    m_selectAllB.setEnabled(BeanInstance.
        getBeanInstances(m_mainKFPerspective.getCurrentTabIndex()).size() > 0);
  }

  private void addComponent(int x, int y) {
    if (m_toolBarBean instanceof MetaBean) {
      // need to add the MetaBean's internal connections
      // to BeanConnection's vector
      Vector associatedConnections = 
        ((MetaBean)m_toolBarBean).getAssociatedConnections();
      BeanConnection.getConnections().addAll(associatedConnections);
    }

    if (m_toolBarBean instanceof BeanContextChild) {
      m_bcSupport.add(m_toolBarBean);
    }
    BeanInstance bi = new BeanInstance(m_beanLayout, m_toolBarBean, x, y, 
        m_mainKFPerspective.getCurrentTabIndex());
    //    addBean((JComponent)bi.getBean());
    m_toolBarBean = null;
    addComponent(bi, true);
  }
  
  private void highlightSubFlow(int startX, int startY,
      int endX, int endY) {
    java.awt.Rectangle r = 
      new java.awt.Rectangle((startX < endX) ? startX : endX,
                             (startY < endY) ? startY: endY,
                             Math.abs(startX - endX),
                             Math.abs(startY - endY));
    //    System.err.println(r);
    Vector selected = 
      BeanInstance.findInstances(r, m_mainKFPerspective.getCurrentTabIndex());
    
    // show connector dots for selected beans
    /*for (int i = 0; i < selected.size(); i++) {
      BeanInstance temp = (BeanInstance)selected.elementAt(i);
      if (temp.getBean() instanceof Visible) {
        ((Visible)temp.getBean()).getVisual().setDisplayConnectors(true);
      }
    }*/
    
    m_mainKFPerspective.setSelectedBeans(selected);
  }
  
  private void groupSubFlow(Vector selected, Vector inputs, Vector outputs) {
    
    int upperLeftX = Integer.MAX_VALUE;
    int upperLeftY = Integer.MAX_VALUE;
    int lowerRightX = Integer.MIN_VALUE;
    int lowerRightY = Integer.MIN_VALUE;
    for (int i = 0; i < selected.size(); i++) {
      BeanInstance b = (BeanInstance)selected.get(i);
      if (b.getX() < upperLeftX) {
        upperLeftX = b.getX();
      }
      
      if (b.getY() < upperLeftY) {
        upperLeftY = b.getY();
      }
      
      if (b.getX() > lowerRightX) {
        // ImageIcon ic = ((Visible)b.getBean()).getVisual().getStaticIcon();
//        lowerRightX = (b.getX() + ic.getIconWidth());
        lowerRightX = b.getX();
      }
      
      if (b.getY() > lowerRightY) {
        // ImageIcon ic = ((Visible)b.getBean()).getVisual().getStaticIcon();
        // lowerRightY = (b.getY() + ic.getIconHeight());
        lowerRightY = b.getY();
      }
    }
        
    java.awt.Rectangle r = new java.awt.Rectangle(upperLeftX, upperLeftY,
        lowerRightX, lowerRightY);
    
    BufferedImage subFlowPreview = null; 
    try {
        subFlowPreview = createImage(m_beanLayout, r);              
    } catch (IOException ex) {
      ex.printStackTrace();
      // drop through quietly
    }

    // Confirmation pop-up
    int result = JOptionPane.showConfirmDialog(KnowledgeFlowApp.this,
                                               "Group this sub-flow?",
                                               "Group Components",
                                               JOptionPane.YES_NO_OPTION);
    if (result == JOptionPane.YES_OPTION) {
      Vector associatedConnections = 
        BeanConnection.associatedConnections(selected);

      String name = JOptionPane.showInputDialog(KnowledgeFlowApp.this,
                                                "Enter a name for this group",
                                                "MyGroup");
      if (name != null) {       
        MetaBean group = new MetaBean();
        group.setSubFlow(selected);
        group.setAssociatedConnections(associatedConnections);
        group.setInputs(inputs);
        group.setOutputs(outputs);
        group.setSubFlowPreview(new ImageIcon(subFlowPreview));
        if (name.length() > 0) {
          //          group.getVisual().setText(name);
          group.setCustomName(name);
        }
        
        if (group instanceof BeanContextChild) {
          m_bcSupport.add(group);
        }
        
        int bx = (int)r.getCenterX();
        int by = (int)r.getCenterY();
        
        /*BeanInstance bi = new BeanInstance(m_beanLayout, group, 
                                           (int)r.getX()+(int)(r.getWidth()/2),
                                           (int)r.getY()+(int)(r.getHeight()/2),
                                           m_mainKFPerspective.getCurrentTabIndex()); */
        BeanInstance bi = new BeanInstance(m_beanLayout, group, bx, by, 
            m_mainKFPerspective.getCurrentTabIndex());
        System.err.println("Meta x, y " + bi.getX() + " " +bi.getY());
        for (int i = 0; i < selected.size(); i++) {
          BeanInstance temp = (BeanInstance)selected.elementAt(i);
          temp.removeBean(m_beanLayout, m_mainKFPerspective.getCurrentTabIndex());
          if (temp.getBean() instanceof Visible) {
            ((Visible)temp.getBean()).getVisual().removePropertyChangeListener(this);
          }
        }
        for (int i = 0; i < associatedConnections.size(); i++) {
          BeanConnection temp = (BeanConnection)associatedConnections.elementAt(i);
          temp.setHidden(true);
        }
        group.shiftBeans(bi, true);
        
        addComponent(bi, true);
      }
    }
    
    for (int i = 0; i < selected.size(); i++) {
      BeanInstance temp = (BeanInstance)selected.elementAt(i);
      if (temp.getBean() instanceof Visible) {
        ((Visible)temp.getBean()).getVisual().setDisplayConnectors(false);
      }
    }
    
    m_mainKFPerspective.setSelectedBeans(new Vector());
    
    revalidate();
    notifyIsDirty();
  }

  /**
   * Accept property change events
   *
   * @param e a <code>PropertyChangeEvent</code> value
   */
  public void propertyChange(PropertyChangeEvent e) {
    revalidate();
    m_beanLayout.repaint();
  }
  
  /**
   * Load a pre-saved layout
   */
  private void loadLayout() {
    m_loadB.setEnabled(false);
    m_saveB.setEnabled(false);
    m_playB.setEnabled(false);
    m_playBB.setEnabled(false);
    
    int returnVal = m_FileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      stopFlow();

      // determine filename
      File oFile = m_FileChooser.getSelectedFile();
      // set internal flow directory environment variable      

      // add extension if necessary
      if (m_FileChooser.getFileFilter() == m_KfFilter) {
        if (!oFile.getName().toLowerCase().endsWith(FILE_EXTENSION)) {
          oFile = new File(oFile.getParent(), 
                           oFile.getName() + FILE_EXTENSION);
        }
      } else if (m_FileChooser.getFileFilter() == m_KOMLFilter) {
        if (!oFile.getName().toLowerCase().endsWith(KOML.FILE_EXTENSION + "kf")) {
          oFile = new File(oFile.getParent(), 
                           oFile.getName() + KOML.FILE_EXTENSION + "kf");
        }
      } else if (m_FileChooser.getFileFilter() == m_XMLFilter) {
        if (!oFile.getName().toLowerCase().endsWith(FILE_EXTENSION_XML)) {
          oFile = new File(oFile.getParent(), 
                           oFile.getName() + FILE_EXTENSION_XML);
        }
      } else if (m_FileChooser.getFileFilter() == m_XStreamFilter) {
        if (!oFile.getName().toLowerCase().endsWith(XStream.FILE_EXTENSION +"kf")) {
          oFile = new File(oFile.getParent(), 
                           oFile.getName() + XStream.FILE_EXTENSION + "kf");
        }
      }
      
      String flowName = oFile.getName();
      if (flowName.lastIndexOf('.') > 0) {
        flowName = flowName.substring(0, flowName.lastIndexOf('.'));
      }
      
      loadLayout(oFile, getAllowMultipleTabs());
    }
    m_loadB.setEnabled(true);
    m_playB.setEnabled(true);
    m_playBB.setEnabled(true);
    m_saveB.setEnabled(true);    
  }
  
  /**
   * Load a layout from a file
   * 
   * @param oFile the file to load from
   * @param newTab true if the loaded layout should be displayed in a new tab
   */
  public void loadLayout(File oFile, boolean newTab) {
    loadLayout(oFile, newTab, false);
  }
  
  /**
   * Load a layout from a file
   * 
   * @param oFile the file to load from
   * @param newTab true if the loaded layout should be displayed in a new tab
   * @param isUndo is this file an "undo" file?
   */
  protected void loadLayout(File oFile, boolean newTab, boolean isUndo) {
    stopFlow();
    m_loadB.setEnabled(false);
    m_saveB.setEnabled(false);
    m_playB.setEnabled(false);
    m_playBB.setEnabled(false);
    
    if (newTab) {
      String flowName = oFile.getName();
      if (flowName.lastIndexOf('.') > 0) {
        flowName = flowName.substring(0, flowName.lastIndexOf('.'));
      }
      m_mainKFPerspective.addTab(flowName);
      //m_mainKFPerspective.setActiveTab(m_mainKFPerspective.getNumTabs() - 1);
      m_mainKFPerspective.setFlowFile(oFile);
      m_mainKFPerspective.setEditedStatus(false);
    }
    
    if (!isUndo) {
      m_flowEnvironment.addVariable("Internal.knowledgeflow.directory", oFile.getParent());
    }
    
    try {
      Vector beans       = new Vector();
      Vector connections = new Vector();

      // KOML?
      if ( (KOML.isPresent()) && 
           (oFile.getAbsolutePath().toLowerCase().
            endsWith(KOML.FILE_EXTENSION + "kf")) ) {
        Vector v     = (Vector) KOML.read(oFile.getAbsolutePath());
        beans        = (Vector) v.get(XMLBeans.INDEX_BEANINSTANCES);
        connections  = (Vector) v.get(XMLBeans.INDEX_BEANCONNECTIONS);
      } /* XStream */ else if ( (XStream.isPresent()) && 
           (oFile.getAbsolutePath().toLowerCase().
            endsWith(XStream.FILE_EXTENSION + "kf")) ) {
        Vector v     = (Vector) XStream.read(oFile.getAbsolutePath());
        beans        = (Vector) v.get(XMLBeans.INDEX_BEANINSTANCES);
        connections  = (Vector) v.get(XMLBeans.INDEX_BEANCONNECTIONS);
      } /* XML? */ else if (oFile.getAbsolutePath().toLowerCase().
                            endsWith(FILE_EXTENSION_XML)) {
        XMLBeans xml = new XMLBeans(m_beanLayout, m_bcSupport, 
            m_mainKFPerspective.getCurrentTabIndex());
        Vector v     = (Vector) xml.read(oFile);
        beans        = (Vector) v.get(XMLBeans.INDEX_BEANINSTANCES);
        connections  = (Vector) v.get(XMLBeans.INDEX_BEANCONNECTIONS);
        //connections  = new Vector();
      } /* binary */ else {
        InputStream is = new FileInputStream(oFile);
        ObjectInputStream ois = new ObjectInputStream(is);
        beans = (Vector) ois.readObject();
        connections = (Vector) ois.readObject();
        ois.close();
      }                

      integrateFlow(beans, connections, true, false);
      setEnvironment();
      if (newTab) {
        m_logPanel.clearStatus();
        m_logPanel.statusMessage("[KnowledgeFlow]|Flow loaded.");
      }
    } catch (Exception ex) {
      m_logPanel.statusMessage("[KnowledgeFlow]|Unable to load flow (see log).");
      m_logPanel.logMessage("[KnowledgeFlow] Unable to load flow ("
          + ex.getMessage() + ").");
      ex.printStackTrace();
    }        
    m_loadB.setEnabled(true);
    m_saveB.setEnabled(true);
    m_playB.setEnabled(true);
    m_playBB.setEnabled(true);
  }

  // Link the supplied beans into the KnowledgeFlow gui
  private void integrateFlow(Vector beans, Vector connections, boolean replace,
      boolean notReplaceAndSourcedFromBinary) {
    java.awt.Color bckC = getBackground();
    m_bcSupport = new BeanContextSupport();
    m_bcSupport.setDesignTime(true);

    // register this panel as a property change listener with each
    // bean
    for (int i = 0; i < beans.size(); i++) {
      BeanInstance tempB = (BeanInstance)beans.elementAt(i);
      if (tempB.getBean() instanceof Visible) {
        ((Visible)(tempB.getBean())).getVisual().
          addPropertyChangeListener(this);

        // A workaround to account for JPanel's with their default
        // background colour not being serializable in Apple's JRE
        ((Visible)(tempB.getBean())).getVisual().
          setBackground(bckC);
        ((JComponent)(tempB.getBean())).setBackground(bckC);
      }
      if (tempB.getBean() instanceof BeanCommon) {
        ((BeanCommon)(tempB.getBean())).setLog(m_logPanel);
      }
      if (tempB.getBean() instanceof BeanContextChild) {
        m_bcSupport.add(tempB.getBean());
      }
    }
    
    if (replace) {
      BeanInstance.setBeanInstances(beans, m_beanLayout, 
          m_mainKFPerspective.getCurrentTabIndex());
      BeanConnection.setConnections(connections, 
          m_mainKFPerspective.getCurrentTabIndex());
    } else if (notReplaceAndSourcedFromBinary){
      BeanInstance.appendBeans(m_beanLayout, beans, 
          m_mainKFPerspective.getCurrentTabIndex());
      BeanConnection.appendConnections(connections, 
          m_mainKFPerspective.getCurrentTabIndex());
    }
    m_beanLayout.revalidate();
    m_beanLayout.repaint();
    
    m_selectAllB.setEnabled(BeanInstance.
        getBeanInstances(m_mainKFPerspective.getCurrentTabIndex()).size() > 0);    
  }

  /**
   * Set the flow for the KnowledgeFlow to edit. Assumes that client
   * has loaded a Vector of beans and a Vector of connections. the supplied
   * beans and connections are deep-copied via serialization before being
   * set in the layout.
   *
   * @param v a Vector containing a Vector of beans and a Vector of connections
   * @exception Exception if something goes wrong
   */
  public void setFlow(Vector v) throws Exception {
    //    Vector beansCopy = null, connectionsCopy = null;
    clearLayout();
    SerializedObject so = new SerializedObject(v);
    Vector copy = (Vector)so.getObject();
    
    Vector beans = (Vector)copy.elementAt(0);
    Vector connections = (Vector)copy.elementAt(1);
    
    // reset environment variables
    m_flowEnvironment = new Environment();
    integrateFlow(beans, connections, true, false);
  }

  /**
   * Gets the current flow being edited. The flow is returned as a single
   * Vector containing two other Vectors: the beans and the connections.
   * These two vectors are deep-copied via serialization before being
   * returned.
   *
   * @return the current flow being edited
   */
  public Vector getFlow() throws Exception {
    Vector v = new Vector();
    Vector beans = 
      BeanInstance.getBeanInstances(m_mainKFPerspective.getCurrentTabIndex());
    Vector connections = 
      BeanConnection.getConnections(m_mainKFPerspective.getCurrentTabIndex());
    detachFromLayout(beans);
    v.add(beans);
    v.add(connections);

    SerializedObject so = new SerializedObject(v);
    Vector copy = (Vector)so.getObject();

    //    tempWrite(beans, connections);
    
    integrateFlow(beans, connections, true, false);
    return copy;
  }
  
  /**
   * Utility method to create an image of a region of the given component
   * @param component the component to create an image of
   * @param region the region of the component to put into the image
   * @return the image
   * @throws IOException
   */
  protected static BufferedImage createImage(JComponent component, Rectangle region)
  throws IOException {
    boolean opaqueValue = component.isOpaque();
    component.setOpaque( true );
    BufferedImage image = new BufferedImage(region.width, 
	region.height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();
    g2d.translate(-region.getX(), -region.getY());
    //g2d.setClip( region );
    component.paint( g2d );
    g2d.dispose();
    component.setOpaque( opaqueValue );
    
    return image;
  }

  // Remove this panel as a property changle listener from
  // each bean
  private void detachFromLayout(Vector beans) {
    for (int i = 0; i < beans.size(); i++) {
      BeanInstance tempB = (BeanInstance)beans.elementAt(i);
      if (tempB.getBean() instanceof Visible) {
        ((Visible)(tempB.getBean())).getVisual().
          removePropertyChangeListener(this);
          
        if (tempB.getBean() instanceof MetaBean) {
          ((MetaBean)tempB.getBean()).
            removePropertyChangeListenersSubFlow(this);
        }

        // A workaround to account for JPanel's with their default
        // background colour not being serializable in Apple's JRE.
        // JComponents are rendered with a funky stripy background
        // under OS X using java.awt.TexturePaint - unfortunately
        // TexturePaint doesn't implement Serializable.
        ((Visible)(tempB.getBean())).getVisual().
          setBackground(java.awt.Color.white);
        ((JComponent)(tempB.getBean())).setBackground(java.awt.Color.white);
      }
    }
  }
  
  public void saveLayout(File toFile, int tabIndex) {
    saveLayout(toFile, tabIndex, false);
  }
  
  protected boolean saveLayout(File sFile, int tabIndex, boolean isUndoPoint) {
    java.awt.Color bckC = getBackground();
    
    Vector beans = 
      BeanInstance.getBeanInstances(tabIndex);
    detachFromLayout(beans);

    // now serialize components vector and connections vector
    try {
      // KOML?
      if ((KOML.isPresent()) && 
          (sFile.getAbsolutePath().toLowerCase().
              endsWith(KOML.FILE_EXTENSION + "kf")) ) {
        Vector v = new Vector();
        v.setSize(2);
        v.set(XMLBeans.INDEX_BEANINSTANCES, beans);
        v.set(XMLBeans.INDEX_BEANCONNECTIONS, 
            BeanConnection.getConnections(tabIndex));
        KOML.write(sFile.getAbsolutePath(), v);
      } /* XStream */ else if ((XStream.isPresent()) && 
          (sFile.getAbsolutePath().toLowerCase().
              endsWith(XStream.FILE_EXTENSION + "kf")) ) {
        Vector v = new Vector();
        v.setSize(2);
        v.set(XMLBeans.INDEX_BEANINSTANCES, beans);
        v.set(XMLBeans.INDEX_BEANCONNECTIONS, 
            BeanConnection.getConnections(tabIndex));
        XStream.write(sFile.getAbsolutePath(), v);
      } /* XML? */ else if (sFile.getAbsolutePath().
          toLowerCase().endsWith(FILE_EXTENSION_XML)) {
        Vector v = new Vector();
        v.setSize(2);
        v.set(XMLBeans.INDEX_BEANINSTANCES, beans);
        v.set(XMLBeans.INDEX_BEANCONNECTIONS, 
            BeanConnection.getConnections(tabIndex));
        XMLBeans xml = new XMLBeans(m_beanLayout, m_bcSupport, tabIndex); 
        xml.write(sFile, v);
      } /* binary */ else {
        OutputStream os = new FileOutputStream(sFile);
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(beans);
        oos.writeObject(BeanConnection.getConnections(tabIndex));
        oos.flush();
        oos.close();
      } 
    } catch (Exception ex) {
        m_logPanel.statusMessage("[KnowledgeFlow]|Unable to save flow (see log).");
        m_logPanel.logMessage("[KnowledgeFlow] Unable to save flow ("
            + ex.getMessage() + ").");
        ex.printStackTrace();
        return false;
      } finally {
        // restore this panel as a property change listener in the beans
        for (int i = 0; i < beans.size(); i++) {
          BeanInstance tempB = (BeanInstance)beans.elementAt(i);
          if (tempB.getBean() instanceof Visible) {
            ((Visible)(tempB.getBean())).getVisual().
            addPropertyChangeListener(this);

            if (tempB.getBean() instanceof MetaBean) {
              ((MetaBean)tempB.getBean()).
              addPropertyChangeListenersSubFlow(this);
            }
            // Restore the default background colour
            ((Visible)(tempB.getBean())).getVisual().
            setBackground(bckC);
            ((JComponent)(tempB.getBean())).setBackground(bckC);
          }
        }
        
        if (!isUndoPoint) {
          Environment e = m_mainKFPerspective.getEnvironmentSettings();
          e.addVariable("Internal.knowledgeflow.directory", sFile.getParent());
          m_mainKFPerspective.setEditedStatus(tabIndex, false);
        }
      }
      return true;
    }

  /**
   * Serialize the layout to a file
   */
  private void saveLayout(int tabIndex, boolean showDialog) {
    //    m_loadB.setEnabled(false);
    //    m_saveB.setEnabled(false);
    java.awt.Color bckC = getBackground();
    
    File sFile = m_mainKFPerspective.getFlowFile(tabIndex);
    int returnVal = JFileChooser.APPROVE_OPTION;
    
    if (showDialog || sFile.getName().equals("-NONE-")) {
      returnVal = m_FileChooser.showSaveDialog(this);
    }
    
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      // temporarily remove this panel as a property changle listener from
      // each bean

      Vector beans = 
        BeanInstance.getBeanInstances(tabIndex);
      detachFromLayout(beans);

      // determine filename
      sFile = m_FileChooser.getSelectedFile();

      // add extension if necessary
      if (m_FileChooser.getFileFilter() == m_KfFilter) {
        if (!sFile.getName().toLowerCase().endsWith(FILE_EXTENSION)) {
          sFile = new File(sFile.getParent(), 
                           sFile.getName() + FILE_EXTENSION);
        }
      } else if (m_FileChooser.getFileFilter() == m_KOMLFilter) {
        if (!sFile.getName().toLowerCase().endsWith(KOML.FILE_EXTENSION + "kf")) {
          sFile = new File(sFile.getParent(), 
                           sFile.getName() + KOML.FILE_EXTENSION + "kf");
        }
      } else if (m_FileChooser.getFileFilter() == m_XStreamFilter) {
        if (!sFile.getName().toLowerCase().endsWith(XStream.FILE_EXTENSION + "kf")) {
          sFile = new File(sFile.getParent(), 
                           sFile.getName() + XStream.FILE_EXTENSION + "kf");
        }
      } else if (m_FileChooser.getFileFilter() == m_XMLFilter) {
        if (!sFile.getName().toLowerCase().endsWith(FILE_EXTENSION_XML)) {
          sFile = new File(sFile.getParent(), 
                           sFile.getName() + FILE_EXTENSION_XML);
        }
      }
      
      saveLayout(sFile, m_mainKFPerspective.getCurrentTabIndex(), false);
    } 
  }

  /**
   * Save the knowledge flow into the OutputStream passed at input. Only
   * supports saving the layout data (no trained models) to XML.
   *
   * @param out		the output stream to save the layout in
   */
  public void saveLayout(OutputStream out, int tabIndex) {
    // temporarily remove this panel as a property changle listener from
    // each bean
    Vector beans = BeanInstance.getBeanInstances(tabIndex);

    for (int i = 0; i < beans.size(); i++) {
      BeanInstance tempB = (BeanInstance) beans.elementAt(i);

      if (tempB.getBean() instanceof Visible) {
        ((Visible) (tempB.getBean())).getVisual()
         .removePropertyChangeListener(this);

        if (tempB.getBean() instanceof MetaBean) {
          ((MetaBean) tempB.getBean()).removePropertyChangeListenersSubFlow(this);
        }
      }
    }

    // now serialize components vector and connections vector
    try {
      Vector v = new Vector();
      v.setSize(2);
      v.set(XMLBeans.INDEX_BEANINSTANCES, beans);
      v.set(XMLBeans.INDEX_BEANCONNECTIONS, 
          BeanConnection.getConnections(tabIndex));

      XMLBeans xml = new XMLBeans(m_beanLayout, m_bcSupport, tabIndex);
      xml.write(out, v);
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      // restore this panel as a property change listener in the beans
      for (int i = 0; i < beans.size(); i++) {
        BeanInstance tempB = (BeanInstance) beans.elementAt(i);

        if (tempB.getBean() instanceof Visible) {
          ((Visible) (tempB.getBean())).getVisual()
           .addPropertyChangeListener(this);

          if (tempB.getBean() instanceof MetaBean) {
            ((MetaBean) tempB.getBean()).addPropertyChangeListenersSubFlow(this);
          }
        }
      }
    }
  }

  private void loadUserComponents() {
    Vector tempV = null;
    String ext = "";
    if (m_UserComponentsInXML)
      ext = USERCOMPONENTS_XML_EXTENSION;
    File sFile = 
      new File(System.getProperty("user.home")
               +File.separator + ".knowledgeFlow"
               +File.separator + "userComponents"
               +ext);
    if (sFile.exists()) {
      try {
        if (m_UserComponentsInXML) {
          XMLBeans xml = new XMLBeans(m_beanLayout, m_bcSupport, XMLBeans.DATATYPE_USERCOMPONENTS);
          tempV = (Vector) xml.read(sFile);
        }
        else {
          InputStream is = new FileInputStream(sFile);
          ObjectInputStream ois = new ObjectInputStream(is);
          tempV = (Vector)ois.readObject();
          ois.close();
        }
      } catch (Exception ex) {
        System.err.println("[KnowledgeFlow] Problem reading user components.");
        ex.printStackTrace();
        return;
      }
      if (tempV.size() > 0) {
        // create the user tab and add the components
        for (int i = 0; i < tempV.size(); i++) {
          MetaBean tempB = (MetaBean)tempV.elementAt(i);
          //addToUserToolBar(tempB, false);
          addToUserTreeNode(tempB, false);
        }
      }
    }
  }

  private void installWindowListenerForSavingUserBeans() {
    ((java.awt.Window)getTopLevelAncestor()).
      addWindowListener(new java.awt.event.WindowAdapter() {
          public void windowClosing(java.awt.event.WindowEvent e) {
            System.out.println("[KnowledgeFlow] Saving user components....");
            File sFile = 
              new File(System.getProperty("user.home")
                       +File.separator+".knowledgeFlow");
            if (!sFile.exists()) {
              if (!sFile.mkdir()) {
                System.err.println("[KnowledgeFlow] Unable to create .knowledgeFlow "
                                   +"directory in your HOME.");
              } else {
                // make the plugins subdirectory for the user
                sFile = new File(sFile.toString() + File.separator 
                    + "plugins");
                sFile.mkdir();
              }
            }
            try {
              String ext = "";
              if (m_UserComponentsInXML)
                ext = USERCOMPONENTS_XML_EXTENSION;
              File sFile2 = new File(sFile.getAbsolutePath()
                                     +File.separator
                                     +"userComponents"
                                     +ext);
                
              if (m_UserComponentsInXML) {
                XMLBeans xml = new XMLBeans(m_beanLayout, m_bcSupport, XMLBeans.DATATYPE_USERCOMPONENTS);
                xml.write(sFile2, m_userComponents);
              }
              else {
                OutputStream os = new FileOutputStream(sFile2);
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(m_userComponents);
                oos.flush();
                oos.close();
              }
            } catch (Exception ex) {
              System.err.println("[KnowledgeFlow] Unable to save user components");
              ex.printStackTrace();
            } 

          }
        });
  }
  
  /**
   * Utility method for grabbing the global info help (if it exists) from
   * an arbitrary object
   *
   * @param tempBean the object to grab global info from 
   * @return the global help info or null if global info does not exist
   */
  public static String getGlobalInfo(Object tempBean) {
    // set tool tip text from global info if supplied
    String gi = null;
    try {
      BeanInfo bi = Introspector.getBeanInfo(tempBean.getClass());
      MethodDescriptor [] methods = bi.getMethodDescriptors();
      for (int i = 0; i < methods.length; i++) {
	String name = methods[i].getDisplayName();
	Method meth = methods[i].getMethod();
	if (name.equals("globalInfo")) {
	  if (meth.getReturnType().equals(String.class)) {
	    Object args[] = { };
	    String globalInfo = (String)(meth.invoke(tempBean, args));
	    gi = globalInfo;
	    break;
	  }
	}
      }
    } catch (Exception ex) {
      
    }
    return gi;
  }

  /** variable for the KnowLedgeFlow class which would be set to null by the 
      memory monitoring thread to free up some memory if we running out of 
      memory.
   */
  private static KnowledgeFlowApp m_knowledgeFlow;

  /** for monitoring the Memory consumption */
  private static Memory m_Memory = new Memory(true);

  // list of things to be notified when the startup process of
  // the KnowledgeFlow is complete
  public static Vector s_startupListeners = new Vector();

  // modifications by Zerbetto
  // If showFileMenu is true, the file menu (open file, new file, save file buttons) is showed
  private boolean m_showFileMenu = true;
  
  /**
   * Create the singleton instance of the KnowledgeFlow
   * @param args can contain a file argument for loading a flow layout 
   * (format: "file=[path to layout file]")
   * Modified by Zerbetto: you can specify the path of a knowledge flow layout file at input
   */
  public static void createSingleton(String[] args) {
    //modifications by Zerbetto 05-12-2007
    String fileName = null;
    boolean showFileMenu = true;

    if ((args != null) && (args.length > 0)) {
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];

        if (arg.startsWith("file=")) {
          fileName = arg.substring("file=".length());
        } else if (arg.startsWith("showFileMenu=")) {
          showFileMenu = Boolean.parseBoolean(arg.substring(
                "showFileMenu=".length()));
        }
      }
    }

    if (m_knowledgeFlow == null) {
      m_knowledgeFlow = new KnowledgeFlowApp(showFileMenu);
    }

    // end modifications by Zerbetto

    // notify listeners (if any)
    for (int i = 0; i < s_startupListeners.size(); i++) {
      ((StartUpListener) s_startupListeners.elementAt(i)).startUpComplete();
    }

    //modifications by Zerbetto 05-12-2007
    if (fileName != null) {
      m_knowledgeFlow.loadInitialLayout(fileName);
    }

    // end modifications 
  }
  
  public static void disposeSingleton() {
    m_knowledgeFlow = null;
  }

  /**
   * Return the singleton instance of the KnowledgeFlow
   *
   * @return the singleton instance
   */
  public static KnowledgeFlowApp getSingleton() {
    return m_knowledgeFlow;
  }

  /**
   * Add a listener to be notified when startup is complete
   * 
   * @param s a listener to add
   */
  public static void addStartupListener(StartUpListener s) {
    s_startupListeners.add(s);
  }

  /**
   * Loads the specified file at input
   *
   * Added by Zerbetto
   */
  //modifications by Zerbetto 05-12-2007
  private void loadInitialLayout(String fileName) {
    File oFile = new File(fileName);

    if (oFile.exists() && oFile.isFile()) {
      m_FileChooser.setSelectedFile(oFile);

      int index = fileName.lastIndexOf('.');

      if (index != -1) {
        String extension = fileName.substring(index);

        if (FILE_EXTENSION_XML.equalsIgnoreCase(extension)) {
          m_FileChooser.setFileFilter(m_knowledgeFlow.m_XMLFilter);
        } else if (FILE_EXTENSION.equalsIgnoreCase(extension)) {
          m_FileChooser.setFileFilter(m_knowledgeFlow.m_KfFilter);
        }
      }
    } else {
      System.err.println("[KnowledgeFlow] File '" + fileName + "' does not exists.");
    }

    try {
      Vector beans = new Vector();
      Vector connections = new Vector();

      // KOML?
      if ((KOML.isPresent()) &&
            (oFile.getAbsolutePath().toLowerCase().endsWith(KOML.FILE_EXTENSION))) {
        Vector v = (Vector) KOML.read(oFile.getAbsolutePath());
        beans = (Vector) v.get(XMLBeans.INDEX_BEANINSTANCES);
        connections = (Vector) v.get(XMLBeans.INDEX_BEANCONNECTIONS);
      } /* XML? */ else if (oFile.getAbsolutePath().toLowerCase()
                                     .endsWith(FILE_EXTENSION_XML)) {
        XMLBeans xml = new XMLBeans(m_beanLayout, m_bcSupport, 
            m_mainKFPerspective.getCurrentTabIndex());
        Vector v = (Vector) xml.read(oFile);
        beans = (Vector) v.get(XMLBeans.INDEX_BEANINSTANCES);
        connections = (Vector) v.get(XMLBeans.INDEX_BEANCONNECTIONS);

        //connections  = new Vector();
      } /* binary */ else {
        InputStream is = new FileInputStream(oFile);
        ObjectInputStream ois = new ObjectInputStream(is);
        beans = (Vector) ois.readObject();
        connections = (Vector) ois.readObject();
        ois.close();
      }

      java.awt.Color bckC = getBackground();
      m_bcSupport = new BeanContextSupport();
      m_bcSupport.setDesignTime(true);

      // register this panel as a property change listener with each
      // bean
      for (int i = 0; i < beans.size(); i++) {
        BeanInstance tempB = (BeanInstance) beans.elementAt(i);

        if (tempB.getBean() instanceof Visible) {
          ((Visible) (tempB.getBean())).getVisual()
           .addPropertyChangeListener(this);

          // A workaround to account for JPanel's with their default
          // background colour not being serializable in Apple's JRE
          ((Visible) (tempB.getBean())).getVisual().setBackground(bckC);
          ((JComponent) (tempB.getBean())).setBackground(bckC);
        }

        if (tempB.getBean() instanceof BeanCommon) {
          ((BeanCommon) (tempB.getBean())).setLog(m_logPanel);
        }

        if (tempB.getBean() instanceof BeanContextChild) {
          m_bcSupport.add(tempB.getBean());
        }
      }

      BeanInstance.setBeanInstances(beans, m_beanLayout, 
          m_mainKFPerspective.getCurrentTabIndex());
      BeanConnection.setConnections(connections, 
          m_mainKFPerspective.getCurrentTabIndex());
      m_beanLayout.revalidate();
      m_beanLayout.repaint();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  
  public void setAllowMultipleTabs(boolean multiple) {
    m_allowMultipleTabs = multiple;
    
    if (!multiple) {
      m_newB.setEnabled(false);
    }
  }
  
  public boolean getAllowMultipleTabs() {
    return m_allowMultipleTabs;
  }

  //end modifications

  /**
   * Notifies to the parent swt that the layout is dirty
   *
   * Added by Zerbetto
   */
  private void notifyIsDirty() {
    //this.firePropertyChange(new Integer(IEditorPart.PROP_DIRTY).toString(), null, null);
    this.firePropertyChange("PROP_DIRTY", null, null);
  }

  /**
   * Main method.
   *
   * @param args a <code>String[]</code> value
   */
  public static void main(String [] args) {

    LookAndFeel.setLookAndFeel();
    
    try {
      // uncomment to disable the memory management:
      //m_Memory.setEnabled(false);

      final javax.swing.JFrame jf = new javax.swing.JFrame();
      jf.getContentPane().setLayout(new java.awt.BorderLayout());
      //final KnowledgeFlowApp tm = new KnowledgeFlowApp();
      m_knowledgeFlow = new KnowledgeFlowApp(true);

      jf.getContentPane().add(m_knowledgeFlow, java.awt.BorderLayout.CENTER);
      jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      jf.setSize(1024,768);
      jf.setVisible(true);     

      
      Thread memMonitor = new Thread() {
        public void run() {
          while(true) {
            try {
              //System.out.println("Before sleeping");
              this.sleep(4000);
              
              System.gc();

              if (m_Memory.isOutOfMemory()) {
                // clean up
                jf.dispose();
                m_knowledgeFlow = null;
                System.gc();

                // stop threads
                m_Memory.stopThreads();
               
                // display error
                System.err.println("\n[KnowledgeFlow] displayed message:");
                m_Memory.showOutOfMemory();
                System.err.println("\nexiting");
                System.exit(-1);
              }

            } catch(InterruptedException ex) { ex.printStackTrace(); }
          }
        }
      };

      memMonitor.setPriority(Thread.NORM_PRIORITY);
      memMonitor.start();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
