package logic;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import view.Gomme;



/**
 * class used to represent plan. It will provide for a given set of results an action to perform in each result
 */
class Plans{
	ArrayList<Result> results;
	ArrayList<ArrayList<String>> actions;
	
	/**
	 * construct an empty plan
	 */
	public Plans() {
		this.results = new ArrayList<Result>();
		this.actions = new ArrayList<ArrayList<String>>();
	}
	
	/**
	 * add a new pair of belief-state and corresponding (equivalent) actions 
	 * @param beliefBeliefState the belief state to add
	 * @param action a list of alternative actions to perform. Only one of them is chosen but their results should be similar
	 */
	public void addPlan(Result beliefBeliefState, ArrayList<String> action) {
		this.results.add(beliefBeliefState);
		this.actions.add(action);
	}
	
	/**
	 * return the number of belief-states/actions pairs
	 * @return the number of belief-states/actions pairs
	 */
	public int size() {
		return this.results.size();
	}
	
	/**
	 * return one of the belief-state of the plan
	 * @param index index of the belief-state
	 * @return the belief-state corresponding to the index
	 */
	public Result getResult(int index) {
		return this.results.get(index);
	}
	
	/**
	 * return the list of actions performed for a given belief-state
	 * @param index index of the belief-state
	 * @return the set of actions to perform for the belief-state corresponding to the index
	 */
	public ArrayList<String> getAction(int index){
		return this.actions.get(index);
	}
}

/**
 * class used to represent a transition function i.e., a set of possible belief states the agent may be in after performing an action
 */
class Result{
	private ArrayList<BeliefState> beliefStates;

	/**
	 * construct a new result
	 * @param states the set of states corresponding to the new belief state
	 */
	public Result(ArrayList<BeliefState> states) {
		this.beliefStates = states;
	}

	/**
	 * returns the number of belief states
	 * @return the number of belief states
	 */
	public int size() {
		return this.beliefStates.size();
	}

	/**
	 * return one of the belief state
	 * @param index the index of the belief state to return
	 * @return the belief state to return
	 */
	public BeliefState getBeliefState(int index) {
		return this.beliefStates.get(index);
	}
	
	/**
	 * return the list of belief-states
	 * @return the list of belief-states
	 */
	public ArrayList<BeliefState> getBeliefStates(){
		return this.beliefStates;
	}
}


/**
 * class implement the AI to choose the next move of the Pacman
 */
public class AI{

	// We want our memory to work on the most general case as possible, and since results can be much more diverse than beliefstates, we use belifstates as keys to our memory
	public static TreeMap<BeliefState,Float> memory = new TreeMap<BeliefState, Float>();
	
	// Parameters to fiddle with

	static final int memory_refresh_rate = 300;
	static final int maxdepth = 2;
	static final String aggregate_method = "mean";
	static final float death_penatly = 4000;
	static final float turnback_penalty = 150;
	static final float max_expand = 1000;
	static final float gom_distance_weight = 10;
	static final float ghost_sight_reward = 5;
	static final float double_ghost_sight_reward = 100;
	static final float move_incentive = 1;
	static final boolean virtual_ghost = false;


	static int number_of_moves = 0;

	public static int manhatan_distance(Position p1, Position p2){
		return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
	}

	// This method will define what function we use in order to compute the values from several children (originally leaves)
	public static float aggregateValues(ArrayList<Float> values) {
		if(values.isEmpty())return 0;
		// Mean version
		if (Objects.equals(aggregate_method, "mean")) {
			float sum= 0;
			for(Float value : values) {
				sum+=value;
			}
            return sum/values.size();
		}
		
		else if (Objects.equals(aggregate_method, "min")){
			return Collections.min(values);
		}
		return 0;
	}

	// Checks if two directions are opposite
	// Is used as a local heuristic to penalize U-turns
	public static boolean opposite_direction(String direction1, char direction2){
        return (Objects.equals(direction1, "UP") && direction2== 'D') ||
                (Objects.equals(direction1, "DOWN") && direction2 == 'U') ||
                (Objects.equals(direction1, "RIGHT") && direction2 == 'L') ||
                (Objects.equals(direction1, "LEFT") && direction2 == 'R');
    }

	public static boolean opposite_direction(char direction1, char direction2){
		return (Objects.equals(direction1, "U") && direction2== 'D') ||
				(Objects.equals(direction1, "D") && direction2 == 'U') ||
				(Objects.equals(direction1, "R") && direction2 == 'L') ||
				(Objects.equals(direction1, "L") && direction2 == 'R');
	}

	// Checks for the distance to the nearest existing gom
	// Is used in the heuristic to incentivize going towards goms
	public static int nearest_gom_distance(BeliefState bstate){
		Position pacmanpos = bstate.getPacmanPosition();
		int pacmanx = pacmanpos.x; int pacmany = pacmanpos.y;

		char[][] map = bstate.getMap();

		int max_search_distance = 25;
		int i; int k; int j;
		for(k=1; k<= max_search_distance; k++){
			for(i=-k; i<=k; i++){
				for(j=-k; j<=k; j++){
					if(pacmanx + i >= 25 ||
					pacmanx + i < 0 ||
					pacmany + j >= 25 ||
					pacmany +j < 0)continue;
					if( map[pacmanx + i][pacmany + j] =='.') {
						return Math.abs(i) + Math.abs(j);
					}
				}
			}
		}
		System.out.println("gom not found. Move "+ number_of_moves);
		return 2 * max_search_distance;
	}

