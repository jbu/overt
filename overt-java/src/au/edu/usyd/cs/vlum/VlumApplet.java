package au.edu.usyd.cs.vlum;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;
import java.applet.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import javax.swing.*;
import javax.swing.event.*;

import java.rmi.*;
//import netscape.javascript.*;
import java.util.regex.*;

public class VlumApplet extends JApplet implements ItemListener, ChangeListener, ActionListener {
  private JSlider markSepSlider;
  private JLabel viewLabel;
  private JLabel markSepLabel;
  private JMenuItem backMenuItem;
  private JMenuItem resetMenuItem;
  private JMenuItem mkMenuItem;
  private java.util.List<String> history;
  private int markSepValue;
  //private Hashtable minmaxes;
  private String currentTag;
  private String currentType;
  private String helpUrl;
  //private Map generalSetup;
  private SquidgePane squidgePane;
  //private Hashtable[] topics;
  private Setup setup;
  private Questions questions;
  private GraphModel graphModel;
  private int currentDisplayIndex;
  //private JSObject thisWin;
  private String baseUrl = "";
  private String initialTitle = "";
  
  //debug stuff
  private static  boolean DEBUG = false;
  private JCheckBoxMenuItem dragableCheckBox = null;
  private JCheckBoxMenuItem antiAliasCheckBox = null;
  private JCheckBoxMenuItem drawAlphaCheckBox = null;
  private JCheckBoxMenuItem animateCheckBox = null;

  private static final boolean inBrowser = false;

