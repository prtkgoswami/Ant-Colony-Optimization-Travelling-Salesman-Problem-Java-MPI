import java.io.*;
import java.lang.*;
import java.util.Random;

/*
    * Ant Object Class
*/
public class Ant {
    private boolean visited[];      // * Boolean Collection of Visited Cities
    public int tour[];              // * Collection of Cities visited
    private double probCity[];      // * Collection of Probabilities of selecting the Next City
    public double tripDist = 0.0;   // * Total Distance Traversed
    private int ncities;            // * Number of cities
    private int currCity;           // * Current City Location of the Ant
    private int visitCount = 0;     // * # of Cities Visited
    private Random rand;            // * Random Object
    private int antIndex;           // * Index of the Ant

    /*
        * Ant()
        * @ desc : Constructor Method of the Ant
        * @ param : The Ant Index
    */
    public Ant(int indx) {
        super();
        ncities = Acotsp.ncities;
        visited = new boolean[ncities];
        tour = new int[ncities];
        probCity = new double[ncities];
        antIndex = indx;
    }

    /*
        * setup()
        * @desc : Setting up the Ant for the first time
    */
    public void setup() {
        clearVisits();

        rand = new Random();
        currCity = rand.nextInt() % ncities;    // * Selecting a Random City to Start from
        if(currCity < 0) currCity *= -1;        // * Converting Negative indexes into Positive

        visited[currCity] = true;               // * Set Current City visit flag to True
        visitCount = 0;

        visitCity(currCity);                    // * Visit the Selected City
    }

    /*
        * moveAnt()
        * @ desc : Function to Move the Ant
    */
    public void moveAnt() {
        // * If all cities are visited visit the source to complete the round trip
        if ( visitCount == ncities ) {
            visitCity(tour[0]);
            visitCount = 0;
        }
        else
            visitCity(selectNextCity());
    }

    /*
        * selectNextCity()
        * @ desc : Function to select the Next City either Randomly or using the Probability Function
        * @ return : The City to Visit Next
    */
    private int selectNextCity( ) {
        int nextCity = -1;
        // * Randomly Secting the next City on a Random Probability
        if (rand.nextDouble() < Parameters.pureRandSelProb) {
            int t = rand.nextInt(ncities - currCity);           // * Finding a Random City
            if ( ( nextCity = selectNthCity( t ) ) != -1 ) {    // * Checking if the Random City is visited
                if(Acotsp.debug == 1)
                    System.out.println("[" + Acotsp.rank + "]DEBUG :: Ant[" + antIndex + "] chose " + nextCity + " randomly." );
                return nextCity;
            }
        }
        // * Calculate the Probabilities for a visit to Each City
        edgeSelProb( );

        // * Randomly selecting the next city according to Probability
        double r = rand.nextDouble();
        double tot = 0;
        for (int i = 0; i < ncities; i++) {
            tot += probCity[i];
            if (tot >= r) {
                if(Acotsp.debug == 1)
                    System.out.println( "[" + Acotsp.rank + "]DEBUG :: Ant[" + antIndex + "] chose " + i + " with probs = " + r );
                return i;
            }
        }
        if ( ( nextCity = selectNthCity( 0 ) ) != -1 ) {
            if(Acotsp.debug == 1)
                System.out.println( "[" + Acotsp.rank + "]DEBUG :: Ant[" + antIndex + "] chose " + nextCity + " as the 1st available city, because tot = " + tot + " < r = " + r );
            return nextCity;
        }
        if(Acotsp.debug == 1) {
            System.out.println( "[" + Acotsp.rank + "]DEBUG :: Ant[" + antIndex + "]: Not supposed to get here" );
            System.out.println( "[" + Acotsp.rank + "]DEBUG :: Current City = " + currCity );
            int i = 0; int j = 0;
            while(i < visitCount && j < ncities){
                System.out.println( "[" + Acotsp.rank + "]DEBUG :: tour[" + i + "] = " + tour[i] );
                System.out.println( "[" + Acotsp.rank + "]DEBUG :: visited[" + j + "] = " + visited[j] );
            }
        }
        throw new RuntimeException("Ant[" + antIndex + "]: Not supposed to get here" );
    }

