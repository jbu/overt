package au.edu.usyd.cs.vlum;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.regex.*;

public class SquidgePane extends JPanel implements MouseListener, MouseMotionListener {

  // constants for colour, etc.

  private static final Color backgroundColour = Color.black;
  private static final Color highLightColour = Color.white;
  
  private static final float positiveHue = 0.3333f;
  private static final float negativeHue = 0.0f;
  private static final float unknownHue = 0.172f;
  private static final float minKnownSaturation = 0.2f;
  private static final float maxKnownSaturation = 0.99f;
  private static final float unknownSaturation = 0.8f;
  private static final float minKnownBrightness = 0.2f;
  private static final float maxKnownBrightness = 0.99f;
  private static final float minUnknownBrightness = 0.2f;
  private static final float maxUnknownBrightness = 0.8f;

  private static final int colourSteps = 10;
  private static final int unknownShiftFactor = 6;
  private static final float unReliablePoint = 0.001f;
  private float knownXShiftMultiplier;
  private int unknownShiftDistance;
  private int currentHeight;

  private static final int largeFont = 16;
  private static final int smallFont = 10;
  private static final int largeUnknownFont = 16;
  private static final int smallUnknownFont = 10;

  // some colour maps

  private Color [] [] knownColourMap;
  private Color [] unknownColourMap;
  private Color [] knownHighLightMap;
  private Color unknownHighLightColour;

  // some font maps

  private Font [] fontMap;
  private FontMetrics [] fontMetricsMap;
  private int [] fontAscentMap;
  private int [] fontDescentMap;

  private Font [] unknownFontMap;
  private FontMetrics [] unknownFontMetricsMap;
  private int [] unknownFontAscentMap;
  private int [] unknownFontDescentMap;

  // arrays to cache some info for quick displays

  // i keep confusing these, so i'll make it plain...

  // this one is an array of length graphModel.size(), that is
  // indexed by topic, and contains y positions of that topic.
  private int [] yPositionMap; 

  // this one is an array of all topics that gives their x offset.
  private Rectangle [] positionMap;
  //private int [] xPositionMap; 
  //private int [] xEndPositionMap; 

  // this one is an array of length currentHeight, that is
  // indexed by y position, and contains the topic at that position.
  // we add unknownTopicPositionMap that handes the shifted topics.
  private int [] topicPositionMap; 
  private int [] unknownTopicPositionMap; 

  // this is like yPositionMap, but is for interim positions
  // in an animation 
  private int [] interimYPositionMap; 
  private int [] interimFontMap; 
  private int [] interimColourMap; 

  // back to regular programming
  private long [] depthGenerationMap;
  private int [] depthMap;
  private float [] normalisedDepthMap;
  private int [] [] drawingOrderMap;
  private int maximumGraphDepth;
  private GraphModel  graphModel;
  private float markStretch;

  private int [] fixedTopics = null;
  
  private int mousePressIndex;
  private int mousePressY;
  private int selectedTopic;
  private int mouseOverColumn;

  private int displayIndex;

  private ActionListener parent;
  private float markToMapMultiplier;
  //private float totalDepths;
  //private Graphics2D offScreenGraphics = null;
  private Graphics offScreenGraphics;
  //private BufferedImage offScreenImage;
  private Image offScreenImage;
  private Rectangle offScreenGraphicsRectangle;

  private int highLightTopic;
  private java.util.List<Integer> oldHighLightTopics;

  private int drawingStatus;
  private float desiredMark;

  private static final int HIGHLIGHTING = 1;
  private static final int MOVING = 2;
  private static final int DRAGGING = 3;
  private static final int IDLE = 4;
  private static final int COLOURING = 5;
  private static final int INIT = 6;

    private boolean sliderIsMarkSep = true;

  private boolean animate = false;

  private boolean alphaTrans = false;
  private boolean antiAlias = false;
  private boolean dragable = false;
  private Point clickPoint;
  private double dragThreshold = 3d;
  private boolean didntDrag = false;
  private boolean swapAx = true;

  // our event listener list. we like ActionListeners.
  java.util.List<ActionListener> actionListeners = null;

  SquidgePane() {
    topicPositionMap = null;
    unknownTopicPositionMap = null;
    yPositionMap = null;
    positionMap = null;
    interimYPositionMap = null;
    interimFontMap = null;
    interimColourMap = null;
    depthMap = null;
    normalisedDepthMap = null;
    depthGenerationMap = null;
    offScreenGraphicsRectangle = new Rectangle(0,0);
    currentHeight = 0;
    maximumGraphDepth = 0;
    offScreenGraphics = null;
    actionListeners = new ArrayList<ActionListener>();
    highLightTopic = -1;
    oldHighLightTopics = new ArrayList<Integer>();
    topicPositionMap = new int[1];
    unknownTopicPositionMap = new int[1];
    yPositionMap = new int [1];
    positionMap = new Rectangle [1];
    interimYPositionMap = new int [1];
    interimFontMap = new int[1];
    interimColourMap = new int [1];
    depthMap = new int [1];
    normalisedDepthMap = new float [1];
    depthGenerationMap = new long[1];
    drawingStatus = INIT;
    this.addMouseListener(this);
    this.addMouseMotionListener(this);
  }

