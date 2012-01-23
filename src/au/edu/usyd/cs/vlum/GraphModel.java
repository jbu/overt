package au.edu.usyd.cs.vlum;

import java.util.*;
import java.io.*;
import java.net.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.awt.*;
import javax.swing.*;
import javax.xml.parsers.*;


public class GraphModel {

  private int markCount;
  private int modelSize;
  private static final int maxNodeCount = 1024;
  private String [] about;
  private String [] title;
  private float [][] mark;  
  private float [][] normalisedMark;  
  private int [] markMins;
  private int [] markMaxes;
  private float [][] reliability;
  private float [][] normalisedReliability;
  private int [] relMins;
  private int [] relMaxes;
  private java.util.List<String> [] vpeers;
  private int [][] peers;
  private int maxPeers = Integer.MIN_VALUE;
  private int minPeers = Integer.MAX_VALUE;
  private int totalPeers = 0;

  Map<String, Integer> nameMap;
  Map<String, Integer> aboutMap; // preserve order

  public GraphModel (Setup s, String uri) throws Exception  {
    this (s, uri, null);
  }

  public GraphModel (Setup s, String uri, Component parentFrame) throws Exception {
    ProgressMonitor progMon = null;
    DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	Document rdfdoc = null;
    if (parentFrame != null) {
      ProgressMonitorInputStream pmis = new ProgressMonitorInputStream( 
        parentFrame, 
        "Loading Model", 
        new URL(uri).openStream());
      pmis.getProgressMonitor().setMillisToDecideToPopup(100);
      rdfdoc = parser.parse(new InputSource(new BufferedInputStream(pmis)));
    } else 
      rdfdoc = parser.parse(uri);
    
    markCount = s.displaysLength();

    NodeList l = rdfdoc.getDocumentElement().getElementsByTagName("rdf:Description");

    //  this gives us too many children! no idea why. we need to hack the iteration to compensate
    //NodeList l = ((Node)rdfdoc.getDocumentElement()).getChildNodes();
    // i think it works with the new parsetype thing

    int doclen = l.getLength();


    nameMap = new HashMap<String, Integer>();
    aboutMap = new HashMap<String, Integer>();
    mark = new float[doclen][markCount];
    normalisedMark = new float[doclen][markCount];
    markMins = new int[markCount];
    markMaxes = new int[markCount];
    reliability = new float[doclen][markCount];
    normalisedReliability = new float[doclen][markCount];
    relMins = new int[markCount];
    relMaxes = new int[markCount];

    for (int i = 0; i < markCount; i++) {
      markMins[i] = Integer.MIN_VALUE;
      markMaxes[i] = Integer.MIN_VALUE;
      relMins[i] = Integer.MIN_VALUE;
      relMaxes[i] = Integer.MIN_VALUE;
    }

    vpeers = (java.util.List<String>[]) new java.util.List [doclen];
    title = new String[doclen];
    about = new String[doclen];
    peers = new int [doclen][];
    modelSize=0;

    if (parentFrame != null) 
      progMon = new ProgressMonitor(parentFrame, "Dealing with model", "Building Model", 0, (doclen)*3);

    for (int i = 0; i < doclen; i++) {
        
      modelSize++;
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
      totalPeers += vpeers[i].size();
      if (vpeers[i].size() > maxPeers) maxPeers = vpeers[i].size();
      if (vpeers[i].size() < minPeers) minPeers = vpeers[i].size();

      java.util.List<Integer> marksToDo = new Vector<Integer>();

      for (int j = 0; j < markCount; j++) {
        mark[i][j] = Float.MIN_VALUE;
        normalisedMark[i][j] = Float.MIN_VALUE;
        reliability[i][j] = 0f;
        normalisedReliability[i][j] = 0f;
        marksToDo.add(new Integer(j));
      }

      NodeList mknl =  el.getElementsByTagName("gmp:results");
      for (int m = 0; m < mknl.getLength(); m++) {
        Element mke = (Element) mknl.item(m);
        String mtype = mke.getAttribute("gmp:dataset");
        String mk = mke.getAttribute("gmp:mark");
        String rl = mke.getAttribute("gmp:reliability");
        int mindex = s.getViewNameIndex(mtype);
        marksToDo.remove(new Integer(mindex));

        //emit("i "+i+"mindex "+mindex+" mk "+mk);
        mark[i][mindex] = (new Float(mk)).floatValue();

        if (markMins[mindex] == Integer.MIN_VALUE || mark[i][mindex] < mark[markMins[mindex]][mindex]) 
          markMins[mindex] = i;
        if (markMaxes[mindex] == Integer.MIN_VALUE || mark[i][mindex] > mark[markMaxes[mindex]][mindex]) 
          markMaxes[mindex] = i;

        reliability[i][mindex] = (new Float(rl)).floatValue();

        if (relMins[mindex] == Integer.MIN_VALUE || reliability[i][mindex] < reliability[relMins[mindex]][mindex]) {
          relMins[mindex] = i;
        }
        if (relMaxes[mindex] == Integer.MIN_VALUE || reliability[i][mindex] > reliability[relMaxes[mindex]][mindex]) {
          relMaxes[mindex] = i;
        }
      }

      for (Integer mindex : marksToDo) {  // we do comparisons here.
        //int mindex = ((Integer)marksToDo.get(j)).intValue();
        if (!s.isComparison(mindex)) {
          if (relMins[mindex] == Integer.MIN_VALUE || reliability[i][mindex] < reliability[relMins[mindex]][mindex]) {
            relMins[mindex] = i;
          }
          if (relMaxes[mindex] == Integer.MIN_VALUE || reliability[i][mindex] > reliability[relMaxes[mindex]][mindex]) {
            relMaxes[mindex] = i;
          }
          continue;
        }
        int base = s.getComparisonBase(mindex);
        int subt = s.getComparisonSubtractor(mindex);

        if (mark[i][base] != Float.MIN_VALUE && mark[i][subt] != Float.MIN_VALUE) {
          mark[i][mindex] = ((mark[i][base] - mark[i][subt]) / 2f) + 0.5f;
          if (markMins[mindex] == Integer.MIN_VALUE || mark[i][mindex] < mark[markMins[mindex]][mindex]) 
            markMins[mindex] = i;
          if (markMaxes[mindex] == Integer.MIN_VALUE || mark[i][mindex] > mark[markMaxes[mindex]][mindex]) 
            markMaxes[mindex] = i;
        }

        reliability[i][mindex] = reliability[i][base] * reliability[i][subt];

        if (relMins[mindex] == Integer.MIN_VALUE || reliability[i][mindex] < reliability[relMins[mindex]][mindex]) {
          relMins[mindex] = i;
        }
        if (relMaxes[mindex] == Integer.MIN_VALUE || reliability[i][mindex] > reliability[relMaxes[mindex]][mindex]) {
          relMaxes[mindex] = i;
        }

      }
      if (progMon != null)
        progMon.setProgress(i);
    }
    float [] mmedians = new float[markCount]; // this is the correction. ie how much to add to get to 0-1
    float [] mstretch = new float[markCount];
    float [] rmedians = new float[markCount];
    float [] rstretch = new float[markCount];
    for (int i = 0; i < markCount; i++) {
      if (markMins[i] != Integer.MIN_VALUE 
       && mark[markMins[i]][i] != Float.MIN_VALUE
       && markMaxes[i] != Integer.MIN_VALUE
       && mark[markMaxes[i]][i] != Float.MIN_VALUE
       && mark[markMaxes[i]][i] != mark[markMins[i]][i]) {
        mmedians[i] = -mark[markMins[i]][i];
        mstretch[i] = 1f / (mark[markMaxes[i]][i] - mark[markMins[i]][i]);
      } else if (mark[markMaxes[i]][i] == mark[markMins[i]][i] ) {
        mmedians[i] = 0f;
        mstretch[i] = 1f;
      } else {
        mmedians[i] = Float.MIN_VALUE;
        mstretch[i] = Float.MIN_VALUE;
      }
      
      //emit("i "+i+" mmed "+mmedians[i]+" ms "+mstretch[i] + " max "+mark[markMaxes[i]][i]+" min "+mark[markMins[i]][i]);

      if (relMins[i] != Integer.MIN_VALUE 
       && relMaxes[i] != Integer.MIN_VALUE
       && reliability[relMaxes[i]][i] != reliability[relMins[i]][i]) {
        rmedians[i] = -reliability[relMins[i]][i];
        rstretch[i] = 1f / (reliability[relMaxes[i]][i] - reliability[relMins[i]][i]);
      } else if (reliability[relMaxes[i]][i] == reliability[relMins[i]][i] ) {
        rmedians[i] = 0f;
        rstretch[i] = 1f;
      } else {
        rmedians[i] = Float.MIN_VALUE;
        rstretch[i] = Float.MIN_VALUE;
      }
      //emit("i "+i+" rmed "+rmedians[i]+" rs "+rstretch[i] + " rmax "+reliability[relMaxes[i]][i]+" rmin "+reliability[relMins[i]][i]);
    }
    if (progMon != null)
      progMon.setNote("Normalising Marks");
    for (int i = 0; i < doclen; i++) {
      for (int j = 0; j < markCount; j++) {  // we normalize to 0.0 -- 1.0 here

        if (mark[i][j] != Float.MIN_VALUE && mmedians[j] != Float.MIN_VALUE) 
          normalisedMark[i][j] = ( mark[i][j] + mmedians[j] ) * mstretch[j];

        normalisedReliability[i][j] = ( reliability[i][j] + rmedians[j] ) * rstretch[j];

        if (normalisedMark[i][j] != Float.MIN_VALUE && mark[i][j] == Float.MIN_VALUE) 
          emit("** mark "+i+","+j+" = "+mark[i][j]+" ms "+mstretch[j] + " mm "+mmedians[j]+" max "+mark[markMaxes[i]][i]+" min "+mark[markMins[j]][j]+" nmark "+normalisedMark[i][j]+" t "+title[i]);

        if (normalisedMark[i][j] > 1 || normalisedMark[i][j] < 0)
          emit("** nmark "+i+","+j+" = "+normalisedMark[i][j]+" ms "+mstretch[j] + " mm "+mmedians[j]+" max "+mark[markMaxes[j]][j]+" min "+mark[markMins[j]][j]+" t "+title[i]);
        if (normalisedReliability[i][j] > 1 || normalisedReliability[i][j] < 0)
          emit("** nreli "+i+","+j+" = "+normalisedReliability[i][j]+" rs "+rstretch[j] + " rm "+rmedians[j]+" max "+reliability[relMaxes[j]][j]+" min "+reliability[relMins[j]][j]+" rel "+reliability[i][j]+" t "+title[i]);
      }
      if (progMon != null)
        progMon.setProgress((doclen)+i);
    }

    // now weave the peers
    if (progMon != null)
      progMon.setNote("Weaving Peers");
    for (int i = 0; i < doclen; i++) {
      java.util.List<String> p = vpeers[i];
      peers[i] = new int[p.size()];
      for (int j = 0; j < p.size(); j++) {
        peers[i][j] = (aboutMap.get(vpeers[i].get(j))).intValue();
      }
      if (progMon != null)
        progMon.setProgress((doclen*2)+i+1);
    }
    vpeers = null;
  }

