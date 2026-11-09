package geometrybook.gui.common;

import geometrybook.domain.LessonTopic;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import javax.swing.JComponent;


public final class ShapeCharacter extends JComponent {
    private final LessonTopic topic;

    /** @param topic 표시할 도형의 레슨 */
    public ShapeCharacter(LessonTopic topic) {
        this.topic = Objects.requireNonNull(topic);
        setPreferredSize(new Dimension(150, 130));
        setOpaque(false);
        setToolTipText(topic.title() + " 캐릭터");
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Insets insets = getInsets();
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
        if (width <= 0 || height <= 0) return;

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double scale = Math.min(width / 132.0, height / 115.0);
        g.translate(insets.left + (width - 132 * scale) / 2,
                insets.top + (height - 115 * scale) / 2);
        g.scale(scale, scale);
        if (topic == LessonTopic.TRIANGLE) {
            TriangleCharacter.draw(g);
        } else {
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Shape shape = outline();
            g.setColor(fillColor());
            g.fill(shape);
            g.setColor(Theme.GREEN);
            g.draw(shape);
            TriangleCharacter.drawFace(g, 66, 55);
        }
        g.dispose();
    }

    private Shape outline() {
        switch (topic) {
            case SQUARE:
                return new Rectangle2D.Double(16, 7, 100, 100);
            case RECTANGLE:
                return new Rectangle2D.Double(2, 17, 128, 80);
            case RHOMBUS:
                return polygon(66, 3, 130, 57, 66, 111, 2, 57);
            case TRAPEZOID:
                return polygon(30, 12, 102, 12, 130, 102, 2, 102);
            case PARALLELOGRAM:
                return polygon(30, 12, 130, 12, 102, 102, 2, 102);
            case CIRCLE:
                return new Ellipse2D.Double(12, 3, 108, 108);
            default:
                throw new IllegalArgumentException("지원하지 않는 도형: " + topic);
        }
    }

    private Color fillColor() {
        switch (topic) {
            case SQUARE: return Theme.LAVENDER;
            case RECTANGLE: return new Color(0xCBE8D6);
            case RHOMBUS: return new Color(0xF5D9E3);
            case TRAPEZOID: return new Color(0xFCE0C3);
            case PARALLELOGRAM: return new Color(0xD0E6F3);
            default: return Theme.YELLOW;
        }
    }

    private static Path2D polygon(double... coordinates) {
        Path2D path = new Path2D.Double();
        path.moveTo(coordinates[0], coordinates[1]);
        for (int i = 2; i < coordinates.length; i += 2) {
            path.lineTo(coordinates[i], coordinates[i + 1]);
        }
        path.closePath();
        return path;
    }
}