  public void setDragable (boolean e) {
    dragable = e;
    drawingStatus = COLOURING;
    repaint();
  }

  public void setAnimate(boolean e) {
    animate = e;
  }

  public void setGraphModel ( GraphModel g ) {

    graphModel = g;
    int tsize = g.size();
    yPositionMap = new int [tsize];
    positionMap = new Rectangle [tsize];
    interimYPositionMap = new int [tsize];
    interimFontMap = new int [tsize];
    interimColourMap = new int [tsize];
    for (int i = 0; i < tsize; i++ ) {
      interimYPositionMap[i] = 0;
      interimFontMap[i] = 0;
      interimColourMap[i] = 0;
    }
    depthMap = new int [tsize];
    normalisedDepthMap = new float [tsize];
    selectedTopic = 0; //graphModel.getMinimumMarkPosition(displayIndex);
    depthGenerationMap = new long[tsize];
    maximumGraphDepth = 0;
    //totalDepths = 0;

    mousePressIndex = -1;
    //setPeerDepth(selectedTopic,0,System.currentTimeMillis());
    genSpanningTree(selectedTopic);

    updateMaps();

    clearFixedTopics();
    positionTopics();
    drawingStatus = COLOURING;
  }

  public void setSelectedTopic ( int index ) {


    String p = "";
    int [] pe = graphModel.getPeers(index);
    for (int q = 0; q < pe.length; q++)
      p = p + pe[q] + "|";

   if (selectedTopic == index)
      return;

    selectedTopic = index;

    maximumGraphDepth = 0;
    //totalDepths = 0;
    genSpanningTree(selectedTopic);
    setNormalisedDepths();
    mousePressIndex = -1;
    clearFixedTopics();
    doAnimation();
  }

    public void setSliderMarkSearch(boolean b) {
	sliderIsMarkSep = !b;
    }

  private void setMarkToMapMultiplier() {
    markToMapMultiplier = (colourSteps - 1) ;
  }

  private float scaledSpaceForDepth(int depth, int items, int height) {
    
    float ret = 0f;

    if (items < 100)
      ret = (float)Math.pow((double)depth+1f, 1.3f);
    else if (items < 350)
      ret = (float)Math.exp((double)depth*0.7+1f);
    else if (items < 550)
      ret = (float)Math.exp((double)depth*0.9+1f);
    else if (items < 750)
      ret = (float)Math.exp((double)depth+1f);


    //emit("se "+spaceExponent + " for d "+depth+" i "+items+" is "+ret);
    return  1/ret;
  }

  private void clearFixedTopics() {
    if (fixedTopics == null)
      fixedTopics = new int[1];
    fixedTopics[0] = -1;
  }

  private void positionTopics() {

    int tsize = graphModel.size();

    //float totalDepths = 0f;
    //for (int i = 0; i < tsize; i++) {
      //totalDepths += spaceForDepth(depthMap[i]);
    //}

    if (currentHeight == 0) {
      for (int i = 0; i < tsize; i++) {
        yPositionMap[i] = 0;
        positionMap[i] = new Rectangle();
        topicPositionMap[0] = i;
        unknownTopicPositionMap[0] = i;
      }
      return;
    }

    for (int i = 0; i < currentHeight; i++) {
      topicPositionMap[i] = -1;
      unknownTopicPositionMap[i] = -1;
    }

    float currentPosition = 0f;

    // these fix things up to take care of space at the ends. 
    if ( currentHeight < 25) {
      currentHeight = currentHeight / 2;
      currentPosition = currentHeight / 2;
    } else {
      currentHeight -= 25;
      currentPosition = 20f;
    }

    if (fixedTopics[0] == -1) {
      fixedTopics = new int[2];
      fixedTopics[0] = 0;
      yPositionMap[0] = (int) Math.round(currentPosition);
      fixedTopics[1] = tsize-1;
      yPositionMap[tsize-1] = currentHeight+20;
    }

    int pos = 0;
    float td = 0;
    for (int ft = 0; ft < fixedTopics.length-1; ft++) {

      int citems = fixedTopics[ft+1] - fixedTopics[ft];

      int cheight = yPositionMap[fixedTopics[ft+1]] - yPositionMap[fixedTopics[ft]];

      //emit("citems "+citems+" cheight "+cheight);

      td = scaledSpaceForDepth(getNormalisedDepth(fixedTopics[ft]),citems,cheight) / 2f;

      for (int i = fixedTopics[ft]; i < fixedTopics[ft+1]; i++) {
        td += scaledSpaceForDepth(getNormalisedDepth(i),citems,cheight);
      }

      td += scaledSpaceForDepth(getNormalisedDepth(fixedTopics[ft+1]),citems,cheight) / 2f;

      float depthSpaceMultiplier = (cheight) / td / 2;

      float sp = scaledSpaceForDepth(getNormalisedDepth(fixedTopics[ft]),citems,cheight) * depthSpaceMultiplier;
      pos = yPositionMap[fixedTopics[ft]];
 // *** possible bug? not adding the fixed one to the position map?
      //int fi = getFontIndexForTopic(fixedTopics[ft]);
      //if (TopicRecord.UNKNOWN_MARK == topicModel.getMarkAt(i)) {
        //int startj = pos - fontAscentMap[fi];
        //int endj = pos + fontDescentMap[fi];
      //}
      currentPosition = pos + sp;

      int fi, startj, endj;

      for (int i = fixedTopics[ft]+1; i < fixedTopics[ft+1]; i++) {

        sp = scaledSpaceForDepth(getNormalisedDepth(i),citems,cheight) * depthSpaceMultiplier;
        pos = Math.round(sp + (float)currentPosition);
        currentPosition += sp * 2;
        fi = getFontIndexForTopic(i);
        
        yPositionMap[i] = pos;
        if (unReliablePoint > graphModel.getNormalisedReliability(i,displayIndex)) {
          startj = pos - unknownFontAscentMap[fi];
          startj = Math.max(0,startj);
          endj = pos + unknownFontDescentMap[fi];
          endj = Math.min(currentHeight,endj);
          for (int j = startj ; j < endj ; j++) {
            topicPositionMap[j] = i;
          }
        } else {
          startj = pos - fontAscentMap[fi];
          startj = Math.max(0,startj);
          endj = pos + fontDescentMap[fi];
          endj = Math.min(currentHeight,endj);
          for (int j = startj ; j < endj ; j++) {
            topicPositionMap[j] = i;
          }
        }
      }
    }
    for (int ft = 0; ft < fixedTopics.length; ft++) {
      int fi = getFontIndexForTopic(fixedTopics[ft]);
      int startj, endj;
      pos = yPositionMap[fixedTopics[ft]];
      //float mark = graphModel.getNormalisedMark(ft,displayIndex);
      if (unReliablePoint > graphModel.getNormalisedReliability(ft,displayIndex)) {
        startj = pos - unknownFontAscentMap[ft];
        startj = Math.max(0,startj);
        endj = pos + unknownFontDescentMap[ft];
        endj = Math.min(currentHeight,endj);
        for (int j = startj ; j < endj ; j++) {
          topicPositionMap[j] = fixedTopics[ft];
        }
      } else {
        startj = pos - fontAscentMap[ft];
        startj = Math.max(0,startj);
        endj = pos + fontDescentMap[ft];
        endj = Math.min(currentHeight,endj);
        for (int j = startj ; j < endj ; j++) {
          topicPositionMap[j] = fixedTopics[ft];
        }
      }
    }
  }

