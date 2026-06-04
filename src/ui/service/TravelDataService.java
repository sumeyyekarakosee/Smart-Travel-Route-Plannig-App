package ui.service;

import data.FileManager;
import graph.Graph;
import model.Location;
import model.RouteEdge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * TXT dosyalarından uygulama verisini yükler ve UI için hazırlar.
 */
public class TravelDataService {

    private final FileManager fileManager;
    private Graph graph;
    private List<Location> allLocations;

    public TravelDataService() {
        this.fileManager = new FileManager();
    }

    public Graph loadGraph() {
        if (graph != null) {
            return graph;
        }

        List<Location> locations = fileManager.loadLocations("resources/locations.txt");
        List<RouteEdge> routes = fileManager.loadRoutes("resources/routes.txt");

        Graph loadedGraph = new Graph();

        for (Location location : locations) {
            loadedGraph.addLocation(location);
        }

        for (RouteEdge route : routes) {
            loadedGraph.addEdge(route);
        }

        graph = loadedGraph;
        allLocations = new ArrayList<>(locations);
        allLocations.sort(Comparator.comparingInt(Location::getId));

        return graph;
    }

    public Graph getGraph() {
        return loadGraph();
    }

    public List<Location> getAllLocations() {
        loadGraph();
        return new ArrayList<>(allLocations);
    }

    public List<Location> getDistricts() {
        List<Location> districts = new ArrayList<>();
        for (Location location : getAllLocations()) {
            if (location.getId() <= 20) {
                districts.add(location);
            }
        }
        return districts;
    }

    public List<Location> getHistoricalPlaces() {
        List<Location> historical = new ArrayList<>();
        for (Location location : getAllLocations()) {
            if (location.getId() > 20) {
                historical.add(location);
            }
        }
        return historical;
    }
}
