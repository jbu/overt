package au.edu.usyd.cs.vlum;
import org.w3c.dom.*;
import javax.xml.parsers.*;

public class Questions {

  private Document questionsDocument;
  private String [] questionIds;
  private int cnt;

  public final static int RADIO = 1;
  public final static int PLAIN = 2;
  public final static int TEXT = 3;
  public final static int INPUT = 4;

  public Questions (String uri) {
    try {
    	
	  DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();

      questionsDocument = parser.parse(uri);
  
      int c = size();
      //emit("len "+c);
      questionIds = new String[c];
      for (int i = 0; i < c; i++) {
        String a = getAttrText(i, "ID");
        if (a != null) {
          questionIds[i] = a;
        } else
          questionIds[i] = null;
      }
    } catch (Exception ex) { emit("questions init problem: "+ex.toString()); }
    cnt = -1;
  }

  public boolean next() {
    if (cnt < (size()-1)) {
      cnt++;
      return true;
    }
    return false;
  }

  private String getAttrText(int index, String s) {
    try {
      NodeList l = questionsDocument.getDocumentElement().getElementsByTagName("question");
      String at = ((Element)l.item(index)).getAttribute(s);
      return at;
    } catch (Exception ex) {}
    return null;
  }


  public String getId () {
    return getAttrText(cnt, "ID");
  }

  public int size() {
    try {
      NodeList l = questionsDocument.getDocumentElement().getElementsByTagName("question");
      return l.getLength();
    } catch (Exception ex) {}
    return 0;
  }

  public String getQuestion() {
    try {
      return questionsDocument.getDocumentElement().getElementsByTagName("question").item(cnt).getFirstChild().getNodeValue();
    } catch (Exception ex) {}
    return null;
  }

  public String getQuestion(int index) {

    try {
      return questionsDocument.getDocumentElement().getElementsByTagName("question").item(index).getFirstChild().getNodeValue();
    } catch (Exception ex) {}
    return null;
  }

  public String getQuestion(String id) {
    int idx = -1;
    for (int i = 0; i < size(); i++) {
      if (id.equals(questionIds[i])) {
        idx = i;
        break;
      }
    }

    if (idx == -1) return null;

    try {
      return questionsDocument.getDocumentElement().getElementsByTagName("question").item(idx).getFirstChild().getNodeValue();
    } catch (Exception ex) {}
    return null;
  }

  public int radioCount() {
    try {
      return Integer.parseInt(getAttrText(cnt, "choices"));
    } catch (Exception ex) {}
    return 0;
  }

  public int getType() {
    try {
      String t = getAttrText(cnt, "type");
      if (t == null || "simple".equals(t))
        return PLAIN;
      if ("radio".equals(t))
        return RADIO;
      if ("textarea".equals(t))
        return TEXT;
      if ("input".equals(t))
        return INPUT;
    } catch (Exception ex) {}
    return PLAIN;
  }

  public int getIndex() {
    return cnt;
  }

  public boolean isRadio() {
    return (getType() == RADIO);
  }

  public boolean isText() {
    return (getType() == TEXT);
  }

  public boolean isPlain() {
    return (getType() == PLAIN);
  }

  public boolean isInput() {
    return (getType() == INPUT);
  }

  public int getCols() {
    if (!isText() && !isInput()) return 0;
    String c = getAttrText(cnt, "cols");
    if (c == null) return 0;
    return Integer.parseInt(c);
  }

  public int getRows() {
    if (!isText()) return 0;
    String c = getAttrText(cnt, "rows");
    if (c == null) return 0;
    return Integer.parseInt(c);
  }

  public String getReasons(int idx) {
    String c = getAttrText(idx, "reason");
    if (c != null)
      return c;
    return "";
  }

  public String getReasons(String id) {

    int idx = -1;
    for (int i = 0; i < size(); i++) {
      if (id.equals(questionIds[i])) {
        idx = i;
        break;
      }
    }

    if (idx == -1) return null;

    String c = getAttrText(idx, "reason");
    if (c != null)
      return c;
    return "";
  }

  public String getPymark(int idx) {
    String c = getAttrText(idx, "pymark");
    if (c != null)
      return c;
    return "";
  }

  public String getPymark(String id) {

    int idx = -1;
    for (int i = 0; i < size(); i++) {
      if (id.equals(questionIds[i])) {
        idx = i;
        break;
      }
    }

    if (idx == -1) return null;

    String c = getAttrText(idx, "pymark");
    if (c != null)
      return c;
    return "";
  }

  public static void main (String [] args) {
    Questions s = new Questions(args[0]);
    if (args.length <= 1) {
      emit("s "+s.size());
      while (s.next()) {
        emit("e "+s.getType() + " q "+s.getQuestion());
      }
      return;
    }
    if ("r".equals(args[1])) {
      int idx = -1;
      try {
        idx = (new Integer(args[2])).intValue();
        String q = s.getReasons(idx);
        if (q != null)
          emit(q);
      } catch (Exception e) {
        String q = s.getReasons(args[2]);
        if (q != null)
          emit(q);
      }
      return;
    }
    if ("p".equals(args[1])) {
      int idx = -1;
      try {
        idx = (new Integer(args[2])).intValue();
        String q = s.getPymark(idx);
        if (q != null)
          emit(q);
      } catch (Exception e) {
        String q = s.getPymark(args[2]);
        if (q != null)
          emit(q);
      }
      return;
    }
    int idx = -1;
    try {
      idx = (new Integer(args[1])).intValue();
      String q = s.getQuestion(idx);
      if (q != null)
        emit(q);
    } catch (Exception e) {
      String q = s.getQuestion(args[1]);
      if (q != null)
        emit(q);
    }
  }
  
  public static void emit(String s) {
    System.out.println(s);
  }
}