  public String getSelectedDescription() {
    return graphModel.getResource(selectedTopic);
  }

  public int getSelectedId() {
    String s = getSelectedDescription();
    return Integer.parseInt(s.substring(s.lastIndexOf('/'),s.length()-1));
  }
  
  public void setDisplayIndex(int i) {
    displayIndex = i;
    setMarkToMapMultiplier();
    updateMaps();
    drawingStatus = COLOURING;
    repaint();
  }

  public void changeDisplay(int index) {
    displayIndex = index;
    desiredMark = 0.5f;
    setMarkToMapMultiplier();
    updateMaps();
    drawingStatus = COLOURING;
    paintImmediately(offScreenGraphicsRectangle);
    //repaint();
  }

  public void addActionListener(ActionListener l) { 
    actionListeners.add(l);
  }

  public void removeActionListener(ActionListener l) {
    actionListeners.remove(l);
  }

  protected void fireActionPerformed(ActionEvent e) {
    for (ActionListener a : actionListeners)
        a.actionPerformed(e);
  }


  public void setMarkSeparationValue(float v) {
    desiredMark = v;
    updateMaps();
    drawingStatus = COLOURING;
    repaint();
  }

  private Color getHighLightColourForTopic(int topicIndex) {

    float topicMark = graphModel.getNormalisedMark(topicIndex,displayIndex);
    
    if (unReliablePoint > graphModel.getNormalisedReliability(topicIndex,displayIndex))
      return unknownHighLightColour;

    int m = Math.round(topicMark * (colourSteps - 1)) ;

    return knownHighLightMap[m];
    
  }
  
  // we don't return a font here, because we'll want to reuse the index
  // to get the fontmetric as well.
  private int getFontIndexForTopic( int topicIndex ) {

    return getNormalisedDepth(topicIndex);

    //if (drawingStatus == MOVING) {
      //if (interimFontMap[topicIndex] - finaldepth != 0)
      //finaldepth = finaldepth + Math.round((interimFontMap[topicIndex] - finaldepth) / 2f);
    //}

    //interimFontMap[topicIndex] = finaldepth;

  }

  private void setNormalisedDepths() {
    // normalise it all to 0-1
    for (int i = 0; i < graphModel.size(); i++) {
      normalisedDepthMap[i] = (float)depthMap[i] / (float) maximumGraphDepth;
      //if (normalisedDepthMap[i] < 0f || normalisedDepthMap[i] > 1f)
      //emit("problem t "+i+" dm "+depthMap[i]+" n "+normalisedDepthMap[i]);
    }
  }

  private int getNormalisedDepth(int topic) {
    return Math.round(normalisedDepthMap[topic] * (colourSteps - 1));
  }

