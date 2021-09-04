import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class CFLP {
	
	private final static Logger LOGGER = Logger.getLogger(CFLP.class.getName());
	
	// Sets
	private static int K = 0; // Set of commodities/products
	private static int I = 0; // Set of production plant
	private static int J = 0; // Set of potential candidate facility locations
	private static int R = 0; // Set of customers

	// Parameters
	private static List<List<Integer>> drk = new ArrayList<>(); // Demand of product k for customer r
	private static List<List<Integer>> pik = new ArrayList<>(); // Capacity of product k for plant i
	private static List<Integer> qj_min = new ArrayList<>(); // Minimum activity level for facility j
	private static List<Integer> qj_max = new ArrayList<>(); // Maximum activity level for facility j
	private static List<Double> fj = new ArrayList<>(); // Facility fixed cost
	private static List<Double> gj = new ArrayList<>(); // Facility marginal cost
	private static List<Double> ck = new ArrayList<>(); // Unit transportation cost for product k
	private static List<List<Double>> lij = new ArrayList<>(); // Distance from plant i to facility j
	private static List<List<Double>> ljr = new ArrayList<>(); // Distance from facility j to customer r
	private static int p; // Desired number of facilities to be open
	private static boolean singleAllocation; // Single allocation or divisible demand
	
	public static void main(String[] args) {
		/*
		 * Arguments
		 * 1 - file with matrix of demand of product k for customer r
		 * 2 - file with matrix of capacity of product k for plant i
		 * 3 - file with minimum activity level for facilities
		 * 4 - file with maximum activity level for facilities 
		 * 5 - file with fixed cost for facilities
		 * 6 - file with marginal cost for facilities
		 * 7 - file with unit transportation cost for product k
		 * 8 - file with distances from plant to facility
		 * 9 - file with distances from facility to customer
		 * 10 - desired number of facilities to be open
		 * 11 - "single" or "divisible" 
		 */
		
		if(args.length != 11) {
			LOGGER.log(Level.SEVERE, "Invalid input. Please reference README for execution instructions.");
			return;
		}
		
		init(args);
		
		//Gurobi Environment 
		GRBEnv env;
		GRBModel model;
		try {
			env = new GRBEnv();
			model = new GRBModel(env);
		} catch (GRBException e) {
			LOGGER.log(Level.SEVERE, "Error creating gurobi enviornment and model. " + e.getMessage());
			return;
		}
		
		//Decision Variables
		GRBVar[][][] x = new GRBVar[K][I][J]; //Amount of product k supplied by plant i to facility j
		for(int k = 0; k < K; k++) {
			for(int i = 0; i < I; i++) {
				for(int j = 0; j < J; j++) {
					double transportationCost = ck.get(k) * lij.get(i).get(j);
					try {
						x[i][j][k] = model.addVar(0, GRB.INFINITY, transportationCost, GRB.CONTINUOUS, "x" + i + "," + j + "," + k);
					} catch (GRBException e) {
						LOGGER.log(Level.SEVERE, "Error creating xijk decision variables. " + e.getMessage());
						return;
					}
				}
			}
		}
		
		GRBVar[] z = new GRBVar[J]; //If facility j is open or not
		for(int j = 0; j < J; j++) {
			try {
				z[j] = model.addVar(0, 1, fj.get(j), GRB.BINARY, "z" + j);
			} catch (GRBException e) {
				LOGGER.log(Level.SEVERE, "Error creating zj decision variables. " + e.getMessage());
				return;
			}
		}
		
		GRBVar[][] y = new GRBVar[J][R]; //If customer r receives supply from facility j
		for(int j = 0; j < J; j++) {
			for(int r = 0; r < R; r++) {
				for(int k = 0; k < K; k++) {
					double transportationCost = ck.get(k) * ljr.get(j).get(r);
					try {
						y[j][r] = model.addVar(0, 1, (transportationCost + gj.get(j)) * drk.get(r).get(k), GRB.BINARY, "y" + j + "," + r);
					} catch (GRBException e) {
						LOGGER.log(Level.SEVERE, "Error creating yjr decision variables. " + e.getMessage());
						return;
					}
				}
			}
		}
			
	}
	
	/**
	 * This method initializes all the sets and parameters.
	 * 
	 * @param args the command line arguments
	 */
	private static void init(String[] args) {
		// Customer Demand
		if (!fileExists(args[0])) {
			logFileDNE(args[0]);
			return;
		}
		Scanner scanner = getScanner(args[0]);

		while (scanner.hasNextLine()) {
			R++;
			String[] rawLine = scanner.nextLine().split(",");
			List<Integer> demands = new ArrayList<>();
			for (String demand : rawLine) {
				demands.add(Integer.parseInt(demand));
			}
			drk.add(demands);
			K = demands.size();
		}

		// @TODO Remove
		System.out.println("R: " + R);
		System.out.println("K: " + K);
		System.out.println("Demand: " + Arrays.deepToString(drk.toArray()));

		// Plant Capacity
		if (!fileExists(args[1])) {
			logFileDNE(args[1]);
			return;
		}
		scanner = getScanner(args[1]);

		while (scanner.hasNextLine()) {
			I++;
			String[] rawLine = scanner.nextLine().split(",");
			List<Integer> capacities = new ArrayList<>();
			for (String capacity : rawLine) {
				capacities.add(Integer.parseInt(capacity));
			}
			pik.add(capacities);
		}

		// @TODO Remove
		System.out.println("I: " + I);
		System.out.println("Capcities: " + Arrays.deepToString(pik.toArray()));

		// Facility Minimum Activity Level
		if (!fileExists(args[2])) {
			logFileDNE(args[2]);
			return;
		}
		scanner = getScanner(args[2]);

		while (scanner.hasNextLine()) {
			String[] rawLine = scanner.nextLine().split(",");
			for (String activityLevel : rawLine) {
				qj_min.add(Integer.parseInt(activityLevel));
			}
			J = rawLine.length;
		}

		// @TODO Remove
		System.out.println("J: " + J);
		System.out.println("Min activity: " + Arrays.deepToString(qj_min.toArray()));

		// Facility Maximum Activity Level
		if (!fileExists(args[3])) {
			logFileDNE(args[3]);
			return;
		}
		scanner = getScanner(args[3]);
		populateIntegerVectorParam(scanner, qj_max);

		// @TODO Remove
		System.out.println("Max acitivity: " + Arrays.deepToString(qj_max.toArray()));

		// Facility Fixed Cost
		if (!fileExists(args[4])) {
			logFileDNE(args[4]);
			return;
		}
		scanner = getScanner(args[4]);
		populateDoubleVectorParam(scanner, fj);

		// @TODO Remove
		System.out.println("Fixed cost: " + Arrays.deepToString(fj.toArray()));

		// Facility Marginal Cost
		if (!fileExists(args[5])) {
			logFileDNE(args[5]);
			return;
		}
		scanner = getScanner(args[5]);
		populateDoubleVectorParam(scanner, gj);

		// @TODO
		System.out.println("Marginal cost: " + Arrays.deepToString(gj.toArray()));

		// Product Unit Transportation Cost
		if (!fileExists(args[6])) {
			logFileDNE(args[6]);
			return;
		}
		scanner = getScanner(args[6]);
		populateDoubleVectorParam(scanner, ck);

		// @TODO Remove
		System.out.println("Product unit transportation cost: " + Arrays.deepToString(ck.toArray()));

		// Distance from Plant to Facility
		if (!fileExists(args[7])) {
			logFileDNE(args[7]);
			return;
		}
		scanner = getScanner(args[7]);
		populateDistanceMatrix(scanner, lij);

		// @TODO Remove
		System.out.println("Distance from plant to facility: " + Arrays.deepToString(lij.toArray()));

		// Distance from Facility to Customer
		if (!fileExists(args[8])) {
			logFileDNE(args[8]);
			return;
		}
		scanner = getScanner(args[8]);
		populateDistanceMatrix(scanner, ljr);

		// @TODO Remove
		System.out.println("Distance from facility to customer: " + Arrays.deepToString(ljr.toArray()));

		// Desired Open Facilities
		p = Integer.parseInt(args[9]);

		// @TODO Remove
		System.out.println("Desired open facilities: " + p);

		// Single Allocation or Divisible Demand
		singleAllocation = args[10].equals("single") ? true : false;
		System.out.println("Single allocation: " + singleAllocation);
	}
	
	/**
	 * This method logs that the file does not exist.
	 * 
	 * @param file the file which does not exist
	 */
	private static void logFileDNE(String file) {
		LOGGER.log(Level.SEVERE, "File '" + file + "' does not exist.");
	}
	
	/**
	 * This method obtains a scanner object.
	 * 
	 * @param filePath the file path to the file to read
	 * @return the scanner object or null on error
	 */
	private static Scanner getScanner(String filePath) {
		if(filePath == null || filePath.length() == 0) {
			LOGGER.log(Level.SEVERE, "filePath provided to getScanner was null or empty.");
			return null;
		}
		try {
			return new Scanner(new File(filePath));
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting scanner for '" + filePath + "'. " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * This method checks to see if a file exists.
	 * 
	 * @param filePath the path to the file to check 
	 * @return true if the file exists, false otherwise
	 */
	private static boolean fileExists(String filePath) {
		Path pathToFile = Paths.get(filePath);
		return Files.exists(pathToFile);
	}
	
	/**
	 * This method populates the integer parameter structure.
	 * 
	 * @param scanner the scanner object reading in the file
	 * @param vectorParam the structure holding the parameter values
	 */
	private static void populateIntegerVectorParam(Scanner scanner, List<Integer> vectorParam) {
		String[] rawLine = scanner.nextLine().split(",");
		for (String value : rawLine) {
			vectorParam.add(Integer.parseInt(value));
		}
	}
	
	/**
	 * This method populates the double parameter structure.
	 * 
	 * @param scanner the scanner object reading in the file
	 * @param vectorParam the structure holding the parameter values
	 */
	private static void populateDoubleVectorParam(Scanner scanner, List<Double> vectorParam) {
		String[] rawLine = scanner.nextLine().split(",");
		for (String value : rawLine) {
			vectorParam.add(Double.parseDouble(value));
		}
	}
	
	/**
	 * This method populates the double distance matrix.
	 * 
	 * @param scanner the scanner object reading in the file
	 * @param matrixParam the structure holding the parameter values
	 */
	private static void populateDistanceMatrix(Scanner scanner, List<List<Double>> matrixParam) {
		while(scanner.hasNextLine()) {
			String[] rawLine = scanner.nextLine().split(",");
			List<Double> distances = new ArrayList<>();
			for(String distance : rawLine) {
				distances.add(Double.parseDouble(distance));
			}
			matrixParam.add(distances);
		}
	}
	
}