	// Tells us if pacman currently sees a ghost
	public static int nb_ghosts_in_sight(BeliefState bstate){
		int nbseen = 0;
		for(int i=0; i< bstate.getNbrOfGhost(); i++){
			if (bstate.getGhostPositions(i).size()==1) nbseen++;
		}
		return nbseen;
	}

	public static float heuristic (BeliefState bstate, BeliefState original_bstate) {
		if (bstate.getLife()==0) return 0; //Float.MIN_VALUE;

		float hscore = 0;

		hscore+= bstate.getScore(); // Most important and basic part : add the score

		if (bstate.getLife()==0) hscore -= death_penatly; // substract points if dead

		// Since our AI plays better (and is faster) when it has information on where the ghosts are, we give a bonus for each ghost pacman sees
		int ghosts_in_sight = nb_ghosts_in_sight(bstate);
		if(ghosts_in_sight==1) hscore += ghost_sight_reward;
		else if (ghosts_in_sight==2) hscore += double_ghost_sight_reward;

		int current_nbgoms = bstate.getNbrOfGommes();

		if(current_nbgoms==0) hscore+= 1000; // Big bonus if the map is finished
		else if(current_nbgoms < original_bstate.getNbrOfGommes()) hscore+= gom_distance_weight; // Else if a new gom was eaten then don't bother with the search and give a small bonus
		else hscore -= gom_distance_weight * bstate.distanceMinToGum(); // else substract points based on distance to nearest gom


		// The idea behind this part is that we disicentivize taking super gums if pacman is not "threatened" to keep them for later
		// meaning the position of at least a ghost is not too uncertain
		if(false && bstate.getNbrOfSuperGommes() < original_bstate.getNbrOfSuperGommes()) {
			int ghost_uncertainty = 100;
			for (int i = 0; i < bstate.getNbrOfGhost(); i++) {
				int ghost_nb_possible_pos = bstate.getGhostPositions(i).size();
				if (ghost_nb_possible_pos < ghost_uncertainty) ghost_uncertainty = ghost_nb_possible_pos;
			}
			if(ghost_uncertainty > 15) hscore-=10;
		}


		//hscore += move_incentive * manhatan_distance(bstate.getPacmanPos(), original_bstate.getPacmanPos()); // add points incentivizing not staying static

		return hscore;
	}
	
	// As we save values based on beliefstates and not on results, had to rework this function to work on  beliefstates
	public static float treesearch(BeliefState bstate, int currentdepth, BeliefState original_state) {

		// If we reach a leaf, stop expanding and return the heuristic value
		if(currentdepth == maxdepth) return heuristic(bstate, original_state);

		ArrayList<BeliefState> beliefchildren;
		ArrayList<Float> children_values;
		float nbchildren;
		float expand_proba;
		Result result;

		float child_value;
		float max_actionvalue = Float.MIN_VALUE;
		float actionvalue;

		Plans plans = bstate.extendsBeliefState();

		for(int i=0; i<plans.size(); i++){

			//if (plans.getAction(i).size()==1 && opposite_direction(plans.getAction(i).get(0), bstate.getPacmanPos().getDirection())) continue;

			result = plans.getResult(i);
			beliefchildren = result.getBeliefStates();

			nbchildren = beliefchildren.size();
			expand_proba = max_expand / nbchildren;

			children_values = new ArrayList<Float>();

			for(BeliefState beliefchild : beliefchildren){
				// To ease the computation burden, we don't compute all the expanded beliefstates
				// We randomly skip some of them depending on how many there are.
				// The expected number of children expanded is the minimum between the number of children and the parameter max_expand.
				if(Math.random()> expand_proba)continue;

				// VIRTUAL GHOST
				// Since pacman is much more efficient when chased by a ghost, the idea is to make him think that he is even when he's not
				// This means we prune moves where pacman stays still or goes backwards
				/*
				if( virtual_ghost &&
					!ghost_in_sight(bstate) && // no ghost in sight
					(manhatan_distance(bstate.getPacmanPos(), beliefchild.getPacmanPos())==0 || // didn't move
					opposite_direction(bstate.getPacmanPos().getDirection(), beliefchild.getPacmanPos().getDirection())) // Or moved backwards
				)child_value=0;


				else */ if(beliefchild.getLife()==0
						//|| manhatan_distance(original_state.getPacmanPos(), beliefchild.getPacmanPos())==0
				){
					// If pacman is dead or came back to his original position, don't expand further
					child_value = heuristic(beliefchild, original_state);
					//memory.put(beliefchild, child_value);
				}

				else if(memory.containsKey(beliefchild)) {
					// If this beliefstate has already been considered, output the already known answer
					child_value =  memory.get(beliefchild);
				}

				else {
					child_value = treesearch(beliefchild, currentdepth + 1, original_state);
					memory.put(beliefchild, child_value); // Obviously, save that result in the memory
				}
				children_values.add(child_value);
			}

			if(children_values.isEmpty()) actionvalue = heuristic(bstate, original_state);

			else actionvalue = aggregateValues(children_values);

			if(actionvalue > max_actionvalue) max_actionvalue = actionvalue;
		}

		return max_actionvalue;
	}