  private Color getColourForTopic(int topicIndex) {

    float topicMark = graphModel.getNormalisedMark(topicIndex,displayIndex);

    if (unReliablePoint > graphModel.getNormalisedReliability(topicIndex,displayIndex))
      return unknownColourMap[getNormalisedDepth(topicIndex)];

    //if (drawingStatus == MOVING) 
      //interimColourMap[topicIndex] = finaldepth + (( interimColourMap[topicIndex] - finaldepth) / 2);
    //else

    //interimColourMap[topicIndex] = finaldepth;


    int m = Math.round(topicMark * (colourSteps - 1)) ;

    return knownColourMap[getNormalisedDepth(topicIndex)][m];
    
  }

  private void emit(String s) { 
    System.out.println(s);
  }

  private void updateMaps() {
    calculateFontMaps();
    calculateColourMaps();
  }

  private void calculateFontMaps() {

    fontMap = new Font[colourSteps];
    fontMetricsMap = new FontMetrics[colourSteps];
    fontAscentMap = new int[colourSteps];
    fontDescentMap = new int[colourSteps];
    unknownFontMap = new Font[colourSteps];
    unknownFontMetricsMap = new FontMetrics[colourSteps];
    unknownFontAscentMap = new int[colourSteps];
    unknownFontDescentMap = new int[colourSteps];

    float fontStep = largeFont - smallFont;
    float unknownFontStep = largeUnknownFont - smallUnknownFont;

    for (int i = 0; i < colourSteps; i++) {
      int fontSize = (int) Math.round(smallFont + fontStep);
      fontStep = fontStep * 0.3f;
      fontMap[i] = new Font("Helvetica", Font.PLAIN, fontSize);
      fontMetricsMap[i] = getFontMetrics(fontMap[i]);
      fontAscentMap[i] = fontMetricsMap[i].getAscent();
      fontDescentMap[i] = fontMetricsMap[i].getDescent();

      fontSize = (int) Math.round(smallUnknownFont + unknownFontStep);
      unknownFontStep = unknownFontStep * 0.3f;
      unknownFontMap[i] = new Font("Helvetica", Font.PLAIN, fontSize);
      unknownFontMetricsMap[i] = getFontMetrics(unknownFontMap[i]);
      unknownFontAscentMap[i] = unknownFontMetricsMap[i].getAscent();
      unknownFontDescentMap[i] = unknownFontMetricsMap[i].getDescent();
     
    }
  }
  
  private void calculateColourMaps() {
    int satMidPoint = Math.round((desiredMark) * colourSteps);

    knownColourMap = new Color[colourSteps][colourSteps];
    unknownColourMap = new Color[colourSteps];
    knownHighLightMap = new Color[colourSteps];
    unknownHighLightColour = Color.getHSBColor(unknownHue, 1.0f, 1.0f);

    int posSatSteps = colourSteps - satMidPoint;
    int negSatSteps = satMidPoint;

    float posSatDelta = (maxKnownSaturation - minKnownSaturation)/posSatSteps;
    float negSatDelta = (maxKnownSaturation - minKnownSaturation)/negSatSteps;
    float brightnessDelta = (maxKnownBrightness - minKnownBrightness)/colourSteps;
    float ubrightDelta = (maxUnknownBrightness - minUnknownBrightness) / colourSteps;

    float b = maxKnownBrightness;
    float ub = maxUnknownBrightness;
    

    for (int i = 0; i < colourSteps; i++) {
      float s = maxKnownSaturation;
      for (int j = 0; j < negSatSteps; j++) {
        knownColourMap[i][j] = Color.getHSBColor(negativeHue, s-=negSatDelta, b); 
      }
      s = minKnownSaturation;
      for (int j = negSatSteps; j < colourSteps; j++) {
        knownColourMap[i][j] = Color.getHSBColor(positiveHue, s+=posSatDelta, b); 
      }
      if (i < negSatSteps)
        knownHighLightMap[i] = Color.getHSBColor(negativeHue, 1.0f, 1.0f);
      else
        knownHighLightMap[i] = Color.getHSBColor(positiveHue, 1.0f, 1.0f);

      unknownColourMap[i] = Color.getHSBColor(unknownHue, unknownSaturation, ub);
      ub -= ubrightDelta;
      //ub = ub / 2;
      b -= brightnessDelta;
    }
  }

