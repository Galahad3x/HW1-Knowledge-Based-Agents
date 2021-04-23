/**
 * @author Joel Aumedes Serrano (48051307Y)
 * @author Joel Farré Cortés (78103400T)
 **/

package apryraz.eworld;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.exit;

/**
 * This agent performs a sequence of movements, and after each
 * movement it "senses" from the evironment the resulting position
 * and then the outcome from the smell sensor, to try to locate
 * the position of Envelope
 **/
public class EnvelopeFinder {
    /**
     * The list of steps to perform
     **/
    ArrayList<Position> listOfSteps;

    /**
     * index to the next movement to perform, and total number of movements
     **/
    int idNextStep, numMovements;

    /**
     * Array of clauses that represent conclusiones obtained in the last
     * call to the inference function, but rewritten using the "past" variables
     **/
    ArrayList<VecInt> futureToPast = new ArrayList<>();

    /**
     * the current state of knowledge of the agent (what he knows about
     * every position of the world)
     **/
    EFState efstate;

    /**
     * The object that represents the interface to the Envelope World
     **/
    EnvelopeWorldEnv EnvAgent;

    /**
     * SAT solver object that stores the logical boolean formula with the rules
     * and current knowledge about not possible locations for Envelope
     **/
    ISolver solver;

    /**
     * Agent position in the world
     **/
    int agentX, agentY;

    /**
     * Dimension of the world and total size of the world (Dim^2)
     **/
    int WorldDim, WorldLinealDim;

    /**
     * ArrayList where we will save the detector answer
     */
    ArrayList<Position> impossiblesBoxes;

    /**
     * This set of variables CAN be use to mark the beginning of different sets
     * of variables in your propositional formula (but you may have more sets of
     * variables in your solution).
     **/
    int EnvelopePastOffset;
    int EnvelopeFutureOffset;
    int actualLiteral;

    /**
     * The class constructor must create the initial Boolean formula with the
     * rules of the Envelope World, initialize the variables for indicating
     * that we do not have yet any movements to perform, make the initial state.
     *
     * @param WDim the dimension of the Envelope World
     **/
    public EnvelopeFinder(int WDim) {
        WorldDim = WDim;
        WorldLinealDim = WorldDim * WorldDim;

        solver = buildGamma();
        numMovements = 0;
        idNextStep = 0;
        System.out.println("STARTING Envelope FINDER AGENT...");

        EnvelopePastOffset = 1;
        EnvelopeFutureOffset = WorldLinealDim + 2;

        efstate = new EFState(WorldDim);  // Initialize state (matrix) of knowledge with '?'
        efstate.printState();
    }

    /**
     * Store a reference to the Environment Object that will be used by the
     * agent to interact with the Envelope World, by sending messages and getting
     * answers to them. This function must be called before trying to perform any
     * steps with the agent.
     *
     * @param environment the Environment object
     **/
    public void setEnvironment(EnvelopeWorldEnv environment) {
        EnvAgent = environment;
    }