	public static float paralleltreesearch(BeliefState bstate, int currentdepth, BeliefState original_state) {
		ForkJoinPool pool = new ForkJoinPool();
		ParallelTreeSearch search = new ParallelTreeSearch (bstate, currentdepth, original_state, memory);
		pool.invoke (search);

		return search.hvalue;
	}
	
	/**
	 * function that compute the next action to do (among UP, DOWN, LEFT, RIGHT)
	 * @param beliefState the current belief-state of the agent
	 * @param depth the depth of the search (size of the largest sequence of action checked)
	 * @return a string describing the next action (among PacManLauncher.UP/DOWN/LEFT/RIGHT)
	 */
	public static String findNextMove(BeliefState beliefState) {
        if (number_of_moves % memory_refresh_rate ==0) memory.clear();
		Plans plans = beliefState.extendsBeliefState();
		
		float max_utility = Integer.MIN_VALUE;
		float plan_utility;
		String chosen_action = PacManLauncher.LEFT;
		ArrayList<Float> belief_utilities;
		
		// We are going to expand all possible beliefstates possible after each action and select the action with the best aggregate utility
		for(int i=0; i<plans.size(); i++){
			Result plan_result = plans.getResult(i);
			String plan_action = plans.getAction(i).get(0);

			belief_utilities = new ArrayList<Float>();
			
			for(BeliefState bstate : plan_result.getBeliefStates()) {
				belief_utilities.add(paralleltreesearch(bstate, 0, bstate));
			}			
			plan_utility = aggregateValues(belief_utilities) ;

			if (opposite_direction(plan_action, beliefState.getPacmanPos().getDirection() )) {
				plan_utility -= turnback_penalty;
			}
			
			if(plan_utility > max_utility){
				max_utility = plan_utility;
				chosen_action = plan_action;
			}
		}
		number_of_moves++;
		//System.out.println("Found move "+ number_of_moves);
		return chosen_action;
	}

	public static synchronized void save_result(TreeMap<BeliefState, Float> memory, BeliefState bstate, Float hvalue){
		memory.put(bstate, hvalue);
	}

	public static synchronized boolean state_in_memory(TreeMap<BeliefState, Float> memory, BeliefState bstate){
		return memory.containsKey(bstate);
	}

	public static synchronized float get_memory_value(TreeMap<BeliefState, Float> memory, BeliefState bstate){
		return memory.get(bstate);
	}
}

class ParallelTreeSearch  extends RecursiveAction {
	BeliefState bstate;
	Result result;
	int currentdepth;
	BeliefState originalstate;
	float hvalue = Float.MIN_VALUE;
	String nodetype;
	TreeMap<BeliefState, Float> memory;


	ParallelTreeSearch (BeliefState bstate, int currentdepth, BeliefState originalstate, TreeMap<BeliefState, Float> memory)
	{
		this.bstate = bstate;
		this.currentdepth = currentdepth;
		this.originalstate = originalstate;
		this.memory = memory;
		nodetype="OR";
	}

	ParallelTreeSearch (Result result, int currentdepth, BeliefState originalstate, TreeMap<BeliefState, Float> memory)
	{
		this.result = result;
		this.currentdepth = currentdepth;
		this.originalstate = originalstate;
		this.memory = memory;
		nodetype="AND";
	}

	protected void compute (){
		if(Objects.equals(nodetype, "OR")) {
			if (currentdepth == AI.maxdepth || bstate.getLife() == 0) hvalue = AI.heuristic(bstate, originalstate);

			else if (AI.state_in_memory(memory, bstate)) hvalue = AI.get_memory_value(memory, bstate);

			else {
				ArrayList<Result> results = bstate.extendsBeliefState().results;
				ArrayList<ParallelTreeSearch> result_children = new ArrayList<>();

				for (Result result : results) {
					result_children.add(new ParallelTreeSearch(result, currentdepth, originalstate, memory));
				}

				invokeAll(result_children); // Recursive parallel call

				ArrayList<Float> children_values = new ArrayList<>();
				for (ParallelTreeSearch child : result_children) {
					children_values.add(child.hvalue);
				}
				hvalue = Collections.max(children_values);
			}
			AI.save_result(memory, bstate, hvalue);
		}

		else{
			ArrayList<BeliefState> states = result.getBeliefStates();
			ArrayList<ParallelTreeSearch> belief_children = new ArrayList<>();

			for(BeliefState bstate : states){
				belief_children.add(new ParallelTreeSearch(bstate, currentdepth+1, originalstate, memory));
			}

			invokeAll(belief_children);

			ArrayList<Float> children_values = new ArrayList<>();
			for (ParallelTreeSearch child : belief_children) {
				children_values.add(child.hvalue);
			}
			hvalue = AI.aggregateValues(children_values);
		}
	}

}