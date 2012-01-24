package au.edu.usyd.cs.vlum;
import java.awt.*;
import java.awt.event.*;

public class SimpleDialog extends Dialog implements ActionListener {
  private TextField field;
  private Frame parent;
  private Button setButton;
  private Button cancelButton;

  SimpleDialog(Frame dw, String title, String l) {
    super(dw, title, true);
    parent = dw;

    Panel p1 = new Panel();
    Label label = new Label (l);
    p1.add(label);
    field = new TextField(40);
    field.addActionListener(this);
    p1.add(field);
    add("Center",p1);

    Panel p2 = new Panel();
    p2.setLayout(new FlowLayout(FlowLayout.RIGHT));
    cancelButton = new Button("Cancel");
    cancelButton.addActionListener(this);
    setButton = new Button("OK");
    setButton.addActionListener(this);
    p2.add(cancelButton);
    p2.add(setButton);
    add("South", p2);
    pack();
    setVisible(true);
  }

  public String getText() {
    return field.getText();
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == cancelButton)
       field.setText("");
    setVisible(false);
  }

  public static void main(String [] args) {
    Frame f = new Frame("");
    SimpleDialog d = new SimpleDialog(f, "this","that");
    System.out.println(""+d.getText());
    d.dispose();
    f.dispose();
  }
  
}