  public float getMark(int index, int dataset) {
    return mark[index][dataset];
  }

  public float getNormalisedMark(int index, int dataset) {
    return normalisedMark[index][dataset];
  }

  public float getReliability(int index, int dataset) {
    return reliability[index][dataset];
  }

  public float getNormalisedReliability(int index, int dataset) {
    return normalisedReliability[index][dataset];
  }

  public String getTitle(int index) {
    return title[index];
  }

  public float getMaximumMark(int index) {
    return mark[markMaxes[index]][index];
  }

  public float getMinimumMark(int index) {
    return mark[markMins[index]][index];
  }

  public int getMaximumMarkPosition(int index) {
    return markMaxes[index];
  }

  public int getMinimumMarkPosition(int index) {
    return markMins[index];
  }

  public float getMaximumReliability(int index) {
    return reliability[markMaxes[index]][index];
  }

  public float getMinimumReliability(int index) {
    return reliability[markMins[index]][index];
  }

  public int getMaximumReliabilityPosition(int index) {
    return relMaxes[index];
  }

  public int getMinimumReliabilityPosition(int index) {
    return relMins[index];
  }

  public String getResource(int index) {
    return about[index];
  }

  public int [] getPeers(int index) {
    return peers[index];
  }

  public float getAveragePeers() {
    return totalPeers / (float)size();
  }

