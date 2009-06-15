/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 *
 * @author anandi
 */
public class MapCanvas extends Canvas {
    private MapDisplay owner;
    private Image image;

    public MapCanvas(MapDisplay display) {
        //This is stupid, but then, I am sleepy! All I need is a means to
        //notify the owner instance that a change in state has occured!
        owner = display;
    }

    public void setImage(Image image) {
        if (image != null)
            this.image = image;
    }

    protected void paint(Graphics g) {
        System.err.println("Painting map canvas.");
        if (image != null)
            g.drawImage(image, 0, 0, Graphics.LEFT | Graphics.TOP);
    }

    protected void keyPressed(int keyCode) {
        int action;
        switch(keyCode) {
            case KEY_STAR:
                System.err.println("Got a '*' key press. Zooming in");
                owner.mapCommand(MapDisplay.COMMAND_ZOOM_IN);
                break;
            case KEY_POUND:
                System.err.println("Got a '#' key press. Zooming out");
                owner.mapCommand(MapDisplay.COMMAND_ZOOM_OUT);
                break;
            default:
                action = getGameAction(keyCode);
                switch (action) {
                    case DOWN:
                        System.err.println("Got a 'Down' key press. Going South");
                        owner.mapCommand(MapDisplay.COMMAND_SOUTH);
                        break;
                    case UP:
                        System.err.println("Got a 'Up' key press. Going North");
                        owner.mapCommand(MapDisplay.COMMAND_NORTH);
                        break;
                    case RIGHT:
                        System.err.println("Got a 'Right' key press. Going East");
                        owner.mapCommand(MapDisplay.COMMAND_EAST);
                        break;
                    case LEFT:
                        System.err.println("Got a 'Left' key press. Going West");
                        owner.mapCommand(MapDisplay.COMMAND_WEST);
                        break;
                    case FIRE:
                        System.err.println("Got a 'Fire' key press. Setting location");
                        owner.mapCommand(MapDisplay.COMMAND_SET_LOCATION);
                        break;
                    default:
                        break;
                }
                break;
        }
    }
}
