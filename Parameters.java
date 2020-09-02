// * Global parameters used to adjust the ACO
public class Parameters {

    // * Pheromone evaporation rate
    public static double rho = 0.5;

    // * Influence of Pheromones
    public static double alpha = 1.0;

    // * Influence of Distance
    public static double beta = 2.0;

    // * Size of ant population
    public static int nants = 4;

    // * Number of iterations to find a good solution
    public static int iterationsMax = 2000;

    // * Number of Simulations
    public static int maxSims = 5;

    // * Pheromones to be Initialized at every Vertex/ City
    public static double initPheromones = 1.0;

    // * Probability of Randomly Sleecting the next City/Vertex to visit by an Ant
    public static double pureRandSelProb = 0.01;

    // * Constant for Calculation for Pheromone Update
    public static double Q = 500;
}