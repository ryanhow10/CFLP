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

public class CFLP {
	
	private static final Logger LOGGER = Logger.getLogger(CFLP.class.getName());
	
	public static void main(String[] args) {
		/*
		 * Arguments
		 * 1 - file with matrix of demand of product k for customer r
		 * 2 - file with matrix of capacity of product k for plant i
		 * 3 - file with minimum activity level for facilities
		 * 4 - file with maximum activity level for facilities 
		 * 5 - file with fixed cost for facilities
		 * 6 - file with marginal cost for facilities
		 * 7 - file with unit transportation cost from plant to facility
		 * 8 - file with unit transportation cost from facility to customer
		 * 9 - file with distances from plant to facility
		 * 10 - file with distances from facility to customer
		 * 11 - desired number of facilities to be open
		 * 12 - "single" or "divisible" 
		 * 
		 */
		
		if(args.length != 12) {
			LOGGER.log(Level.SEVERE, "Invalid input. Please reference README for execution instructions.");
			return;
		}
		
		//Sets
		int K = 0; //Set of commodities/products
		int I = 0; //Set of production plant
		int J = 0; //Set of potential candidate facility locations
		int R = 0; //Set of customers
		
		//Parameters
		List<List<Integer>> drk = new ArrayList<>(); //Demand of product k for customer r
		List<List<Integer>> pik = new ArrayList<>(); //Capacity of product k for plant i
		
		
		//Check existence of demand file
		Path pathToDemandMatrix = Paths.get(args[0]);
		if (!Files.exists(pathToDemandMatrix)) {
			LOGGER.log(Level.SEVERE, "File '" + args[0] + "' does not exist.");
			return;
		}
		Scanner scanner = getScanner(pathToDemandMatrix.toString());
		
		while(scanner.hasNextLine()) {
			R++;
			String[] rawLine = scanner.nextLine().split(",");
			List<Integer> demands = new ArrayList<>();
			for(String demand : rawLine) {
				demands.add(Integer.parseInt(demand));
			}
			drk.add(demands);
			K = demands.size();
		}
		
		//@TODO Remove
		System.out.println("R: " + R);
		System.out.println("K: " + K);
		System.out.println("Demand: " + Arrays.deepToString(drk.toArray()));
		
		//Check existence of plant capacity file
		Path pathToPlantCapacities = Paths.get(args[1]);
		if (!Files.exists(pathToPlantCapacities)) {
			LOGGER.log(Level.SEVERE, "File '" + args[1] + "' does not exist.");
			return;
		}
		scanner = getScanner(pathToPlantCapacities.toString());
		
		while(scanner.hasNextLine()) {
			I++;
			String[] rawLine = scanner.nextLine().split(",");
			List<Integer> capacities = new ArrayList<>();
			for(String capacity : rawLine) {
				capacities.add(Integer.parseInt(capacity));
			}
			pik.add(capacities);
		}
		
		//@TODO Remove
		System.out.println("I: " + I);
		System.out.println("Capcities: " + Arrays.deepToString(pik.toArray()));
		
		
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
	
}