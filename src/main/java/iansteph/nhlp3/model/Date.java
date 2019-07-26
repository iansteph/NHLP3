package iansteph.nhlp3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// The service calls to the NHL API endpoint will be serialized from JSON into this object. This allows for much easier access to the
// returned data, for only keeping the data that will be used (accomplished through the annotation to the class), etc.
//
// This class specifically is the representation of the inner "Date" data from the NHL Schedule API endpoint response
@JsonIgnoreProperties(ignoreUnknown = true)
public class Date {

    private List<Game> games;
    private int totalGames;

    public List<Game> getGames() {
        return games;
    }

    public void setGames(final List<Game> games) {
        this.games = games;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(final int totalGames) {
        this.totalGames = totalGames;
    }

    public String toString() {
        return "Date(totalGames=" + totalGames + ",games=" + games + ")";
    }
}
