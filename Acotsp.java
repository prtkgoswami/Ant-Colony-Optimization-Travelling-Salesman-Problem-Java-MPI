/*
    * Topic : Travelling Salesman Problem using Ant Colony Optimization
    * Course : CSS534 - Parallel Programming in Grid and Cloud
    * By : Pratik Goswami
    * Parallelized Using : Java MPI
*/

import java.io.*;
import java.lang.*;
import mpi.*;

/*
    * The Main Driver Class for Ant Colony Optimization of Travelling Salesman Problem
*/
public class Acotsp {
    public static int debug = 0;        // * Debug Flag
    private static double GRAPH[][];    // * Distance Matrix
    public static int ncities;          // * # of Cities
    static Acotsp driver;               // * Object of the Driver Class
    private static int nprocs = 1;      // * # of processes/ Computing Nodes
    public static City cities[];        // * Array of City Objects
    public static Ant ants[];           // * Array of Ant Objects
    public static int rank;             // * MPI Rank

    /*
        * Main Function that acts like the driver class for the program
    */
    public static void main(String args[]) throws MPIException {
        String filename;            // * Name of the Input File
        long startTime = 0L;        // * Start Time of the program
        long endTime = 0L;          // * Endtime of the Program
        int niter = 1;              // * # of Iterations

        int root = 0;               // * Rank of the root MPI Node

        // * Variables to be used for Message Transfer using Java MPI
        double bestTripDist[];      // * Object to hold the Shortest Trip Distance of a single Ant
        int bestTrip[];             // * Onject to store the Path taken by a single Ant for the Shortest Distance
        double allBestDist[];       // * Collection of the Shortest Trip Distance traversed from all the MPI Nodes
        int allBestTrip[][];        // * Collection of the Shortest Path taken by a single Ant from all the MPI Nodes

        // * Checking & Storing the Arguments passed to the program
        if (args.length < 3) {
            System.err.println("Usage :: acotsp <filename> <ncities> <#Iterations> [<Debug>]");
            System.exit(-1);
        }
        filename = args[0];
        ncities = Integer.parseInt(args[1]);
        niter = Integer.parseInt(args[2]);
        if (args.length == 3) {
            debug = Integer.parseInt(args[3]);
        }

        driver = new Acotsp();              // * Initializing the Driver class Object
        try {
            driver.readFile(filename);      // * Function to read the Input file & Initialize the Disatnce Matrix
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        if (debug == 1)
            driver.printGraph(0);           // * Printing the Initialized Distance Matrix

        MPI.Init(args);                     // * Starting Java MPI
        rank = MPI.COMM_WORLD.Rank();       // * Fetching the Rank of the current MPI Computing Node
        nprocs = MPI.COMM_WORLD.Size();     // * Getting the Number of Processes/MPI Computing Nodes

        if(rank == root){
            System.out.println("Input File :: " + filename + "\n# Processes :: " + nprocs + "\n# Cities :: "
                                + ncities + "\n# of Iterations :: " + niter + "\n");

            // * Getting the Start Time
            startTime = System.currentTimeMillis();

            // * Broadcasting the Distance Matrix to all the MPI Nodes from the Root Node
            MPI.COMM_WORLD.Bcast(GRAPH, 0, ncities*ncities, MPI.DOUBLE, root);
        }

        // * Start of Actual TSP Problem
        for(int sim = 1; sim <= Parameters.maxSims; sim++) {
            if(rank == root)
                System.out.println("Simulation #" + sim);

            bestTripDist = new double[1];
            bestTrip = new int[ncities];
            allBestDist = new double[nprocs];
            allBestTrip = new int[nprocs][ncities];

            init();     // * Initializing Cities and Ants Collection

            for(int i = 0; i < niter; i++) {
                // * Setting Up every Ant one by one
                // * The process runs for (# Ants/# Processes) so as to distribute the Ants over each Processes
                for(int j = 0; j < (Parameters.nants/nprocs); j++)
                    ants[j].setup();

                // * Move every Ant over all the Cities
                for(int j = 0; j < ncities; j++)
                    for(int k = 0; k < (Parameters.nants/nprocs); k++)
                        ants[k].moveAnt();

                // * Update the Evaporation of Phermomones from all Cities
                for(int j = 0; j < ncities; j++)
                    cities[j].evaporatePhermones();

                // * Update overall Pheromone Trails
                for(int j = 0; j < ncities; j++)
                    for(int k = 0; k < (Parameters.nants/nprocs); k++)
                        ants[k].updatePherTrails();
            }

            // * Finding the Shortest Path Traversed by all the Ants in a single Node
            if(debug == 1)
                System.out.println("[" + rank + "] Computation Completed. Finding Best Trip");
            int min = 0;
            for(int j = 1; j < (Parameters.nants/nprocs); j++){
                if(ants[min].tripDist > ants[j].tripDist)   // * Finding the index of the Ant with the minimum Distance Traversed
                    min = j;
            }
            bestTripDist[0] = ants[min].tripDist;   // * Storing the Shortest Distance Traversed
            bestTrip = ants[min].tour;              // * Storing the Shortest Path Traversed
            if(debug == 1) {
                System.out.println("[" + rank + "] Best Distance :" + bestTripDist[0] +"\n Best Trip :" + arrayToString(bestTrip));
                System.out.println("[" + rank + "] Reaching Barrier");
            }

            MPI.COMM_WORLD.Barrier();   // * Barrier Synchronization

            // * Fetching the Shortest Distance travelled by the Ants in every MPI Node
            if(rank == root){
                if(debug == 1)
                    System.out.println("[" + rank + "] Gathering Best Trip Details");

                allBestDist[0] = bestTripDist[0];
                for(int k = 1; k < nprocs; k++) {
                    if(debug == 1)
                        System.out.println("[" + rank + "] Receiving Dsitance from rank " + k);
                    MPI.COMM_WORLD.Recv(allBestDist[k], 0, 1, MPI.DOUBLE, k, 0);
                }
            }
            else{
                if(debug == 1)
                    System.out.println("[" + rank + "] Sending Dsitance to root");

                MPI.COMM_WORLD.Send(bestTripDist, 0, 1, MPI.DOUBLE, root, 0);
            }

            // * Fetching the SHortest Path travelled by the Ants in every MPI Node
            if(rank == root){
                if(debug == 1)
                    System.out.println("[" + rank + "] Gathering Best Trip Details");

                allBestTrip[0] = bestTrip;
                for(int k = 1; k < nprocs; k++) {
                    if(debug == 1)
                        System.out.println("[" + rank + "] Receiving trip from rank " + k);
                    MPI.COMM_WORLD.Recv(allBestTrip[k], 0, ncities, MPI.INT, k, 0);
                }
            }
            else{
                if(debug == 1)
                    System.out.println("[" + rank + "] Sending trip to root");

                MPI.COMM_WORLD.Send(bestTrip, 0, ncities, MPI.INT, root, 0);
            }

            if(rank == root) {
                if(debug == 1)
                    System.out.println("[" + rank + "] Mining Minimum");

                // * Finding the Minimum Distance Traversed and the Path Taken
                int m = 0;
                for(int i = 0; i < nprocs; i++) {
                    if(allBestDist[m] < allBestDist[i])
                        m = i;
                }
                // * Printing the minimum Distance
                System.out.printf("Best Trip Distance :: %.2f\n", allBestDist[m]);
                System.out.print("Best Trip :: ");
                for(int i = 0; i < ncities; i++)
                    System.out.print(allBestTrip[m][i] + " ");
                System.out.println();
                System.out.println();
            }
        }

        if(rank == root){
            // * Getting the End Time
            endTime = System.currentTimeMillis();

            // * Printing the Elapsed Time
            System.out.printf("Elapsed Time :: %d ms\n", (endTime - startTime));
        }

        // * Turning off MPI
        MPI.Finalize();
    }

    /*
        * printGraph()
        * @ desc : Print the graph created
        * @ param : The Rank of the MPI Node calling to print
    */
    public void printGraph(int rank) {
        System.out.println("Printing for rank " + rank);
        for(int i = 0; i < ncities; i++) {
            for(int j = 0; j < ncities; j++) {
                System.out.print(GRAPH[i][j] + "\t");
            }
            System.out.println();
        }
    }

    /*
        * readFile(Fielname)
        * @ desc : Read File and generate Graph
        * @ param : Name of the Input File
    */
    public void readFile(String file) throws NumberFormatException, IOException {
        BufferedReader br = null;
        GRAPH = new double[ncities][ncities];
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while((line = br.readLine()) != null) {
                String[] parts = line.split("=");
                int x = Integer.parseInt(parts[0]);
                String part2 = parts[1];
                String[] subparts = part2.split(",");
                for(int i = 0; i < subparts.length; i++) {
                    String[] neighbor = subparts[i].split(":");
                    int vertex = Integer.parseInt(neighbor[0]);
                    double dist = Double.parseDouble(neighbor[1]);
                    GRAPH[x-1][vertex-1] = dist;
                    GRAPH[vertex-1][x-1] = dist;
                }
            }
        } catch(IOException e){
            e.printStackTrace();
            System.exit(-1);
        } finally {
            if(br!=null)
                br.close();
        }
    }

    /*
        * init()
        * @ desc : To Initialize the COllection go City and Ant Objects
    */
    public static void init() {
        // * Initializing the Collection of City Objects
        cities = new City[ncities];
        for(int i = 0; i < ncities; i++)
            cities[i] = new City(GRAPH[i]);

        // * Initializing the Collection of Ant Objects
        ants = new Ant[Parameters.nants];
        for(int i = 0; i < (Parameters.nants/nprocs); i++)
            ants[i] = new Ant(i);
    }

    /*
        * arrayToString()
        * @ desc : Conversion of Array to String
        * @ param : Array to be converted
    */
    public static String arrayToString(int arr[]) {
        String s = new String();
        for(int i = 0; i < ncities; i++) {
            s += arr[i];
            if(i < arr.length-1)
                s += ", ";
        }
        return s;
    }

}