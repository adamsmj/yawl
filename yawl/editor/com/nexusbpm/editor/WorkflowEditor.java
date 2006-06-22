package com.nexusbpm.editor;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.Log4jFactory;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;

import au.edu.qut.yawl.elements.YSpecification;
import au.edu.qut.yawl.persistence.dao.DAOFactory;
import au.edu.qut.yawl.persistence.dao.SpecificationDAO;
import au.edu.qut.yawl.persistence.managed.DataContext;
import au.edu.qut.yawl.persistence.managed.TestDataContext;

import com.nexusbpm.editor.desktop.DesktopPane;
import com.nexusbpm.editor.editors.net.GraphEditor;
import com.nexusbpm.editor.icon.ApplicationIcon;
import com.nexusbpm.editor.logger.CapselaLogPanel;
import com.nexusbpm.editor.persistence.EditorDataProxy;
import com.nexusbpm.editor.tree.DatasourceRoot;
import com.nexusbpm.editor.tree.STree;
import com.nexusbpm.editor.tree.SharedNode;
import com.nexusbpm.editor.tree.SharedNodeTreeModel;
import command.EditorCommand;

/**
 *
 * @author  SandozM
 */
public class WorkflowEditor extends javax.swing.JFrame {
    
	private final static int DEFAULT_CLIENT_WIDTH = 800;
	private final static int DEFAULT_CLIENT_HEIGHT = 692;

	private static WorkflowEditor singleton = null;
	
    /**
     * Creates new form WorkflowEditor 
     */
    private WorkflowEditor() {
		PropertyConfigurator.configure( WorkflowEditor.class.getResource( "client.logging.properties" ) );
    	initComponents();
        this.pack();
    	this.setSize(DEFAULT_CLIENT_WIDTH,DEFAULT_CLIENT_HEIGHT);
    }
    
