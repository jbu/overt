package au.edu.usyd.cs.vlum;
import org.w3c.dom.*;
import javax.xml.parsers.*;

public class Setup {

  Document setupDocument;
  String [] displayIds;

  public Setup (String uri) {
    try {
      DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      setupDocument = parser.parse(uri);
  
      int c = displaysLength();
      //emit("len "+c);
      displayIds = new String[c];
      for (int i = 0; i < c; i++) {
        String a = getAttrText(i, "ID");
        if (a != null) {
          displayIds[i] = a;
        } else
          displayIds[i] = null;
      }
    } catch (Exception ex) { emit("setup init problem: "+ex.toString()); }
  }

  private int getId (String s) {
    for (int i = 0; i < displayIds.length; i++) {
      if (s.equals(displayIds[i])) return i;
    }
    return -1;
  }

  public int displaysLength() {
    try {
      NodeList l = setupDocument.getDocumentElement().getElementsByTagName("display");
      return l.getLength();
    } catch (Exception ex) {}
    return 0;
  }

  public int getViewTagIndex(String s) {
    try {
      NodeList l = setupDocument.getDocumentElement().getElementsByTagName("display");
      for (int i = 0; i < l.getLength(); i++) {
        NodeList nl = l.item(i).getChildNodes();
        for (int j = 0; j < nl.getLength(); j++) {
          String nodeName = nl.item(j).getNodeName();
          if ("tag".equals(nodeName)) {
            String nval = nl.item(j).getFirstChild().getNodeValue();
            if (s.equals(nval))
              return i;
          } 
        }
      }
    } catch (Exception ex) {}
    return -1;
  }

  public int getViewNameIndex(String s) {
    try {
      NodeList l = setupDocument.getDocumentElement().getElementsByTagName("display");
      for (int i = 0; i < l.getLength(); i++) {
        NodeList nl = l.item(i).getChildNodes();
        for (int j = 0; j < nl.getLength(); j++) {
          String nodeName = nl.item(j).getNodeName();
          if ("name".equals(nodeName)) {
            String nval = nl.item(j).getFirstChild().getNodeValue();
            if (s.equals(nval))
              return i;
          } 
        }
      }
    } catch (Exception ex) {}
    return -1;
  }

  private String getNodeText(int index, String s) {
    try {
      NodeList l = setupDocument.getDocumentElement().getElementsByTagName("display");
      NodeList nl = l.item(index).getChildNodes();
      for (int j = 0; j < nl.getLength(); j++) {
        String nodeName = nl.item(j).getNodeName();
        if (s.equals(nodeName)) {
          String nval = nl.item(j).getFirstChild().getNodeValue();
          return nval;
        } 
      }
    } catch (Exception ex) {}
    return null;
  }

  private String getAttrText(int index, String s) {
    try {
      NodeList l = setupDocument.getDocumentElement().getElementsByTagName("display");
      String at = ((Element)l.item(index)).getAttribute(s);
      return at;
    } catch (Exception ex) {}
    return null;
  }
 
  public String getDisplayType (int index) {
    return getAttrText(index, "type");
  }

  public int getComparisonBase (int index) {
    if (!"comparison".equals(getDisplayType(index)))
      return -1;
    String d = getNodeText(index,"baseData");
    return getId(d);
  }

  public int getComparisonSubtractor (int index) {
    if (!"comparison".equals(getDisplayType(index)))
      return -1;
    String d = getNodeText(index,"compareWith");
    return getId(d);
  }

  public String getNodeTag (int index) {
    return getNodeText(index, "tag");
  }

  public String getNodeReliabilityTag (int index) {
    return getNodeText(index, "reliabilityTag");
  }

  public String getNodeName (int index) {
    return getNodeText(index, "name");
  }

  public String getNodeToolTip (int index) {
    return getNodeText(index, "tooltip");
  }

  public boolean isAdjustable( int index ) {
    String a = getAttrText(index, "adjustable");
    return (a == null || a.length() == 0 || "true".equals(a));
  }

  public boolean isViewable( int index ) {
    String a = getAttrText(index, "view");
    return (a == null || a.length() == 0 || "true".equals(a));
  }

  public boolean isComparison (int index) {
    String a = getAttrText(index, "type");
    return (a != null && a.length() != 0 && "comparison".equals(a));
  }

  public static void main (String [] args) {
    Setup s = new Setup(args[0]);
    emit("tag "+s.getNodeText(0,"tag"));
    emit("attr "+s.getAttrText(0,"type"));
    emit("index "+s.getViewTagIndex(args[1]));
    emit("name index "+s.getViewNameIndex(args[2]));
    emit("display type: "+s.getDisplayType(0));
    emit("comparison sub: "+s.getComparisonSubtractor (0));
    emit("adj 0: "+s.isAdjustable (0));
    emit("comparison 0: "+s.isComparison (0));
    emit("comparison 0: "+s.isComparison (0));
    emit("view: "+s.isViewable (0));
  }
  
  public static void emit(String s) {
    System.out.println(s);
  }
}
