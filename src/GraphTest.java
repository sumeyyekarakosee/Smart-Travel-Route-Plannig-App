import data.FileManager;
import graph.Graph;

import model.Location;
import model.RouteEdge;

import java.util.List;

public class GraphTest {

    public static void main(String[] args) {

        FileManager fileManager = new FileManager();

        List<Location> locations =
                fileManager.loadLocations("resources/locations.txt");

        List<RouteEdge> routes =
                fileManager.loadRoutes("resources/routes.txt");

        System.out.println("===== TXT TEST =====");

        System.out.println(locations);

        System.out.println(routes);


        Graph graph = new Graph();

        for (Location location : locations) {

            graph.addLocation(location);
        }

        for (RouteEdge route : routes) {

            graph.addEdge(route);
        }

        System.out.println("===== GRAPH TEST =====");

        System.out.println(graph.getNeighbors(1));

    }
}