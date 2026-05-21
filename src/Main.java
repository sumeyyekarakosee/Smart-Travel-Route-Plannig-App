import data.FileManager;
import graph.Graph;

import model.Location;
import model.RouteEdge;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        FileManager fileManager = new FileManager();

        List<Location> locations =
                fileManager.loadLocations("resources/locations.txt");

        List<RouteEdge> routes =
                fileManager.loadRoutes("resources/routes.txt");


        Graph graph = new Graph();

        for (Location location : locations) {
            graph.addLocation(location);
        }

        for (RouteEdge route : routes) {
            graph.addEdge(route);
        }


    }
}