  public void init() {
    baseUrl = this.getCodeBase().toString();
    if (baseUrl.endsWith(".html"))
        baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/')+1);
    emit("baseUrl "+baseUrl);

    String questionsUrl = getParameter("questionsUrl");
    emit("questionsUrl "+questionsUrl);
    questions = null;
    if (inBrowser && questionsUrl != null)
      questions = new Questions(baseUrl+questionsUrl);

   /* thisWin = null;
    if (inBrowser) {
      try {
        thisWin = (JSObject) JSObject.getWindow(this);
      } catch (Exception ex) {}
    }*/
    JPanel contentPane = new JPanel();
    contentPane.setBackground(Color.white);
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    setContentPane(contentPane);

    history = new ArrayList<String>();
  
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    JMenuItem menuItem;

    DEBUG = "true".equals(getParameter("debug"));

    initialTitle = getParameter("initialTitle");
    emit("initialTitle "+initialTitle);

    String setupUrl = baseUrl + getParameter("setupUrl");
    emit("setupUrl "+setupUrl);
    setup = new Setup(setupUrl);

    helpUrl = baseUrl + getParameter("helpUrl");
    emit("helpUrl "+helpUrl);

    
    JMenu displayMenu = new JMenu("Display");
    menuBar.add(displayMenu);
    for (int i = 0; i < setup.displaysLength(); i++) {
      if (setup.isViewable(i)) {
        menuItem = new JMenuItem(setup.getNodeTag(i));
        menuItem.addActionListener(this);
        displayMenu.add(menuItem);
      }
    }
    if (setup.displaysLength() <= 1) {
      displayMenu.setEnabled(false);
    }
      
    JMenu menu = new JMenu("Action");
    menuBar.add(menu);

    backMenuItem = new JMenuItem("Go Back One");
    backMenuItem.addActionListener(this);
    backMenuItem.setEnabled(false);
    menu.add(backMenuItem);

    menuItem = new JMenuItem("View Selected");
    menuItem.addActionListener(this);
    menu.add(menuItem);

    menuItem = new JMenuItem("Search Titles");
    menuItem.addActionListener(this);
    menu.add(menuItem);
    menuItem = new JMenuItem("View Evidence");
    menuItem.addActionListener(this);
    menu.add(menuItem);
    //menuItem = new JMenuItem("Try Questions");
    //menuItem.addActionListener(this);
    //menu.add(menuItem);

    if (questions == null) {
      menu.addSeparator();

      resetMenuItem = new JMenuItem("* Reset *");
      resetMenuItem.addActionListener(this);
      menu.add(resetMenuItem);

      menu.addSeparator();

      menuItem = new JMenuItem("100 Data Set");
      menuItem.addActionListener(this);
      menu.add(menuItem);
      menuItem = new JMenuItem("300 Data Set");
      menuItem.addActionListener(this);
      menu.add(menuItem);
      menuItem = new JMenuItem("500 Data Set");
      menuItem.addActionListener(this);
      menu.add(menuItem);
      menuItem = new JMenuItem("700 Data Set");
      menuItem.addActionListener(this);
      menu.add(menuItem);
    }

    if (DEBUG) {
      menu = new JMenu("Draw");
      menuBar.add(menu);
      dragableCheckBox = new JCheckBoxMenuItem("Drag");
      dragableCheckBox.addItemListener(this);
      menu.add(dragableCheckBox);
      antiAliasCheckBox = new JCheckBoxMenuItem("AntiAlias");
      antiAliasCheckBox.addItemListener(this);
      menu.add(antiAliasCheckBox);
      drawAlphaCheckBox = new JCheckBoxMenuItem("Alpha");
      drawAlphaCheckBox.addItemListener(this);
      menu.add(drawAlphaCheckBox);
      animateCheckBox = new JCheckBoxMenuItem("Animate");
      animateCheckBox.setSelected(true);
      animateCheckBox.addItemListener(this);
      menu.add(animateCheckBox);
    }

    mkMenuItem = new JMenuItem("Help");
    mkMenuItem.addActionListener(this);
    mkMenuItem.setEnabled(true);
    menuBar.add(mkMenuItem);
    
    this.repaint();

    // pane
    JPanel controlPane = new JPanel();
    controlPane.setLayout(new BoxLayout(controlPane, BoxLayout.X_AXIS));
    JPanel labelPane = new JPanel();

    labelPane.setLayout(new BoxLayout(labelPane, BoxLayout.Y_AXIS));
    labelPane.setMinimumSize( new Dimension( 180, 30 ));

    contentPane.add(controlPane);

    squidgePane = new SquidgePane();
    squidgePane.setAnimate(true);
    squidgePane.setDisplayIndex(currentDisplayIndex);
    
    markSepValue = 50;
    squidgePane.addActionListener(this);
        
    markSepSlider = new JSlider(0,100);
    markSepSlider.addChangeListener(this);
    markSepLabel = new JLabel();

    controlPane.add(markSepSlider);

    Dimension minSize = new Dimension(5, 20);
    Dimension prefSize = new Dimension(5, 20);
    Dimension maxSize = new Dimension(10, 20);

    controlPane.add(new Box.Filler(minSize, prefSize, maxSize));

    controlPane.add(labelPane);

    viewLabel = new JLabel();
    
    labelPane.add(viewLabel);
    labelPane.add(markSepLabel);

    contentPane.add(squidgePane);
    this.repaint();

    String userUrl = baseUrl + getParameter("userUrl");
    emit("userUrl "+userUrl);
    loadDataSet(userUrl);

    //if ( Math.random() < 0.5f ) setDraggable(true);
    dragOn();

    showHelp();



  }

  private void loadDataSet(String url) {
    try {
      graphModel = new GraphModel(setup, url, this);
    } catch (Exception ex) {
    }
    squidgePane.setGraphModel(graphModel);
    int cm;
    if (initialTitle == null)
      cm = graphModel.getMinimumMarkPosition(currentDisplayIndex);
    else
      cm = graphModel.getTitleIndex(initialTitle);
    history = new ArrayList<String>();
    history.add(""+cm);
    squidgePane.setSelectedTopic(cm);
    this.changeToView(0);
  }

