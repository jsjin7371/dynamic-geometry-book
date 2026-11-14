package geometrybook.gui.auth;

import geometrybook.gui.common.Theme;
import geometrybook.gui.common.TriangleCharacter;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

final class CharacterArt extends JComponent {
    CharacterArt() { setPreferredSize(new Dimension(420, 160)); setOpaque(false); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double scale = Math.min(getWidth() / 420.0, getHeight() / 160.0);
        g.translate((getWidth() - 420 * scale) / 2, (getHeight() - 160 * scale) / 2);
        g.scale(scale, scale);
        g.setStroke(new BasicStroke(2));
        Graphics2D triangle = (Graphics2D) g.create();
        triangle.translate(-1, 19);
        TriangleCharacter.draw(triangle);
        triangle.dispose();
        g.setColor(Theme.YELLOW); g.fillOval(150, 20, 110, 110); g.setColor(Theme.GREEN); g.drawOval(150, 20, 110, 110);
        g.setColor(Theme.LAVENDER); g.fillRoundRect(300, 25, 100, 100, 22, 22); g.setColor(Theme.GREEN); g.drawRoundRect(300, 25, 100, 100, 22, 22);
        TriangleCharacter.drawFace(g, 205, 73); TriangleCharacter.drawFace(g, 350, 73);
        g.dispose();
    }
}