    /*
        * selectNthCity()
        * @ desc : Selection of the nth Unvisited City
        * @ param : Nth City Index
        * @ return : The nth City or -1 if none is found
    */
    private int selectNthCity(int nCity) {
        int unvisitedCount = -1;
        for (int i = 0; i < ncities; i++) {
            if (!visited[i])
                unvisitedCount++;           // * Incrementing Unvisited Count
            if (unvisitedCount == nCity)    // * Returning the index when the Unvisited City count equals the nth index
                return i;
        }
        return -1;
    }

    /*
        * edgeSelProb()
        * @ desc : Initialize the Probabilities of visiting the n=Neighbor Cities
    */
    private void edgeSelProb() {
        double[] distances = (Acotsp.cities[currCity]).distances;   // * Fetching the Distance of the Neighboring Cities
        double[] pherTrails = (Acotsp.cities[currCity]).pheromones; // * Fetching the Pheromone Trails of the Neighboring Cities

        // * Calculating the Denominator for calculation of teh Probabilities
        double denominator = 0.0;
        for (int i = 0; i < ncities; i++)
            if (!visited[i])
                denominator += pow(pherTrails[i], Parameters.alpha) * pow(1.0 / distances[i], Parameters.beta);

        // * Probability Calculation and updating the Probability Collection
        for (int j = 0; j < ncities; j++) {
            if (visited[j]) {
                probCity[j] = 0.0;
            } else {
                probCity[j] = (pow(pherTrails[j], Parameters.alpha) * pow(1.0 / distances[j], Parameters.beta)) / denominator;
            }
        }
    }

    /*
        * visitCity()
        * @ desc : Visiting the Selected Next City
        * @ param : The Selected Next City
    */
    private void visitCity(int city) {
        // * Checking for if the ant in enroute to traverse all cities and has not completed it
        if (visitCount < ncities) {
            tour[visitCount++] = city;          // * Write selected city in the Ant's Tour
            visited[city] = true;               // * Check the visited flag of the selected City
        }

        // * Checking for if the Ant has left the source City
        if (visitCount > 0) {
            double[] distances = (Acotsp.cities[currCity]).distances;
            tripDist += distances[city];        // * Update the Traversed Distance
        }

        currCity = city;                        // * Updating the Current City for the Ant
    }

    /*
        * updatePherTrails()
        * @ desc : Update the Pheromone Trails for the ant
    */
    public void updatePherTrails() {
        double contribution = Parameters.Q / tripDist;                          // * Compute the Contribution
        double[] trails = (Acotsp.cities[currCity]).pheromones;                 // * Retrieve this City's Pheromone Trails
        ++visitCount;

        // * Put the Pheromone from the current to the Next City
        trails[ ( visitCount == ncities ) ? tour[0] : tour[visitCount] ] += contribution;
    }

    /*
        * clearVisits()
        * @desc : Clear Visited Array and reset tour distance
    */
    public void clearVisits() {
        for(int i = 0; i < ncities; i++)
            visited[i] = false;
        tripDist = 0.0;
    }

    /*
        * pow()
        * @ desc : An Approximate Power Function
        * @ param : The Base and the Power Numbers
        * I used an approximate power function to speed up Power as Math.pow() is slow
        * Source : https://www.reddit.com/r/gamedev/comments/n7na0/fast_approximation_to_mathpow/
        * It is 40 times faster than math.pow with an error margin of 1.7%
    */
    public static double pow(final double a, final double b) {
        // exponentiation by squaring
        double r = 1.0;
        int exp = (int) b;
        double base = a;
        while (exp != 0) {
                if ((exp & 1) != 0)
                        r *= base;
                base *= base;
                exp >>= 1;
        }
        // use the IEEE 754 trick for the fraction of the exponent
        final double b_faction = b - (int)b;
        final long tmp = Double.doubleToLongBits(a);
        final long tmp2 = (long) (b_faction * (tmp - 4606921280493453312L)) + 4606921280493453312L;
        return r * Double.longBitsToDouble(tmp2);
    }

}