  public void paintComponent (Graphics g) {

    //Graphics2D g2 = (Graphics2D) g;
    Graphics g2 = g;
    super.paintComponent(g);
    Insets insets = getInsets();
    int currentWidth = getWidth() - insets.left - insets.right;
    currentHeight = getHeight() - insets.top - insets.bottom;

    if ((offScreenGraphics == null)
      || (currentWidth != offScreenGraphicsRectangle.width)
      || (currentHeight != offScreenGraphicsRectangle.height))
    {
      //emit("reallocating offImage");
      unknownShiftDistance = currentWidth / unknownShiftFactor;
      knownXShiftMultiplier = unknownShiftDistance;
      //offScreenImage = (BufferedImage) createImage(currentWidth, currentHeight);
      offScreenImage = createImage(currentWidth, currentHeight);
      //offScreenGraphics = offScreenImage.createGraphics();
      offScreenGraphics = offScreenImage.getGraphics();
      offScreenGraphicsRectangle = new Rectangle(currentWidth, currentHeight);
      offScreenGraphics.setColor(backgroundColour);
      offScreenGraphics.fillRect(0,0,currentWidth, currentHeight);
      topicPositionMap = new int[currentHeight];
      unknownTopicPositionMap = new int[currentHeight];
      positionTopics();
    }
    
    if (drawingStatus == INIT) {
      offScreenGraphics.setColor(backgroundColour);
      offScreenGraphics.fillRect(0,0,currentWidth, currentHeight);
      topicPositionMap = new int[currentHeight];
      unknownTopicPositionMap = new int[currentHeight];
      positionTopics();
    } else if (drawingStatus == COLOURING || drawingStatus == MOVING || drawingStatus == DRAGGING) {

      offScreenGraphics.setColor(backgroundColour);
      offScreenGraphics.fillRect(0,0,currentWidth, currentHeight);

      int topicLength = graphModel.size();
      int fixedTopicIndex = 0;
      for (int i=0; i< topicLength; i++) {
        offScreenGraphics.setColor(getColourForTopic(i));
        String todraw = (String) graphModel.getTitle(i);
        float mark = graphModel.getNormalisedMark(i,displayIndex);
        int ypos;
        if ((drawingStatus == MOVING) && animate) {
          ypos = interimYPositionMap[i];
          interimYPositionMap[i] = ypos + (yPositionMap[i] - ypos) / 2;
        } else {
          ypos = yPositionMap[i];
          interimYPositionMap[i] = ypos;
        }
        int xpos;
        int fi = getFontIndexForTopic(i);
        xpos = 4 + Math.round((1f-graphModel.getNormalisedReliability(i,displayIndex)) * knownXShiftMultiplier);
        if (unReliablePoint > graphModel.getNormalisedReliability(i,displayIndex))  {
          offScreenGraphics.setFont(unknownFontMap[fi]);
          int ty = ypos - unknownFontAscentMap[fi];
          int tx = xpos;
          int h = unknownFontAscentMap[fi] + unknownFontDescentMap[fi];
          int w = unknownFontMetricsMap[fi].stringWidth(todraw);
          positionMap[i] = new Rectangle (tx,ty,w,h);
        } else {
          offScreenGraphics.setFont(fontMap[fi]);
          int ty = ypos - fontAscentMap[fi];
          int tx = xpos;
          int h = fontAscentMap[fi] + fontDescentMap[fi];
          int w = fontMetricsMap[fi].stringWidth(todraw);
          positionMap[i] = new Rectangle (tx,ty,w,h);
        }

        if (fixedTopics[fixedTopicIndex] == i) {
          //emit("fti; "+fixedTopicIndex+" i "+i);
          if (fixedTopicIndex != 0 && fixedTopicIndex != fixedTopics.length-1 ) {
            offScreenGraphics.setColor(Color.red);
            //offScreenGraphics.fillRect( 0, ypos - 5, 5, 5);
            offScreenGraphics.fillOval( 0, ypos - 5, 5, 5);
            offScreenGraphics.setColor(Color.white);
          } else
            offScreenGraphics.setColor(getColourForTopic(i));

          offScreenGraphics.drawString(todraw, xpos, ypos );

          if (fixedTopicIndex < fixedTopics.length-1) 
            fixedTopicIndex++;
        }
        else {
          offScreenGraphics.setColor(getColourForTopic(i));
          offScreenGraphics.drawString(todraw, xpos, ypos );
        }
        // bounding box drawing - for debugging.

        //offScreenGraphics.setColor(Color.gray);
        //offScreenGraphics.draw(positionMap[i]);

      }
    } else if (drawingStatus == HIGHLIGHTING) {
      int vLength = oldHighLightTopics.size();
      for (int ti : oldHighLightTopics) {
      //for (int i=0; i< vLength; i++) {
        //int ti = ((Integer)oldHighLightTopics.firstElement()).intValue();
        offScreenGraphics.setColor(getColourForTopic(ti));
        String todraw = (String) graphModel.getTitle(ti);
        float mark = graphModel.getNormalisedMark(ti,displayIndex);
        int ypos = yPositionMap[ti];
        int fi = getFontIndexForTopic(ti);
        int xpos;
        xpos = 4 + Math.round((1f-graphModel.getNormalisedReliability(ti,displayIndex)) * knownXShiftMultiplier);
        if (unReliablePoint > graphModel.getNormalisedReliability(ti,displayIndex))
          offScreenGraphics.setFont(unknownFontMap[fi]);
        else 
          offScreenGraphics.setFont(fontMap[fi]);

        //xPositionMap[ti] = xpos;
        //int namelen = fontMetricsMap[fi].stringWidth(todraw);
        //xEndPositionMap[ti] = xpos + namelen;
        
        offScreenGraphics.drawString(todraw, xpos, ypos );
        //oldHighLightTopics.removeElementAt(0);
      }
      oldHighLightTopics.clear();
      if (highLightTopic != -1) {
        //offScreenGraphics.setColor(getHighLightColourForTopic(highLightTopic));
        offScreenGraphics.setColor(highLightColour);
        String todraw = (String) graphModel.getTitle(highLightTopic);
        int ypos = yPositionMap[highLightTopic];
        float mark = graphModel.getNormalisedMark(highLightTopic,displayIndex);
        //emit("drawing at ypos: "+ypos);
        int fi = getFontIndexForTopic(highLightTopic);
        int xpos;
        xpos = 4 + Math.round((1f-graphModel.getNormalisedReliability(highLightTopic,displayIndex)) * knownXShiftMultiplier);
        if (unReliablePoint > graphModel.getNormalisedReliability(highLightTopic,displayIndex))
          offScreenGraphics.setFont(unknownFontMap[fi]);
        else 
          offScreenGraphics.setFont(fontMap[fi]);

        //xPositionMap[highLightTopic] = xpos;
        //int namelen = fontMetricsMap[fi].stringWidth(todraw);
        //xEndPositionMap[highLightTopic] = xpos + namelen;

        offScreenGraphics.drawString(todraw, xpos, ypos );

      }
      
    }
    g2.drawImage(offScreenImage, 0, 0, this);
  }
  
