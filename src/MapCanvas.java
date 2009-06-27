/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Font;

/**
 *
 * @author anandi
 */
public class MapCanvas extends Canvas {
    private static final int TEXT_HEIGHT = Font.getDefaultFont().getHeight() + 10;
    private static final int MAX_TEXT_CHARS = 15; //Arbitrary. Need to refine later.
    private static final int DEFAULT_COLOR = 0;
    private static final int HIGHLIGHT_COLOR = 1;
    private static final int TEXT_Y_OFFSET = 20;

    private int height;
    private int width;

    private MapDisplay owner;
    private Image image;
    private String displayText;
    private MapItem[] items;
    private int highlightedMapItemIndex;

    public MapCanvas(MapDisplay display) {
        //This is stupid, but then, I am sleepy! All I need is a means to
        //notify the owner instance that a change in state has occured!
        owner = display;
        displayText = null;
        items = null;
        highlightedMapItemIndex = -1;
        height = getHeight();
        width = getWidth();
    }

    public void setImage(Image image) {
        if (image != null)
            this.image = image;
    }

    public void setDisplayText(String text) {
        if (text == null)
            return;
        if (text.length() > MAX_TEXT_CHARS)
            displayText = text.substring(0, MAX_TEXT_CHARS) + " ...";
        else
            displayText = text;
    }

    public void clearDisplayText() {
        displayText = null;
    }

    protected void paint(Graphics g) {
        if (image == null)
            return;
        g.drawImage(image, 0, 0, Graphics.LEFT | Graphics.TOP);
        if (displayText != null)
            paintString(g, displayText);

        if (items != null) {
            //We have items to draw...
            for (int i = 0 ; i < items.length ; i++) {
                String desc = null;
                if (items[i].getDescription().length() > 64)
                    desc = items[i].getDescription().substring(0, 61) + "...";
                else
                    desc = items[i].getDescription();
                LocationData iloc = items[i].getLocation();
                if ((i == highlightedMapItemIndex))
                    continue;
                if (isMapItemVisible(i))
                    paintTarget(g, items[i].x, items[i].y, MapCanvas.DEFAULT_COLOR);
            }

            //Draw the highlighted one last. That way, it is not overwritten.
            if (highlightedMapItemIndex >= 0)
                paintTarget(g, items[highlightedMapItemIndex].x,
                            items[highlightedMapItemIndex].y, MapCanvas.HIGHLIGHT_COLOR);
        }
    }

    protected void paintString(Graphics g, String text) {
        int oldColor = g.getColor ();
        g.setColor (0, 0, 0);
        g.drawRect (0, TEXT_Y_OFFSET, width - 1, TEXT_HEIGHT - 1);
        //g.setColor (0x40, 0x40, 0x80);
        //g.fillRect (1, 1, width - 2, TEXT_HEIGHT - 2);
        g.setColor (0x00, 0x00, 0x00);
        g.drawString (text, 4, TEXT_Y_OFFSET + 4, Graphics.LEFT | Graphics.TOP);
        g.setColor (oldColor);
    }

    protected void paintTarget(Graphics g, int x, int y, int color) {
        int radius = 4;

        int oldColor = g.getColor();
        if (color == MapCanvas.HIGHLIGHT_COLOR)
            g.setColor(255, 0, 0);
        else
            g.setColor(0, 0, 0);
        g.drawLine(x - radius, y, x + radius, y);
        g.drawLine(x, y - radius, x, y + radius);
        g.drawArc(x - radius, y - radius, 2 * radius, 2 * radius, 0, 360);
        g.setColor (oldColor);
    }

    protected void keyPressed(int keyCode) {
        int action;
        switch(keyCode) {
            case KEY_STAR:
                owner.mapCommand(MapDisplay.COMMAND_ZOOM_IN);
                break;
            case KEY_POUND:
                owner.mapCommand(MapDisplay.COMMAND_ZOOM_OUT);
                break;
            default:
                action = getGameAction(keyCode);
                switch (action) {
                    case DOWN:
                        owner.mapCommand(MapDisplay.COMMAND_SOUTH);
                        break;
                    case UP:
                        owner.mapCommand(MapDisplay.COMMAND_NORTH);
                        break;
                    case RIGHT:
                        owner.mapCommand(MapDisplay.COMMAND_EAST);
                        break;
                    case LEFT:
                        owner.mapCommand(MapDisplay.COMMAND_WEST);
                        break;
                    case FIRE:
                        owner.mapCommand(MapDisplay.COMMAND_SET_LOCATION);
                        break;
                    case 0:
                        owner.mapCommand(MapDisplay.COMMAND_SWITCH_MODE);
                    default:
                        break;
                }
                break;
        }
    }

    public void setMapItems(MapItem[] itemList) {
        items = itemList; //Even if it is null!
    }
    
    public boolean hasMapItems() {
        if ((items != null) && (items.length > 0))
            return true;
        return false;
    }

    private boolean isMapItemVisible(int index) {
        if (((items[index].x >= 0) && (items[index].x < width))
            && ((items[index].y >= 0) && (items[index].y < height)))
            return true;
        return false;
    }

    //Name is not accurate. It is called to set even the first item.
    public void highlightNextMapItem(boolean forward) {
        int i = -1;

        if (items != null) {
            if (highlightedMapItemIndex == -1) {
                i = 0;
                while ((i != items.length) && !isMapItemVisible(i))
                    i++;
                if (i == items.length)
                    i = -1;
            } else if (forward) {
                if (highlightedMapItemIndex < (items.length - 1))
                    i = highlightedMapItemIndex + 1;
                else
                    i = 0;
                while ((i != highlightedMapItemIndex) && !isMapItemVisible(i)) {
                    if (i == items.length)
                        i = 0;
                    else
                        i++;
                }
            } else {
                if (highlightedMapItemIndex > 0)
                    i = highlightedMapItemIndex - 1;
                else
                    i = items.length - 1;
                while ((i != highlightedMapItemIndex) && !isMapItemVisible(i)) {
                    if (i == 0)
                        i = items.length - 1;
                    else
                        i--;
                }
            }

            if (i == highlightedMapItemIndex)
                highlightedMapItemIndex = -1; //Can't find any!
            else
                highlightedMapItemIndex = i;
        }
    }

    public void clearMapItemHighlight() {
        highlightedMapItemIndex = -1;
    }

    public MapItem getHighlightedMapItem() {
        if (highlightedMapItemIndex == -1)
            return null;
        if (items == null)
            return null;
        if (items.length <= highlightedMapItemIndex)
            return null;
        return items[this.highlightedMapItemIndex];
    }

    public int getDescCharCount() {
        return MapCanvas.MAX_TEXT_CHARS + 4; //Allow for a ' ...'
    }
}
