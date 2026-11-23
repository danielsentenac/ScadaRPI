import com.genlogic.*; import javax.swing.*; import java.awt.*; import java.awt.image.*;
public class SnapM { public static void main(String[] a) throws Exception {
  final JFrame f = new JFrame(); final GlgJLWBean b = new GlgJLWBean(); b.SetDrawingFile(a[0]);
  f.getContentPane().setLayout(new GridLayout(1,1)); f.getContentPane().add(b); f.setSize(1280, 800); f.setVisible(true);
  Thread.sleep(4000);
  for (int i = 2; i + 1 < a.length; i += 2) b.SetSTag(a[i], a[i+1], true);
  b.Update(); Thread.sleep(1500);
  BufferedImage im = new BufferedImage(b.getWidth(), b.getHeight(), BufferedImage.TYPE_INT_RGB);
  Graphics2D g = im.createGraphics(); b.paint(g); g.dispose();
  javax.imageio.ImageIO.write(im, "png", new java.io.File(a[1])); System.exit(0); } }