  public void mouseReleased ( MouseEvent e ) {
    //emit("Mouse button release "+ e.getY());
    //emit ("mouseReleased");
    if (mousePressY == e.getY())
      mouseClicked(e);
    if (didntDrag) 
      mouseClicked(e);
    didntDrag = false;
    mousePressY = -1;
    mousePressIndex = -1;
  }
  
  public void mouseEntered ( MouseEvent e ) {
  }

  public void mouseExited ( MouseEvent e ) {
    //emit ("mouseExited");
    if (highLightTopic != -1) 
      oldHighLightTopics.add(new Integer(highLightTopic));
    else
      return;
    highLightTopic = -1;
    mouseOverColumn = -1;
    drawingStatus = HIGHLIGHTING;
    repaint();
  }
  
  public void mouseDragged ( MouseEvent e ) {
    //emit ("mouseDragged");
    if (!dragable) { 
      return;
    }
    //if (e.getPoint().distance(clickPoint) < dragThreshold) {
    if (Math.abs(e.getY() - clickPoint.y) < dragThreshold) {
      didntDrag = true;
      return;
    }
    didntDrag = false;
    int y = e.getY();
    //int topic = topicPositionMap[y];
    int topic = mousePressIndex;
    
    if (y == -1 || topic == -1) return;

    dragTopic(topic, y);

    positionTopics();
    setPaintOrder();
    drawingStatus = DRAGGING;
    paintImmediately(offScreenGraphicsRectangle);
    //repaint();

  }
  
  private void dragTopic(int topic, int y) {

    //emit("topic: "+topic+" at "+y);
    //for (int i = 0; i < fixedTopics.length; i++) {
       //System.out.print(" "+fixedTopics[i]);
    //}
    //emit("");

    yPositionMap[topic] = y;

    int tindex = 0;

    boolean alreadyFixed = false;
    for (int i = 0; i < fixedTopics.length && fixedTopics[i] != -1; i++) {
      if (fixedTopics[i] == topic) {
        alreadyFixed = true;
        tindex = i;
      }
    }

    int totalLen = fixedTopics.length;

    if (! alreadyFixed) { //  insert
      //log("dragged on "+graphModel.getTitle(y)+ " index "+y);
      int i = 0;
      int [] newft = new int[fixedTopics.length+1];
      for (i = 0; i<fixedTopics.length && fixedTopics[i] < topic ; i++) {  // find i > topic
        newft[i] = fixedTopics[i];
      }
      //emit("inserting: "+topic+" at "+i);
      tindex = i;
      newft[i++] = topic;
      for (; i < newft.length; i++) {
        newft[i] = fixedTopics[i-1];
      }
      fixedTopics = newft;
      totalLen = fixedTopics.length;
    } else {  // release possibly overdragged topics
      //emit("checking "+tindex+" at "+yPositionMap[fixedTopics[tindex]]+" against "+(tindex-1)+ " at "+yPositionMap[fixedTopics[tindex-1]]);
      if (tindex > 1 && yPositionMap[fixedTopics[tindex]] < yPositionMap[fixedTopics[tindex-1]]) {
        int i = tindex-1;
        //emit("dragged above topic - deleting");
        for (; i < fixedTopics.length-1; i++)
          fixedTopics[i] = fixedTopics[i+1];
        fixedTopics[i] = -1;
        tindex--;
        totalLen--;
      }
      if (tindex < fixedTopics.length-1 && yPositionMap[fixedTopics[tindex]] > yPositionMap[fixedTopics[tindex+1]]) {
        int i = tindex+1;
        //emit("dragged below topic - deleting");
        for (; i < fixedTopics.length-1; i++)
          fixedTopics[i] = fixedTopics[i+1];
        fixedTopics[i] = -1;
        totalLen--;
      }
    }

    if (totalLen != fixedTopics.length) {
      int [] newft = new int[totalLen];
      for (int i = 0; i < totalLen ; i++) { 
        newft[i] = fixedTopics[i];
      }
      fixedTopics = newft;
    }

    //for (int i = 0; i < fixedTopics.length; i++) {
       //System.out.print(" "+fixedTopics[i]);
    //}
    //emit("");
    
  }