    /**
     * Load a sequence of steps to be performed by the agent. This sequence will
     * be stored in the listOfSteps ArrayList of the agent.  Steps are represented
     * as objects of the class Position.
     *
     * @param numSteps  number of steps to read from the file
     * @param stepsFile the name of the text file with the line that contains
     *                  the sequence of steps: x1,y1 x2,y2 ...  xn,yn
     **/
    public void loadListOfSteps(int numSteps, String stepsFile) {
        String[] stepsList;
        String steps = ""; // Prepare a list of movements to try with the FINDER Agent
        try {
            BufferedReader br = new BufferedReader(new FileReader(stepsFile));
            System.out.println("STEPS FILE OPENED ...");
            steps = br.readLine();
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MSG.   => Steps file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(EnvelopeFinder.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }
        stepsList = steps.split(" ");
        listOfSteps = new ArrayList<>(numSteps);
        for (int i = 0; i < numSteps; i++) {
            String[] coords = stepsList[i].split(",");
            listOfSteps.add(new Position(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
        }
        numMovements = listOfSteps.size(); // Initialization of numMovements
        idNextStep = 0;
    }

    /**
     * Returns the current state of the agent.
     *
     * @return the current state of the agent, as an object of class EFState
     **/
    public EFState getState() {
        return efstate;
    }

    /**
     * Execute the next step in the sequence of steps of the agent, and then
     * use the agent sensor to get information from the environment. In the
     * original Envelope World, this would be to use the Smelll Sensor to get
     * a binary answer, and then to update the current state according to the
     * result of the logical inferences performed by the agent with its formula.
     **/
    public void runNextStep() throws
            ContradictionException, TimeoutException {
        // Add the conclusions obtained in the previous step
        // but as clauses that use the "past" variables
        addLastFutureClausesToPastClauses();

        // Ask to move, and check whether it was successful
        // Also, record if a pirate was found at that position
        processMoveAnswer(moveToNext());

        // Next, use Detector sensor to discover new information
        processDetectorSensorAnswer(DetectsAt());

        // Perform logical consequence questions for all the positions
        // of the Envelope World
        performInferenceQuestions();
        efstate.printState();      // Print the resulting knowledge matrix
    }

    /**
     * Ask the agent to move to the next position, by sending an appropriate
     * message to the environment object. The answer returned by the environment
     * will be returned to the caller of the function.
     *
     * @return the answer message from the environment, that will tell whether the
     * movement was successful or not.
     **/
    public AMessage moveToNext() {
        Position nextPosition;

        if (idNextStep < numMovements) {
            nextPosition = listOfSteps.get(idNextStep);
            idNextStep = idNextStep + 1;
            return moveTo(nextPosition.x, nextPosition.y);
        } else {
            System.out.println("NO MORE steps to perform at agent!");
            return (new AMessage("NOMESSAGE", "", "", ""));
        }
    }

    /**
     * Use agent "actuators" to move to (x,y)
     * We simulate this by telling to the World Agent (environment)
     * that we want to move, but we need the answer from it
     * to be sure that the movement was made with success
     *
     * @param x horizontal coordinate (row) of the movement to perform
     * @param y vertical coordinate (column) of the movement to perform
     * @return returns the answer obtained from the environment object to the
     * moveto message sent
     **/
    public AMessage moveTo(int x, int y) {
        // Tell the EnvironmentAgentID that we want  to move
        AMessage msg, ans;

        msg = new AMessage("moveto", (Integer.valueOf(x)).toString(), (Integer.valueOf(y)).toString(), "");
        ans = EnvAgent.acceptMessage(msg);
        System.out.println("FINDER => moving to : (" + x + "," + y + ")");

        return ans;
    }

    /**
     * Process the answer obtained from the environment when we asked
     * to perform a movement
     *
     * @param moveans the answer given by the environment to the last move message
     **/
    public void processMoveAnswer(AMessage moveans) {
        if (moveans.getComp(0).equals("movedto")) {
            agentX = Integer.parseInt(moveans.getComp(1));
            agentY = Integer.parseInt(moveans.getComp(2));

            System.out.println("FINDER => moved to : (" + agentX + "," + agentY + ")");
        }
    }

    /**
     * Send to the environment object the question:
     * "Does the detector sense something around(agentX,agentY) ?"
     *
     * @return return the answer given by the environment
     **/
    public AMessage DetectsAt() {
        AMessage msg, ans;

        msg = new AMessage("detectsat", (Integer.valueOf(agentX)).toString(),
                (Integer.valueOf(agentY)).toString(), "");
        ans = EnvAgent.acceptMessage(msg);
        System.out.println("FINDER => detecting at : (" + agentX + "," + agentY + ")");
        return ans;
    }

    /**
     * Process the answer obtained for the query "Detects at (x,y)?"
     * by adding the appropriate evidence clause to the formula
     *
     * @param ans message obtained to the query "Detects at (x,y)?".
     *            It will a message with three fields: DetectorValue x y
     *            <p>
     *            DetectorValue must be a number that encodes all the valid readings
     *            of the sensor given the envelopes in the 3x3 square around (x,y)
     **/
    public void processDetectorSensorAnswer(AMessage ans) throws ContradictionException {

        int x = Integer.parseInt(ans.getComp(1));
        int y = Integer.parseInt(ans.getComp(2));
        String detects = ans.getComp(0);

        System.out.println("DETECTED " + detects);
        // Call your function/functions to add the evidence clauses
        // to Gamma to then be able to infer new NOT possible positions
        // This new evidences could be removed at the end of the current step,
        // if you have saved the consequences over the "past" variables (the memory
        // of the agent) and the past is consistent with the future in your Gamma
        // formula

        /**
         * Initializes all the possible states
         **/
        impossiblesBoxes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (this.EnvAgent.withinLimits(x - 1 + i, y - 1 + j)) {
                    impossiblesBoxes.add(new Position(x - 1 + i, y - 1 + j));
                }
            }
        }
        /**
         * Creates the sensor of the agent.
         * Sensor value:
         * 1 -> Positions: {(x+1, y-1), (x+1, y), (x+1, y)}
         * 2 -> Positions: {(x-1, y+1), (x, y+1), (x+1, y+1)}
         * 3 -> Positions: {(x-1, y-1), (x-1, y), (x-1, y)}
         * 4 -> Positions: {(x-1, y-1), (x, y-1), (x+1, y-1)}
         **/
        for (int i = 0; i < detects.length(); i++) {
            if (Character.getNumericValue(detects.charAt(i)) == 1) {
                for (int j = 0; j < 3; j++) {
                    for (Position pos : impossiblesBoxes) {
                        if (pos.x == x + 1 && pos.y == y - 1 + j) {
                            impossiblesBoxes.remove(pos);
                            break;
                        }
                    }
                }
            } else if (Character.getNumericValue(detects.charAt(i)) == 2) {
                for (int ii = 0; ii < 3; ii++) {
                    for (Position pos : impossiblesBoxes) {
                        if (pos.x == x - 1 + ii && pos.y == y + 1) {
                            impossiblesBoxes.remove(pos);
                            break;
                        }
                    }
                }
            } else if (Character.getNumericValue(detects.charAt(i)) == 3) {
                for (int j = 0; j < 3; j++) {
                    for (Position pos : impossiblesBoxes) {
                        if (pos.x == x - 1 && pos.y == y - 1 + j) {
                            impossiblesBoxes.remove(pos);
                            break;
                        }
                    }
                }
            } else if (Character.getNumericValue(detects.charAt(i)) == 4) {
                for (int ii = 0; ii < 3; ii++) {
                    for (Position pos : impossiblesBoxes) {
                        if (pos.x == x - 1 + ii && pos.y == y - 1) {
                            impossiblesBoxes.remove(pos);
                            break;
                        }
                    }
                }
            } else if (Character.getNumericValue(detects.charAt(i)) == 5) {
                for (Position pos : impossiblesBoxes) {
                    if (pos.x == x && pos.y == y) {
                        impossiblesBoxes.remove(pos);
                        break;
                    }
                }
            }
        }
        if (!detects.contains("1") || !detects.contains("2")) {
            if (EnvAgent.withinLimits(x + 1, y + 1)) {
                impossiblesBoxes.add(new Position(x + 1, y + 1));
            }
        }
        if (!detects.contains("1") || !detects.contains("4")) {
            if (EnvAgent.withinLimits(x + 1, y - 1)) {
                impossiblesBoxes.add(new Position(x + 1, y - 1));
            }
        }
        if (!detects.contains("3") || !detects.contains("2")) {
            if (EnvAgent.withinLimits(x - 1, y + 1)) {
                impossiblesBoxes.add(new Position(x - 1, y + 1));
            }
        }
        if (!detects.contains("3") || !detects.contains("4")) {
            if (EnvAgent.withinLimits(x - 1, y - 1)) {
                impossiblesBoxes.add(new Position(x - 1, y - 1));
            }
        }
        //Impossibles es les posicions on SEGUR que no hi ha sobre
        for (Position pos : impossiblesBoxes) {
            int variableEnFutur = coordToLineal(pos.x, pos.y, EnvelopeFutureOffset);
            VecInt clausula = new VecInt();
            clausula.insertFirst(-variableEnFutur); // -e x,y t+1
            solver.addClause(clausula);
        }
    }

    /**
     * This function should add all the clauses stored in the list
     * futureToPast to the formula stored in solver.
     * Use the function addClause( VecInt ) to add each clause to the solver
     **/
    public void addLastFutureClausesToPastClauses() throws ContradictionException {
        for (VecInt vec : futureToPast) {
            solver.addClause(vec);
        }
        futureToPast = new ArrayList<>();
    }

    /**
     * This function should check, using the future variables related
     * to possible positions of Envelope, whether it is a logical consequence
     * that an envelope is NOT at certain positions. This should be checked for all the
     * positions of the Envelope World.
     * The logical consequences obtained, should be then stored in the futureToPast list
     * but using the variables corresponding to the "past" variables of the same positions
     * <p>
     * An efficient version of this function should try to not add to the futureToPast
     * conclusions that were already added in previous steps, although this will not produce
     * any bad functioning in the reasoning process with the formula.
     **/
    public void performInferenceQuestions() throws TimeoutException {
        for (int i = 1; i < EnvAgent.WorldDim + 1; i++) {
            for (int j = 1; j < EnvAgent.WorldDim + 1; j++) {
                int variableEnFutur = coordToLineal(i, j, EnvelopeFutureOffset);
                int variableEnPassat = coordToLineal(i, j, EnvelopePastOffset);

                VecInt variableNegative = new VecInt();
                variableNegative.insertFirst(variableEnFutur); // e x,y t+1

                if (!(solver.isSatisfiable(variableNegative))) {
                    // Add conclusion to list, but rewritten with respect to "past" variables
                    VecInt concPast = new VecInt();
                    concPast.insertFirst(-(variableEnPassat)); // -e x,y t-1

                    futureToPast.add(concPast);
                    efstate.set(i, j, "X");
                }
            }
        }
    }

    /**
     * This function builds the initial logical formula of the agent and stores it
     * into the solver object.
     *
     * @return returns the solver object where the formula has been stored
     **/
    public ISolver buildGamma() {
        //Number of positions in the map, that can have envelopes or not
        int totalNumVariables = this.WorldLinealDim;

        // You must set this variable to the total number of boolean variables
        // in your formula Gamma
        // totalNumVariables = this.WorldLinealDim
        solver = SolverFactory.newDefault();
        solver.setTimeout(3600);
        solver.newVar(totalNumVariables);
        // This variable is used to generate, in a particular sequential order,
        // the variable indentifiers of all the variables
        actualLiteral = 1;

        return solver;
    }

    /**
     * Convert a coordinate pair (x,y) to the integer value  t_[x,y]
     * of variable that stores that information in the formula, using
     * offset as the initial index for that subset of position variables
     * (past and future position variables have different variables, so different
     * offset values)
     *
     * @param x      x coordinate of the position variable to encode
     * @param y      y coordinate of the position variable to encode
     * @param offset initial value for the subset of position variables
     *               (past or future subset)
     * @return the integer indentifer of the variable  b_[x,y] in the formula
     **/
    public int coordToLineal(int x, int y, int offset) {
        return ((x - 1) * WorldDim) + (y - 1) + offset;
    }
}