  public void itemStateChanged (ItemEvent e) {
    if (e.getItemSelectable() == dragableCheckBox) 
      squidgePane.setDragable(e.getStateChange() == ItemEvent.SELECTED);
    if (e.getItemSelectable() == antiAliasCheckBox) 
      squidgePane.setAntiAlias(e.getStateChange() == ItemEvent.SELECTED);
    if (e.getItemSelectable() == drawAlphaCheckBox) 
      squidgePane.setAlphaTransparency(e.getStateChange() == ItemEvent.SELECTED);
    if (e.getItemSelectable() == animateCheckBox) 
      squidgePane.setAnimate(e.getStateChange() == ItemEvent.SELECTED);
  }

  private void setSepLabel() {
    int val = markSepValue;
    if (setup.isComparison(currentDisplayIndex)) {
      this.markSepLabel.setText("Standard is "+((val >= 50)?(((val-50)*2)+"% Above Average"):(((50-val)*2)+"% Below Average")));
    } else {
      this.markSepLabel.setText("Standard is "+val+"%");
    }
  }

  public void stateChanged(ChangeEvent e) {
    JSlider source = (JSlider)e.getSource();
    int val = (int)source.getValue();
      if (val == markSepValue) 
        return;
    markSepValue = val;
    setSepLabel();
    //if (!source.getValueIsAdjusting()) {
      squidgePane.setMarkSeparationValue(val/100f);
    //}
  }

  private static void emit (String s) {
    System.out.println(s);
  }

  private void changeToView(int index) {
    
    currentDisplayIndex = index;
    markSepValue = 50;
    markSepSlider.setValue(markSepValue);
    setSepLabel();
    squidgePane.changeDisplay(currentDisplayIndex);

    viewLabel.setToolTipText(setup.getNodeToolTip(currentDisplayIndex));
    viewLabel.setText("Viewing "+setup.getNodeTag(currentDisplayIndex));

    if (setup.isAdjustable(currentDisplayIndex)) {
      markSepSlider.setEnabled(true);
      markSepLabel.setVisible(true);
    } else {
      markSepSlider.setEnabled(false);
      markSepLabel.setVisible(false);
    }
  }
  