  private int mouseOverTopic ( MouseEvent e ) {
    int x = e.getX();
    int y = e.getY();

    Point pt = new Point(x,y);

    int theDi = Integer.MAX_VALUE;
    int mini = Integer.MAX_VALUE;
    int amini = Integer.MAX_VALUE;
    int aminix = Integer.MAX_VALUE;

    //emit(".");

    for (int i = 0; i < yPositionMap.length; i++) {

      // find the approx. range
      if ( yPositionMap[i] < y - 15) continue;
      if ( yPositionMap[i] > y + 10) break;

      // find a more exact range.
      //int xpos = (int) positionMap[i].getLocation().getX();
      //int ypos = (int) positionMap[i].getLocation().getY();
      int xpos = (int) positionMap[i].getLocation().x;
      int ypos = (int) positionMap[i].getLocation().y;
      //int botpos = ypos + (int) positionMap[i].getSize().getHeight();
      int botpos = ypos + (int) positionMap[i].getSize().height;

      if ( y < ypos || y > botpos ) continue;

      // if we're out of them all, just return the most reliable.
      if ( xpos < aminix ) {
        amini = i;
        aminix = xpos;
      }

      if ( ! positionMap[i].contains(pt) ) continue;

      //emit(" pt "+pt.toString());

      // by here we have only containing rects.
      // we find the rect with the min distance from x to either the
      // left or right edge.


      int lside = xpos;
      //emit ("  "+positionMap[i].toString() + " t " +graphModel.getTitle(i));
      if ( x > lside && x - lside < theDi ) {
        theDi = x - lside;
        mini = i;
        //emit ("   mil "+minil+ " mindil "+theDil);
      }
      //int rside = xpos + (int) positionMap[i].getSize().getWidth();
      int rside = xpos + (int) positionMap[i].getSize().width;
      if ( x < rside && rside - x < theDi ) {
        theDi = rside - x;
        mini = i;
        //emit ("   mir "+minir+ " mindir "+theDir);
      }

    }

    if (mini == Integer.MAX_VALUE)
      mini = amini;
    if (mini == Integer.MAX_VALUE)
      mini = -1;

    return mini;
  }

  
  public void mouseMoved ( MouseEvent e ) {
    //emit ("mouseMoved");
    //emit("Mouse move "+ e.toString());
    if (mouseOverTopic(e) == highLightTopic)
      return;

    String ast = "SquidgePaneShowString";

    if (highLightTopic != -1) 
      oldHighLightTopics.add(highLightTopic);
    
    highLightTopic = mouseOverTopic(e);

    if (highLightTopic != -1) {
      float mark = graphModel.getMark(highLightTopic,displayIndex);
      float rel = graphModel.getNormalisedReliability(highLightTopic,displayIndex);
      ast = ast +graphModel.getTitle(highLightTopic) + " " + 
            "score "+(mark*100)+"%" +
            " certainty "+(rel*100)+"%";

            //(unReliablePoint > graphModel.getNormalisedReliability(highLightTopic,displayIndex)?"":"score "+(mark*100)+"%") +
    }

    drawingStatus = HIGHLIGHTING;
    //emit("firing ap: "+ast);
    fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ast));
    repaint();
  }
  
  public void mousePressed(MouseEvent e) {
    //emit ("mousePressed");
    //emit("Mouse button press "+ e.getY());
    int ypos = e.getY();
    mousePressIndex = mouseOverTopic(e);
    mousePressY = e.getY();
    clickPoint = e.getPoint();
  }

  private void doExpando(int index) {
    if (!dragable) { 
      return;
    }
    //emit("expando on "+index);
    //clearFixedTopics();
    int steps = 5;
    int step = 20;
    int sDelta = graphModel.size()/20;
    int y = positionMap[index].getLocation().y;
    int ut = Math.max(0,index-sDelta);
    int dt = Math.min(graphModel.size()-1,index+sDelta);

    int utmp = positionMap[ut].getLocation().y;
    int dtmp = positionMap[dt].getLocation().y;

   /*
    utmp = utmp - step;
    utmp =  Math.max(10,utmp);
    dtmp = dtmp + step;
    dtmp =  Math.min(currentHeight-10,dtmp);

    //emit("uputing "+ut+" at "+utmp);
    dragTopic(ut, utmp);
    //emit("dputing "+dt+" at "+dtmp);
    dragTopic(dt, dtmp);

    positionTopics();
    setPaintOrder();
    drawingStatus = DRAGGING;
    paintImmediately(offScreenGraphicsRectangle);
    //repaint();
*/

    int ftlen = fixedTopics.length;
    int [] newft = new int[ftlen];
    int ftc = 0;

    for (int i = 0; i < fixedTopics.length; i++) {
      if (fixedTopics[i] <= ut || fixedTopics[i] >= dt) {
        newft[ftc++] = fixedTopics[i];
      } else {
        //emit("removing "+fixedTopics[i]);
      }
    }

    if (ftc < ftlen) {
      fixedTopics = new int[ftc];
      for (int i = 0;  i < ftc; i++) {
        fixedTopics[i] = newft[i];
        //emit("ft "+fixedTopics[i]);
      }
    }

    for (int i = 0; i < steps; i++) { 

      utmp = utmp - step;
      utmp =  Math.max(10,utmp);
      dtmp = dtmp + step;
      dtmp =  Math.min(currentHeight-10,dtmp);

      //emit("uputing "+ut+" at "+utmp);
      dragTopic(ut, utmp);
      //emit("dputing "+dt+" at "+dtmp);
      dragTopic(dt, dtmp);

      positionTopics();
      setPaintOrder();
      drawingStatus = DRAGGING;
      paintImmediately(offScreenGraphicsRectangle);
      //repaint();
    }
  }
  
  public void mouseClicked(MouseEvent e) {
    //emit ("mouseClicked");
    if (mousePressY == -1)
      return; // already handled by mouseReleased;
    //emit("Mouse button click "+ e.getY());
    int ypos = e.getY();
    ypos = mouseOverTopic(e);

    if (ypos == -1) {
    } else if ((e.getModifiers() & InputEvent.BUTTON3_MASK)
      == InputEvent.BUTTON3_MASK) {
    //} else if (e.isPopupTrigger()) {
      doExpando(ypos);
      return;
    } else {
      String p = "";
      int [] pe = graphModel.getPeers(ypos);
      for (int q = 0; q < pe.length; q++)
        p = p + pe[q] + "|";
    }

    if (ypos == selectedTopic) 
      return;

    if (ypos != -1) {
      fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "SquidgePaneClick:"+ypos));
      mousePressIndex = ypos;
      selectedTopic = ypos;
      maximumGraphDepth = 0;
      //totalDepths = 0;
      //setPeerDepth(mousePressIndex,0,System.currentTimeMillis());
      genSpanningTree(mousePressIndex);
      mousePressIndex = -1;
      clearFixedTopics();
      doAnimation();
    } 
  }

  private void setPaintOrder() {
    // do this later for gods' sake.
    // in fact, don't do it at all.
  }
  
  private void genSpanningTree (int root) {
    //  just sets the depths and the maximumGraphDepth;
    // we shall ignore orphans...

    // take the root. set it's depth to 0, and set a marker so done.
    // stick it in queue. 

    // take from queue.
    // find children and set depth to node.depth + 1 and dm if ! done.
    // stick children on queue.

    long genMarker = System.currentTimeMillis();

    depthMap[root] = 0;
    depthGenerationMap[root] = genMarker;

    java.util.List<Integer> todo = new ArrayList<Integer>();
    todo.add(root);
    
    while (todo.size() != 0) {
      root = todo.get(0);
      todo.remove(0);
      int peers [] = graphModel.getPeers(root);
      int depth = depthMap[root];
      if (peers != null) {
        for (int i = 0; i < peers.length; i++) {
          if (depthGenerationMap[peers[i]] != genMarker) {
            depthGenerationMap[peers[i]] = genMarker;
            depthMap[peers[i]] = depth + 1;
            //System.out.println("adding node "+peers[i]+" d "+(depth+1));
            todo.add(peers[i]);
          } else if (depthMap[peers[i]] > depth + 1) {
            depthMap[peers[i]] = depth + 1;
            //System.out.println("adding node "+peers[i]+" d "+(depth+1));
            todo.add(peers[i]);
          }
        }
      }
      if (depth > maximumGraphDepth)
        maximumGraphDepth = depth;
    }
    setNormalisedDepths();
  }

    public void searchMarkFor (float mark) {
	int tsize = graphModel.size();
	for (int i = 0; i < tsize; i++) {
	}
    }

  public void searchFor (String regx) {
    Pattern pat = null;
    //totalDepths = 0f;
    try {
      pat = Pattern.compile(regx, Pattern.CASE_INSENSITIVE);
    } catch (Exception ex) { return; }
    int tsize = graphModel.size();
    for (int i = 0; i < tsize; i++) {
      String name = (String) graphModel.getTitle(i);
      Matcher match = pat.matcher(name);
      boolean ma = false;
      try {
	ma = match.lookingAt();
      } catch (Exception ex)
      {}
      if (ma) {
        depthMap[i] = 0;
      }
      else
        depthMap[i] = 10;
    }
    maximumGraphDepth=10;
    setNormalisedDepths();
    doAnimation();
    MouseEvent m = new MouseEvent (this, 0, (new Date()).getTime(),0,0,0,1,false);
    //this.mouseClicked(m); // give it one...
  }

  private void doAnimation() {
    if (!animate) {
      drawingStatus = COLOURING;
      paintImmediately(offScreenGraphicsRectangle);
      return;
    }

    positionTopics();
    setPaintOrder();

    int steps = 6;
    long startTime = System.currentTimeMillis();
    int held = 0;
    long taken = 0l;

    long lastAnimStep = 0l;
    long now = 0l;
    int mss = 900/steps;

    drawingStatus = MOVING;

    for (int i = 0; i < steps - 1; i++) {
      now = System.currentTimeMillis();
      taken = now - lastAnimStep;
      if (taken < mss) {
        held += mss - taken;
        try { 
          Thread.currentThread().sleep(mss - taken);
        } catch (InterruptedException ex) {
        }
      }

      lastAnimStep = now;
      paintImmediately(offScreenGraphicsRectangle);
    }

    now = System.currentTimeMillis();
    taken = now - lastAnimStep;
    if (now - lastAnimStep < mss) {
      held += mss - taken;
      try { 
        Thread.currentThread().sleep(mss - taken);
      } catch (InterruptedException ex) {
      }
    }

    drawingStatus = COLOURING;
    paintImmediately(offScreenGraphicsRectangle);
    //emit("animation took "+(System.currentTimeMillis() - startTime) + " held "+held);
  }

  public void setAntiAlias(boolean b){}
  public void setAlphaTransparency(boolean b){}


}