  public int getMaxPeers() {
    return maxPeers;
  }

  public int getMinPeers() {
    return minPeers;
  }

  public int getTitleIndex(String title) {
    return ((Integer)nameMap.get(title)).intValue();

  }

  public int[] greaterThan(int mindex, float m) {
    java.util.List<Integer> ret = new ArrayList<Integer>();
    for (int i = 0 ; i < mark.length; i++) {
      if (mark[i][mindex] > m)
        ret.add(new Integer(i));
    }

    int[] r = new int[ret.size()];
    for (int i = 0 ; i < ret.size(); i++) {
      r[i] = (ret.get(i)).intValue();
    }

    return r;
    
  }

  public int[] lessThan(int mindex, float m) {
    java.util.List<Integer> ret = new ArrayList<Integer>();
    for (int i = 0 ; i < mark.length; i++) {
      if (mark[i][mindex] < m)
        ret.add(i);
    }

    int[] r = new int[ret.size()];
    for (int i = 0 ; i < ret.size(); i++) {
      //r[i] = (ret.get(i)).intValue();
      r[i] = ret.get(i);
    }

    return r;
    
  }

  public int size() { return modelSize; }

  public static void main (String [] args) throws Exception {
    Setup s = new Setup(args[0]);
    GraphModel g = new GraphModel(s, args[1]);
    int mindex = g.getTitleIndex(args[2]);
    emit("max "+
         g.getTitle(g.getMaximumMarkPosition(0))+ 
         " min "+
         g.getTitle(g.getMinimumMarkPosition(0))+
         " len "+
         g.size()
    );
    emit("i "+mindex+" m "+g.getMark(mindex,0)+" r "+g.getReliability(mindex,0));
    int [] p = g.getPeers(mindex);
    for (int i = 0; i < p.length; i++)
      emit("peers: "+g.getTitle(p[i])+" m "+g.getMark(p[i],0)+" r "+g.getReliability(p[i],0));
    p = g.lessThan(0,0.4f);
    for (int i = 0; i < p.length; i++)
      emit("lessthan: "+p[i]);
    p = g.greaterThan(0,0.8f);
    for (int i = 0; i < p.length; i++)
      emit("greaterthan: "+p[i]);
  }

  public static void emit(String s) {
    System.out.println(s);
  }
}
