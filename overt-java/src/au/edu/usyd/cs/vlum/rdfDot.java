package au.edu.usyd.cs.vlum;
import java.util.*;
import java.io.*;
import java.net.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;


public class rdfDot {

  private int modelSize;
  private static final int maxNodeCount = 1024;
  private String [] about;
  private String [] title;
  private List<String> [] vpeers;
  private int [][] peers;

  public Map<String,Integer> nameMap;
  private Map<String,Integer> aboutMap; // preserve order
  
  public rdfDot (String uri) throws Exception {
      DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document rdfdoc = parser.parse(uri);

      NodeList l = rdfdoc.getDocumentElement().getElementsByTagName("rdf:Description");

      int doclen = l.getLength();

      int curId = 0;

      nameMap = new HashMap<String,Integer>();
      aboutMap = new HashMap<String,Integer>();
      
      vpeers = (java.util.List<String>[]) new java.util.List[doclen];
      title = new String[doclen];
      about = new String[doclen];
      peers = new int [doclen][];
      modelSize=doclen;
      
      
      for (int i = 0; i < doclen; i++) {
	  
	  Node n = l.item(i);
	  Element el = (Element)n;
	  about[i] = el.getAttribute("about");
	  //emit("about "+about[i]);
	  try {
	      title[i] = el.getElementsByTagName("dc:Title").item(0).getFirstChild().getNodeValue();
	  } catch (Exception e) {
	      title[i] = "No Title";
	  }

	  nameMap.put(title[i], new Integer(i));
	  aboutMap.put(about[i], new Integer(i));
	  
	  NodeList peersnl =  el.getElementsByTagName("gmp:peer");
	  vpeers[i] = new ArrayList<String>();
	  for (int k = 0; k < peersnl.getLength(); k++) {
	      Element p = (Element) peersnl.item(k);
	      vpeers[i].add(p.getAttribute("rdf:resource"));
	  }
	  
      }
	  
      // now weave the peers
      for (int i = 0; i < doclen; i++) {
	  List<String> p = vpeers[i];
	  peers[i] = new int[p.size()];
	  for (int j = 0; j < p.size(); j++) {
	      peers[i][j] = aboutMap.get(vpeers[i].get(j));
	  }
      }
      vpeers = null;
  }


  public String getTitle(int index) {
    return title[index];
  }

  public String getResource(int index) {
    return about[index];
  }

  public int [] getPeers(int index) {
    return peers[index];
  }

  public int getTitleIndex(String title) {
    return ((Integer)nameMap.get(title)).intValue();

  }

  public int size() { return modelSize; }

  public static void main (String [] args) throws Exception {
    rdfDot g = new rdfDot( args[0]);
     emit("digraph rdfGraph {");
     for (int i = 0; i < g.size(); i++) {
	 emit("  n"+i+" [ label = \""+g.getTitle(i)+"\"]");
	 int [] peers = g.getPeers(i);
	 for (int p=0;p< peers.length;p++)
	     emit("  n"+i+" -> n"+peers[p]);
     }
     emit("}");  
  }

  public static void emit(String s) {
    System.out.println(s);
  }
}