    public static WorkflowEditor getInstance() {
    	if (WorkflowEditor.singleton == null) {
    		WorkflowEditor.singleton = new WorkflowEditor();
    	}
    	return singleton;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">                          

    private static String slashify(String path, boolean isDirectory) {
    	String p = path;
    	if (File.separatorChar != '/')
    	    p = p.replace(File.separatorChar, '/');
    	if (!p.startsWith("/"))
    	    p = "/" + p;
    	if (!p.endsWith("/") && isDirectory)
    	    p = p + "/";
    	return p;
        }
    public static String toURIString(String s, boolean isDirectory) {
    	try {
    	    String sp = slashify(s, isDirectory);
    	    if (sp.startsWith("//"))
    		sp = "//" + sp;
    	    return new URI("file", null, sp, null).toString();
    	} catch (URISyntaxException x) {
    	    throw new Error(x);		// Can't happen
    	}
        }

    
    private void initComponents() {
	    JFrame.setDefaultLookAndFeelDecorated(true);
		try {
	        UIManager.setLookAndFeel(
	            UIManager.getSystemLookAndFeelClassName());
	    } catch (Exception e) { e.printStackTrace();}
        componentEditorSplitPane = new javax.swing.JSplitPane();
        componentList1Panel = new javax.swing.JPanel();
        componentList2Panel = new javax.swing.JPanel();
        componentTreesSplitPane = new JSplitPane();
        componentTreesPanel = new javax.swing.JPanel();
        componentList1ScrollPane = new javax.swing.JScrollPane();
        componentList2ScrollPane = new javax.swing.JScrollPane();
        SpecificationDAO memdao = DAOFactory.getDAOFactory(DAOFactory.Type.MEMORY).getSpecificationModelDAO();
        SpecificationDAO filedao = DAOFactory.getDAOFactory(DAOFactory.Type.FILE).getSpecificationModelDAO();
        DataContext memdc = new DataContext(memdao, EditorDataProxy.class);
        DataContext filedc = new DataContext(filedao, EditorDataProxy.class);
        EditorDataProxy memdp = (EditorDataProxy) memdc.getDataProxy(new DatasourceRoot("virtual://memory/home/sandozm/templates/testing/"), null);
        String dataroot = new File("exampleSpecs/").toURI().toString();
        EditorDataProxy filedp = (EditorDataProxy) filedc.getDataProxy(new DatasourceRoot(dataroot), null);
       
        EditorDataProxy dprox = (EditorDataProxy) memdc.getChildren(memdp).toArray()[0];
        YSpecification spec = null; 
//superseded by copy command
//        try {        
//        	spec = (YSpecification) ((YSpecification) dprox.getData()).clone();
//            System.out.println("uri " + spec.getID());
//            URI uri = new URI(spec.getID());
//            System.out.println("uri to file " + new File(new URI(spec.getID())).getAbsolutePath());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		EditorDataProxy newOne = (EditorDataProxy) filedc.getDataProxy(spec, null);
//		URI desturi = null;
//		try {
//		desturi = TestDataContext.moveUri(new URI(spec.getID()), new URI(dataroot));
//		System.out.println("desturi " + desturi);
//		spec.setID(desturi.toString());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		filedc.put(newOne); 
        EditorCommand.executeCopyCommand(dprox, filedp);
        SharedNode root1 = new SharedNode(memdp);
        SharedNode root2 = new SharedNode(filedp);

        SharedNodeTreeModel treeModel1 = new SharedNodeTreeModel(root1);
        SharedNodeTreeModel treeModel2 = new SharedNodeTreeModel(root2);
        root1.setTreeModel(treeModel1);
        root2.setTreeModel(treeModel2);
        componentList1Tree = new STree(treeModel1, componentList1ScrollPane);
        componentList1Tree.setShowsRootHandles(false);
        componentList1Tree.setRootVisible(true);
        componentList1Tree.setRowHeight(34);
        
        componentList2Tree = new STree(treeModel2, componentList1ScrollPane);
        componentList2Tree.setShowsRootHandles(false);
        componentList2Tree.setRootVisible(true);
        componentList2Tree.setRowHeight(34);
        
        desktopAndStatusPanel = new javax.swing.JPanel();
        desktopLogSplitPane = new javax.swing.JSplitPane();
        desktopPanel = new javax.swing.JPanel();
        desktopScrollPane = new javax.swing.JScrollPane();
        desktopPane = new DesktopPane();
        logPanel = new CapselaLogPanel();
        logTextScrollPane = new javax.swing.JScrollPane();
        logTextArea = new javax.swing.JTextArea();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        cutMenuItem = new javax.swing.JMenuItem();
        copyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        contentsMenuItem = new javax.swing.JMenuItem();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Nexus/BPM Process Editor");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setForeground(java.awt.Color.lightGray);
        setName("Nexus/BPM Process Editor");
        this.setIconImage(ApplicationIcon.getIcon("NexusFrame.window_icon", ApplicationIcon.LARGE_SIZE).getImage());
        componentEditorSplitPane.setDividerLocation(200);
        componentList1Panel.setLayout(new java.awt.BorderLayout());
        componentList2Panel.setLayout(new java.awt.BorderLayout());
        componentTreesPanel.setLayout(new java.awt.BorderLayout());

        componentList1ScrollPane.setViewportView(componentList1Tree);

        componentList2ScrollPane.setViewportView(componentList2Tree);

        componentList1Panel.add(componentList1ScrollPane, java.awt.BorderLayout.CENTER);
        componentList2Panel.add(componentList2ScrollPane, java.awt.BorderLayout.CENTER);

        componentEditorSplitPane.setLeftComponent(componentTreesPanel);
        componentTreesSplitPane.setDividerLocation(300);
        componentTreesSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        componentTreesSplitPane.setTopComponent(componentList1Panel);
        componentTreesSplitPane.setBottomComponent(componentList2Panel);
        componentTreesPanel.add(componentTreesSplitPane);

        desktopAndStatusPanel.setLayout(new java.awt.GridLayout(1, 0));

        desktopLogSplitPane.setDividerLocation(480);
        desktopLogSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        desktopLogSplitPane.setRequestFocusEnabled(false);
        desktopPanel.setLayout(new java.awt.GridLayout(1, 0));

        desktopPane.setBackground(new java.awt.Color(135, 145, 161));
        desktopScrollPane.setViewportView(desktopPane);

        desktopPanel.add(desktopScrollPane);

        desktopLogSplitPane.setTopComponent(desktopPanel);

        desktopLogSplitPane.setBottomComponent(logPanel);

        desktopAndStatusPanel.add(desktopLogSplitPane);

        componentEditorSplitPane.setRightComponent(desktopAndStatusPanel);

        getContentPane().add(componentEditorSplitPane, java.awt.BorderLayout.CENTER);

        fileMenu.setText("File");
        openMenuItem.setText("Open");
        fileMenu.add(openMenuItem);

        saveMenuItem.setText("Save");
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setText("Save As ...");
        fileMenu.add(saveAsMenuItem);

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText("Edit");
        cutMenuItem.setText("Cut");
        editMenu.add(cutMenuItem);

        copyMenuItem.setText("Copy");
        editMenu.add(copyMenuItem);

        pasteMenuItem.setText("Paste");
        editMenu.add(pasteMenuItem);

        deleteMenuItem.setText("Delete");
        editMenu.add(deleteMenuItem);

        menuBar.add(editMenu);

        helpMenu.setText("Help");
        contentsMenuItem.setText("Contents");
        helpMenu.add(contentsMenuItem);

        aboutMenuItem.setText("About");
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        pack();
    }
    // </editor-fold>                        

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                             
        System.exit(0);
    }                                            
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
    public void run() {
                WorkflowEditor.getInstance().setVisible(true);
            }
        });
    }
    
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JSplitPane componentEditorSplitPane;
    private javax.swing.JPanel componentList1Panel;
    private javax.swing.JPanel componentList2Panel;
    private javax.swing.JSplitPane componentTreesSplitPane;
    private javax.swing.JPanel componentTreesPanel;
    private javax.swing.JScrollPane componentList1ScrollPane;
    private javax.swing.JScrollPane componentList2ScrollPane;
    private STree componentList1Tree;
    private STree componentList2Tree;
    private javax.swing.JMenuItem contentsMenuItem;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JMenuItem cutMenuItem;
    private javax.swing.JMenuItem deleteMenuItem;
    private javax.swing.JPanel desktopAndStatusPanel;
    private javax.swing.JSplitPane desktopLogSplitPane;
    private javax.swing.JDesktopPane desktopPane;
    private javax.swing.JPanel desktopPanel;
    private javax.swing.JScrollPane desktopScrollPane;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private CapselaLogPanel logPanel;
    private javax.swing.JTextArea logTextArea;
    private javax.swing.JScrollPane logTextScrollPane;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;

	public javax.swing.JDesktopPane getDesktopPane() {
		return desktopPane;
	}

}