  public void actionPerformed (ActionEvent e) {
    //System.out.println("event e: "+e.toString());
    Object source = e.getSource();
    String t = null;
    try {
      if (source.getClass() == Class.forName("javax.swing.JMenuItem"))
        t = ((JMenuItem)source).getText();
      else
        t = e.getActionCommand();
    } catch (ClassNotFoundException ex) {
      t = e.getActionCommand();
    }
    //System.out.println("action on "+t);
    AppletContext cntx = getAppletContext();
    if (t.equals("Search Titles")) {
      String regex = JOptionPane.showInputDialog(this, "Search String?");
      this.squidgePane.searchFor(regex);
    } else if (t.equals("View Evidence")) {
      try {
        cntx.showDocument(new URL(baseUrl+"evidence.html"),"topicframe");
      } catch (Exception ex) {
        //System.out.println("bad url: "+ex.toString());
      }
    } else if (t.equals("Try Questions")) {
      try {
        cntx.showDocument(new URL("http://www.gmp.usyd.edu.au/servlets/OnlineAssessmentServlet/doQuestion?topicId="+squidgePane.getSelectedId()),"topicframe");
      } catch (Exception ex) {
        //System.out.println("bad url: "+ex.toString());
      }
    } else if (t.equals("View Selected")) {
      try {
        String url = squidgePane.getSelectedDescription();
        //System.out.println("going to url: "+url);
        if ( url.startsWith("http://www.imdb"))
          cntx.showDocument(new URL(url),"topicframe");
        else
          cntx.showDocument(new URL(baseUrl+"randomTopic.html"),"topicframe");
      } catch (Exception ex) {
        //System.out.println("bad url: "+ex.toString());
      }
    } else if (t.length() >= 21 && t.startsWith("SquidgePaneShowString")){
      cntx.showStatus(t.substring(21,t.length()));
    } else if (t.length() >= 17 && t.startsWith("SquidgePaneClick:")){
      history.add(t.substring(17,t.length()));
      if (history.size() > 1) backMenuItem.setEnabled(true);
    } else if (t.equals("Go Back One")){
      if (history.size() > 1) {
        int index = Integer.parseInt(history.get(history.size()-2));
        history.remove(history.size()-1);
        squidgePane.setSelectedTopic(index);
      }
      if (history.size() == 1) backMenuItem.setEnabled(false);
    } else if (t.equals("Help")){
      try {
        showHelp();
      } catch (Exception ex) {
        //System.out.println("bad url: "+ex.toString());
      }
    } else if (t.equals("* Reset *")) {
      if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null,
                 "Are you sure you want to reset?",
                 "reset check",
                 JOptionPane.YES_NO_OPTION,
                 JOptionPane.WARNING_MESSAGE)) {
        resetSquidge();
      }
    } else if (t.equals("Next Data Set")) {
      if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null,
                 "Are you sure you want to move on?",
                 "next data",
                 JOptionPane.YES_NO_OPTION,
                 JOptionPane.WARNING_MESSAGE)) 
        nextDataSet();
    } else if (t.equals("100 Data Set")) {
      loadDataSet(baseUrl + "gendata/movies100.rdf");
    } else if (t.equals("300 Data Set")) {
      loadDataSet(baseUrl + "gendata/movies300.rdf");
    } else if (t.equals("500 Data Set")) {
      loadDataSet(baseUrl + "gendata/movies500.rdf");
    } else if (t.equals("700 Data Set")) {
      loadDataSet(baseUrl + "gendata/movies700.rdf");
    } else {   // look for view change
      changeToView(setup.getViewTagIndex(t));
    }
    repaint();
  }

  public void showHelp() {
    try {
      getAppletContext().showDocument(new URL(helpUrl),"topicframe");
    } catch (Exception e) {
      emit("help problem: "+e.toString());
    }
  }

  public void nextDataSet() {
    //int r = (int) Math.floor(Math.random()*4f);
    int r = (int) Math.floor(Math.random()*3f);
    if ( r == 1 )
      //loadDataSet("file:///home/hemul/lib/html/sq2p/gendata/movies100.rdf");
      loadDataSet(baseUrl + "gendata/movies100.rdf");
    else if ( r == 2 )
      //loadDataSet("file:///home/hemul/lib/html/sq2p/gendata/movies300.rdf");
      loadDataSet(baseUrl+"gendata/movies300.rdf");
    else if ( r == 3 )
      //loadDataSet("file:///home/hemul/lib/html/sq2p/gendata/movies500.rdf");
      loadDataSet(baseUrl+"gendata/movies500.rdf");
    //else 
      //loadDataSet("file:///home/hemul/lib/html/sq2p/gendata/movies700.rdf");
      //loadDataSet(baseUrl+"gendata/movies700.rdf");
  }

  public void resetSquidge() {
    this.changeToView(0);
    int cm;
    if (initialTitle == null)
      cm = graphModel.getMinimumMarkPosition(currentDisplayIndex);
    else
      cm = graphModel.getTitleIndex(initialTitle);
    history = new ArrayList<String>();
    history.add(""+cm);
    squidgePane.setSelectedTopic(cm);
    backMenuItem.setEnabled(false);
  }


  public void selectItem(String s) {
    int cm = -1;
    try {
      cm = graphModel.getTitleIndex(s);
    } catch (Exception ex) {
      return;
    }
    if (cm != -1) 
      squidgePane.setSelectedTopic(cm);
  }

  public void dragOn() {
    setDraggable(true);
  }

  public void dragOff() {
    setDraggable(false);
  }

  public void setDraggable(boolean b) {
    squidgePane.setDragable(b);
  }

}

