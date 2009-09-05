
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Form;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author anandi
 */
public class SearchInputDisplay extends DisplayModule {
    private String query;
    private String lastQuery;
    private int maxResults;
    private int startPos;

    private LocationData lastLocation;

    private TextField q;
    private Form form;
    private Command nextPageCommand;
    private Command prevPageCommand;
    private Command searchCommand;

    private SearchResults backEnd;

    public SearchInputDisplay(GeoCrawler app, SearchResults _backEnd) {
        super(app);
        query = "";
        lastQuery = "";
        resetSearchParams();

        q = new TextField("Search For: ", null, 100, TextField.ANY);

        prevPageCommand = new Command("Previous Results", Command.SCREEN, 3);
        nextPageCommand = new Command("Next Results", Command.SCREEN, 2);
        searchCommand = new Command("Search", Command.SCREEN, 1);

        form = new Form("Search");
        form.append(q);
        form.addCommand(this.getBackCommand());
        form.addCommand(searchCommand);

        form.setCommandListener(this);

        backEnd = _backEnd;
        lastLocation = null;
    }

    private void resetSearchParams() {
        maxResults = 10;
        startPos = 1;
    }

    private void kickOff() {
        //Tell the background thread to do the search.
        if (backEnd == null) {
            app.showError("Internal error. The query backend is not registered.", previousState);
            return;
        }

        backEnd.setQuery(query);
        backEnd.setResults(maxResults);
        backEnd.setStart(startPos);
        backEnd.setRefreshMode(MapItemSource.REFRESH_REQUESTED);
        app.getMapDisplay().refreshMapItems();
    }

    public void display(int prevState) {
        //Set the GPS location fields.
        if ((prevState != GeoCrawler.STATE_SEARCH)
            && (prevState != GeoCrawler.STATE_ERROR))
            this.previousState = prevState;

        boolean locationChanged = true;
        if ((lastLocation != null) && (app.getCurrentLocation().equals(lastLocation)))
            locationChanged = false;
        
        form.removeCommand(nextPageCommand);
        form.removeCommand(prevPageCommand);

        if ((startPos < 250) && (query.length() > 0) && !locationChanged)
            form.addCommand(nextPageCommand);
        if (startPos > 1)
            form.addCommand(prevPageCommand);
        lastQuery = query;
        q.setString(query);
        app.getDisplay().setCurrent(form);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == this.getBackCommand()) {
            app.handleNextState(previousState);
            return;
        }

        query = q.getString();
        if (query.length() == 0) {
            app.showError("Please enter a query term", GeoCrawler.STATE_SEARCH);
            return;
        }

        if (!query.equals(lastQuery)) {
            resetSearchParams();
        } else {
            if (c == nextPageCommand)
                startPos += 10;
            else if (c == prevPageCommand)
                startPos -= 10;
            else
                resetSearchParams();
        }

        lastLocation = app.getCurrentLocation();
        kickOff();
        app.handleNextState(previousState);
    }
}
