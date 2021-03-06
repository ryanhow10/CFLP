/**
 * This program solves the Capacitated Facility Location Problem given the necessary arguments (see README.md)
 * 
 * @author ryanhow
 *
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import gurobi.*;

public class CFLP {
	
	private final static Logger LOGGER = Logger.getLogger(CFLP.class.getName());
	
	//Sets
	private static int K = 0; //Set of commodities/products
	private static int I = 0; //Set of production plant
	private static int J = 0; //Set of potential candidate facility locations
	private static int R = 0; //Set of customers

	//Parameters
	private static List<List<Integer>> drk = new ArrayList<>(); //Demand of product k for customer r
	private static List<List<Integer>> pik = new ArrayList<>(); //Capacity of product k for plant i
	private static List<Integer> qj_min = new ArrayList<>(); //Minimum activity level for facility j
	private static List<Integer> qj_max = new ArrayList<>(); //Maximum activity level for facility j
	private static List<Double> fj = new ArrayList<>(); //Facility fixed cost
	private static List<Double> gj = new ArrayList<>(); //Facility marginal cost
	private static List<Double> ck = new ArrayList<>(); //Unit transportation cost for product k
	private static List<List<Double>> lij = new ArrayList<>(); //Distance from plant i to facility j
	private static List<List<Double>> ljr = new ArrayList<>(); //Distance from facility j to customer r
	private static int p; //Desired number of facilities to be open
	private static boolean singleAllocation; //Single allocation or divisible demand
	
	//Decision Variables
	private static GRBVar[][][] x; //Amount of product k supplied by plant i to facility j (single allocation model)
	private static GRBVar[] z; //If facility j is open or not (both models)
	private static GRBVar[][] y; //If customer r receives supply from facility j (single allocation model)
	private static GRBVar[][][][] s; //Amount of product k supplied by plant i to facility j to customer r (divisible demand model)
	
	
	public static void main(String[] args) {		
		if(args.length != 11) {
			LOGGER.log(Level.SEVERE, "Invalid input. Please reference README.md for execution instructions.");
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
		
		addDecisionVariables(model, env);
			
		
		//Objective Function
		try {
			model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
		} catch (GRBException e) {
			LOGGER.log(Level.SEVERE, "Error setting objective function. " + e.getMessage());
			cleanup(model, env);
			return;
		}
				
		addConstraints(model, env);
		
		//Solving Model
		try {
			model.optimize();
		} catch (GRBException e) {
			LOGGER.log(Level.SEVERE, "Error optimizing model. " + e.getMessage());
			cleanup(model, env);
			return;
		}
		
		printSolution(model, env, x, y, z, s);
		
		cleanup(model, env);
		
	}
	
	/**
	 * This method initializes all the sets, parameters and decision variables. 
	 * 
	 * @param args the command line arguments
	 */
	private static void init(String[] args) {
		//Customer Demand
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

		//Plant Capacity
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

		//Facility Minimum Activity Level
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

		//Facility Maximum Activity Level
		if (!fileExists(args[3])) {
			logFileDNE(args[3]);
			return;
		}
		scanner = getScanner(args[3]);
		populateIntegerVectorParam(scanner, qj_max);

		//Facility Fixed Cost
		if (!fileExists(args[4])) {
			logFileDNE(args[4]);
			return;
		}
		scanner = getScanner(args[4]);
		populateDoubleVectorParam(scanner, fj);
		
		//Facility Marginal Cost
		if (!fileExists(args[5])) {
			logFileDNE(args[5]);
			return;
		}
		scanner = getScanner(args[5]);
		populateDoubleVectorParam(scanner, gj);

		//Product Unit Transportation Cost
		if (!fileExists(args[6])) {
			logFileDNE(args[6]);
			return;
		}
		scanner = getScanner(args[6]);
		populateDoubleVectorParam(scanner, ck);

		//Distance from Plant to Facility
		if (!fileExists(args[7])) {
			logFileDNE(args[7]);
			return;
		}
		scanner = getScanner(args[7]);
		populateDistanceMatrix(scanner, lij);

		//Distance from Facility to Customer
		if (!fileExists(args[8])) {
			logFileDNE(args[8]);
			return;
		}
		scanner = getScanner(args[8]);
		populateDistanceMatrix(scanner, ljr);

		//Desired Open Facilities
		p = Integer.parseInt(args[9]);

		//Single Allocation or Divisible Demand
		singleAllocation = args[10].equals("single") ? true : false;
		
		z = new GRBVar[J];
		if(singleAllocation) {
			x = new GRBVar[K][I][J];
			y = new GRBVar[J][R];
		} else {
			s = new GRBVar[K][I][J][R];
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
	
	/**
	 * This method adds decision variables to the model based on if the model is single allocation or divisible demand.
	 * 
	 * @param model the gurobi model
	 * @param env the gurobi environment
	 */
	private static void addDecisionVariables(GRBModel model, GRBEnv env) {
		for(int j = 0; j < J; j++) {
			try {
				z[j] = model.addVar(0, 1, fj.get(j), GRB.BINARY, "z" + j);
			} catch (GRBException e) {
				logDecisionVariableError("zj", e);
				cleanup(model, env);
				return;
			}
		}
		
		if(singleAllocation) {
			for(int k = 0; k < K; k++) {
				for(int i = 0; i < I; i++) {
					for(int j = 0; j < J; j++) {
						double transportationCost = ck.get(k) * lij.get(i).get(j);
						try {
							x[k][i][j] = model.addVar(0, GRB.INFINITY, transportationCost, GRB.CONTINUOUS, "x" + i + "," + j + "," + k);
						} catch (GRBException e) {
							logDecisionVariableError("xijk", e);
							cleanup(model, env);
							return;
						}
					}
				}
			}
					
			for(int j = 0; j < J; j++) {
				for(int r = 0; r < R; r++) {
					double totalCost = 0;
					for(int k = 0; k < K; k++) {
						double transportationCost = ck.get(k) * ljr.get(j).get(r);
						double marginalCost = gj.get(j);
						totalCost += (transportationCost + marginalCost) * drk.get(r).get(k);
					}
					try {
						y[j][r] = model.addVar(0, 1, totalCost, GRB.BINARY, "y" + j + "," + r);
					} catch (GRBException e) {
						logDecisionVariableError("yjr", e);
						cleanup(model, env);
						return;
					}
				}
			}
		} else {
			for(int k = 0; k < K; k++) {
				for(int i = 0; i < I; i++) {
					for(int j = 0; j < J; j++) {
						for(int r = 0; r < R; r++) {
							double totalDistance = lij.get(i).get(j) + ljr.get(j).get(r);
							double transportationCost = ck.get(k) * totalDistance;
							double marginalCost = gj.get(j);
							try {
								s[k][i][j][r] = model.addVar(0, GRB.INFINITY, transportationCost + marginalCost, GRB.CONTINUOUS, "s" + k + "," + i + "," + j + "," + r);
							} catch (GRBException e) {
								logDecisionVariableError("skir", e);
								cleanup(model, env);
								return;
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * This method logs decision variable errors.
	 * 
	 * @param decisionVariable the decision variable being added to the model
	 * @param e the exception
	 */
	private static void logDecisionVariableError(String decisionVariable, Exception e) {
		LOGGER.log(Level.SEVERE, "Error adding " + decisionVariable + " decision variable. " + e.getMessage());
	}
	
	/**
	 * This method cleans up the gurobi model and environment.
	 * 
	 * @param model the gurobi model
	 * @param env the gurobi env
	 */
	private static void cleanup(GRBModel model, GRBEnv env) {
		try {
			model.dispose();
			env.dispose();
		} catch(GRBException e) {
			LOGGER.log(Level.SEVERE, "Error disposing model and environment. " + e.getMessage());
		}
	}
	
	/**
	 * This method adds constraints to the model based on if the model is single allocation or divisible demand.
	 * 
	 * @param model the gurobi model
	 * @param env the gurobi environment
	 */
	private static void addConstraints(GRBModel model, GRBEnv env) {
		// Desired Open Facilities
		GRBLinExpr numberOfFacilities = new GRBLinExpr();
		for (int j = 0; j < J; j++) {
			numberOfFacilities.addTerm(1, z[j]);
		}
		try {
			model.addConstr(numberOfFacilities, GRB.EQUAL, p, "Desired number of open facilities");
		} catch (GRBException e) {
			logConstraintError("desired open facilities", e);
			cleanup(model, env);
			return;
		}
				
		if(singleAllocation) {
			//Single Allocation for Demand
			for (int r = 0; r < R; r++) {
				GRBLinExpr sumOfFacilityDoesSupply = new GRBLinExpr();
				for (int j = 0; j < J; j++) {
					sumOfFacilityDoesSupply.addTerm(1, y[j][r]);
				}
				try {
					model.addConstr(sumOfFacilityDoesSupply, GRB.EQUAL, 1, "Customer " + r + "demand");
				} catch (GRBException e) {
					logConstraintError("demand", e);
					cleanup(model, env);
					return;
				}
			}

			//Production Plant Capacity
			for (int i = 0; i < I; i++) {
				for (int k = 0; k < K; k++) {
					GRBLinExpr productFromPlant = new GRBLinExpr();
					for (int j = 0; j < J; j++) {
						productFromPlant.addTerm(1, x[k][i][j]);
					}
					try {
						model.addConstr(productFromPlant, GRB.LESS_EQUAL, pik.get(i).get(k), "Product " + k + " capacity at plant " + i);
					} catch (GRBException e) {
						logConstraintError("plant capacity", e);
						cleanup(model, env);
						return;
					}
				}
			}

			//Maximum Facility Activity Level
			for (int j = 0; j < J; j++) {
				GRBLinExpr productFromFacility = new GRBLinExpr();
				for (int r = 0; r < R; r++) {
					for (int k = 0; k < K; k++) {
						productFromFacility.addTerm(drk.get(r).get(k), y[j][r]);
					}
				}
				GRBLinExpr maxActivity = new GRBLinExpr();
				maxActivity.addTerm(qj_max.get(j), z[j]);
				try {
					model.addConstr(productFromFacility, GRB.LESS_EQUAL, maxActivity, "Facility " + j + " maximum activity level");
				} catch (GRBException e) {
					logConstraintError("facility maximum activity level", e);
					cleanup(model, env);
					return;
				}
			}

			//Minimum Facility Activity Level
			for (int j = 0; j < J; j++) {
				GRBLinExpr productFromFacility = new GRBLinExpr();
				for (int r = 0; r < R; r++) {
					for (int k = 0; k < K; k++) {
						productFromFacility.addTerm(drk.get(r).get(k), y[j][r]);
					}
				}
				GRBLinExpr minActivity = new GRBLinExpr();
				minActivity.addTerm(qj_min.get(j), z[j]);
				try {
					model.addConstr(productFromFacility, GRB.GREATER_EQUAL, minActivity, "Facility " + j + " minimum activity level");
				} catch (GRBException e) {
					logConstraintError("facility minimum activity level", e);
					cleanup(model, env);
					return;
				}
			}

			//Facility Product Flow Balance
			for (int j = 0; j < J; j++) {
				for (int k = 0; k < K; k++) {
					GRBLinExpr productIn = new GRBLinExpr();
					for (int i = 0; i < I; i++) {
						productIn.addTerm(1, x[k][i][j]);
					}

					GRBLinExpr productOut = new GRBLinExpr();
					for (int r = 0; r < R; r++) {
						productOut.addTerm(drk.get(r).get(k), y[j][r]);
					}

					try {
						model.addConstr(productIn, GRB.EQUAL, productOut, "Product " + k + " flow balance at facility " + j);
					} catch (GRBException e) {
						logConstraintError("flow balance", e);
						cleanup(model, env);
						return;
					}
				}
			}
		} else {
			//Divisible Demand
			for(int r = 0; r < R; r++) {
				for(int k = 0; k < K; k++) {
					GRBLinExpr productToCustomer = new GRBLinExpr();
					for(int i = 0; i < I; i++) {
						for(int j = 0; j < J; j++) {
							productToCustomer.addTerm(1, s[k][i][j][r]);
						}
					}
					try {
						model.addConstr(productToCustomer, GRB.EQUAL, drk.get(r).get(k), "Customer " + r + " demand");
					} catch (GRBException e) {
						logConstraintError("demand", e);
						cleanup(model, env);
						return;
					}
				}
			}
			
			//Production Plant Capacity
			for(int i = 0; i < I; i++) {
				for(int k = 0; k < K; k++) {
					GRBLinExpr productFromPlant = new GRBLinExpr();
					for(int j = 0; j < J; j++) {
						for(int r = 0; r < R; r++) {
							productFromPlant.addTerm(1, s[k][i][j][r]);
						}
					}
					try {
						model.addConstr(productFromPlant, GRB.LESS_EQUAL, pik.get(i).get(k), "Product " + k + " capacity at plant " + i);
					} catch (GRBException e) {
						logConstraintError("plant capacity", e);
						cleanup(model, env);
						return;
					}
				}
			}
			
			//Maximum Facility Activity Level
			for(int j = 0; j < J; j++) {
				GRBLinExpr productFromFacility = new GRBLinExpr();
				for(int i = 0; i < I; i++) {
					for(int r = 0; r < R; r++) {
						for(int k = 0; k < K; k++) {
							productFromFacility.addTerm(1, s[k][i][j][r]);
						}
					}
				}
				GRBLinExpr maxActivity = new GRBLinExpr();
				maxActivity.addTerm(qj_max.get(j), z[j]);
				try {
					model.addConstr(productFromFacility, GRB.LESS_EQUAL, maxActivity, "Facility " + j + " maximum activity level");
				} catch (GRBException e) {
					logConstraintError("facility maximum activity level", e);
					cleanup(model, env);
					return;
				}
			}
			
			//Minimum Facility Activity Level
			for(int j = 0; j < J; j++) {
				GRBLinExpr productFromFacility = new GRBLinExpr();
				for(int i = 0; i < I; i++) {
					for(int r = 0; r < R; r++) {
						for(int k = 0; k < K; k++) {
							productFromFacility.addTerm(1, s[k][i][j][r]);
						}
					}
				}
				GRBLinExpr minActivity = new GRBLinExpr();
				minActivity.addTerm(qj_min.get(j), z[j]);
				try {
					model.addConstr(productFromFacility, GRB.GREATER_EQUAL, minActivity, "Facility " + j + " minimum activity level");
				} catch (GRBException e) {
					logConstraintError("facility minimum activity level", e);
					cleanup(model, env);
					return;
				}
			}
		}
	}
	
	/**
	 * This method logs constraint errors.
	 * 
	 * @param constraint the constraint being added to the model
	 * @param e the exception
	 */
	private static void logConstraintError(String constraint, Exception e) {
		LOGGER.log(Level.SEVERE, "Error adding " + constraint + " constraint. " + e.getMessage());
	}
	
	/**
	 * This model prints the solution to the console.
	 * 
	 * @param model the gurobi model
	 * @param env the gurobi environment
	 * @param x the x decision variable
	 * @param y the y decision variable
	 * @param z the z decision variable
	 */
	private static void printSolution(GRBModel model, GRBEnv env, GRBVar[][][] x, GRBVar y[][], GRBVar z[], GRBVar s[][][][]) {
		System.out.println();
		System.out.println("***OPTIMAL SOLUTION***");
		System.out.println();
		
		try {
			System.out.println("Total Cost: " + model.get(GRB.DoubleAttr.ObjVal));
		} catch (GRBException e) {
			LOGGER.log(Level.SEVERE, "Error obtaining objective function value. Model is likely infeasible... see details above. " + e.getMessage());
			cleanup(model, env);
			return;
		}
		System.out.println();
		
		for(int j = 0; j < J; j++) {
			double facilityOpen;
			try {
				facilityOpen = z[j].get(GRB.DoubleAttr.X);
			} catch (GRBException e) {
				logDecisionVariableValue("zj", e);
				cleanup(model, env);
				return;
			}
			
			if(facilityOpen > 0.99) {
				System.out.println("Facility " + (j + 1) + ": Open");
			} else {
				System.out.println("Facility " + (j + 1) + ": Closed");
			}
		}
		System.out.println();
		
		if(singleAllocation) {
			for(int k = 0; k < K; k++) {
				System.out.println("Product " + (k + 1));
				
				for(int j = 0; j < J; j++) {
					if(j == 0) {
						System.out.format("%-12s%-12s", "???From/To???", "Facility " + (j + 1));
						continue;
					}
					System.out.format("%-12s", "Facility " + (j + 1));
				}
				System.out.println();
				
				for(int i = 0; i < I; i++) {
					System.out.format("%-12s", "Plant " + (i + 1));
					for(int j = 0; j < J; j++) {
						double plantToFacilityAmount;
						try {
							plantToFacilityAmount = x[k][i][j].get(GRB.DoubleAttr.X);
						} catch (GRBException e) {
							logDecisionVariableValue("xijk", e);
							cleanup(model, env);
							return;
						}
						System.out.format("%-12.2f", plantToFacilityAmount);
					}
					System.out.println();
				}
				System.out.println();
			}
			
			for(int k = 0; k < K; k++) {
				System.out.println("Product " + (k + 1));
				
				for(int r = 0; r < R; r++) {
					if(r == 0) {
						System.out.format("%-12s%-12s", "???From/To???", "Customer " + (r + 1));
						continue;
					}
					System.out.format("%-12s", "Customer " + (r + 1));
				}
				System.out.println();
				
				for(int j = 0; j < J; j++) {
					System.out.format("%-12s", "Facility " + (j + 1));
					for(int r = 0; r < R; r++) {
						double demandFromFacility;
						try {
							demandFromFacility = y[j][r].get(GRB.DoubleAttr.X);
						} catch (GRBException e) {
							logDecisionVariableValue("yjr", e);
							cleanup(model, env);
							return;
						}
						System.out.format("%-12d", demandFromFacility == 1 ? drk.get(r).get(k) : 0);
					}
					System.out.println();
				}
				
				System.out.println();
			}
		} else {
			for(int k = 0; k < K; k++) {
				System.out.println("Product " + (k + 1));
				for(int i = 0; i < I; i++) {
					for(int j = 0; j < J; j++) {
						for(int r = 0; r < R; r++) {
							double product;
							try {
								product = s[k][i][j][r].get(GRB.DoubleAttr.X);
							} catch (GRBException e) {
								logDecisionVariableValue("skijr", e);
								cleanup(model, env);
								return;
							}
							if(product > 0) {
								System.out.println("Plant " + (i + 1) + " ??? " + "Facility " + (j + 1) + " ??? " + "Customer " + (r + 1) + ": " + product);
							}
						}
					}
				}
				System.out.println();
			}
		}
		
	}
	
	/**
	 * This method logs errors when obtaining the decision variable values.
	 * 
	 * @param decisionVariable the decision variable being obtained
	 * @param e the exception
	 */
	private static void logDecisionVariableValue(String decisionVariable, Exception e) {
		LOGGER.log(Level.SEVERE, "Error obtaining " + decisionVariable + " decision variable value. " + e.getMessage());
	}
	
}