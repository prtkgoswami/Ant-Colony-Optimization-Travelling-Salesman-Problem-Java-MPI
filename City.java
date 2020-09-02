import java.io.*;
import java.util.*;

/*
    * City Object Class
*/
public class City {
    public double distances[];      // * Collection of Distances of the Neighboring Vertices/Cities form the current City
    public double pheromones[];     // * Collection of Pheromones of the Neighboring Vertices/Cities form the current City
    private static int ncities;     // * Total Number of Cities/Vertices

    public City() {}

    /*
        * City()
        * @ desc : Initializa the City
        * @ param : The collection of the neighboring vertex distances of the current city
    */
    public City(double graph[]) {
        super();
        distances = graph;          // * Initializing the Neighbor Distances
        ncities = Acotsp.ncities;   // * Fetching Number of Cities from the Driver Class

        reset();                    // * Reset the Pheromone Concentrations for the Current City Neighbors
    }

    /*
        * reset()
        * @ desc : Reset the Pheromone Concentrations
    */
    public void reset() {
        pheromones = new double[ncities];
        for(int i = 0; i < ncities; i++)
                pheromones[i] = Parameters.initPheromones;
    }

    /*
        * evaporatePheromones()
        * @ desc : Calculation of the Pheromone Evaporation
    */
    public void evaporatePhermones() {
        for (int i = 0; i < pheromones.length; i++)
            pheromones[i] *= Parameters.rho;
    }
}