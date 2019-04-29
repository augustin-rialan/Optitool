import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import solver.*;

public class Main {
	static boolean debug = false;
	static long startTime = System.currentTimeMillis();
	private static JProgressBar progressBar;
	private static String progressBarTitle = "Idle";
	private static JFormattedTextField budgetInputField;
	private static ButtonGroup optGoalBG;
	private static OptGoal optGoal=null; //La classe OptGoal est-elle une énumération ? (non-trouvable dans le code)
	
	private static File XMLFile;

	public static void main(String[] args) {

		//initGUI();

		System.out.println("Timestamp: " + (System.currentTimeMillis() - startTime) / 1000. + "s");
		ADDAG T = new ADDAG(new File(args[0]));	//transformation du fichier xml en ensemble sémantique lisible par lp-solve
		switch(args[2].toLowerCase()) {  //choix du mode via les arguments dans la commande
		case "coverage":
			optGoal=OptGoal.COVERAGE;
			break;
		case "investment":
			optGoal=OptGoal.INVESTMENT;
			break;
		default:
			System.out.println("Unrecognized optimization goal. Use \"coverage\" or \"investment\". Defaulting to coverage.");
			optGoal=OptGoal.INVESTMENT;
		}

		System.out.println(T);
		/*
		 * / System.out.
		 * println("========================Testing partial set semantics========================"
		 * ); System.out.println("PSS for the ADDAG: "); Set<Set<ADNode>> pss =
		 * T.root.computePartialSetSemantics(); ADNode.printLabelsOnly = true; for
		 * (Set<ADNode> elem : pss) { System.out.println(elem); } ADNode.printLabelsOnly
		 * = false;
		 * 
		 * System.out.
		 * println("========================Testing pruning========================");
		 * System.out.println("Pruning out security cameras:"); Set<String> def = new
		 * HashSet<String>(); def.add("Barbed Wire");
		 * def.add("Monitor with Biometric Sensors"); def.add("Employ Guards");
		 * 
		 * System.out.println("Pruned DAG wrt " + def); ADNode pruned =
		 * T.root.prunedClone(def, true); for (ADNode node : pruned.getNodeSet()) {
		 * 
		 * System.out.println(node); }
		 * 
		 * System.out.
		 * println("========================Testing AS computation on pruned tree========================"
		 * ); System.out.println("AS witnessed by: " + def);
		 * 
		 * for (Set<ADNode> as : pruned.getAttackStrategiesByWitness(def)) {
		 * System.out.println(as); } // System.out.
		 * println("========================Testing global AS computation (dumb way)========================"
		 * );
		 * 
		 * Set<String> defActionsLabels = new HashSet<String>(); Set<String>
		 * attActionsLabels = new HashSet<String>(); for (ADNode node :
		 * T.root.getNodeSet()) { if (node.ref == Refinement.N && node.type == Actor.D)
		 * defActionsLabels.add(node.label); if (node.ref == Refinement.N && node.type
		 * == Actor.A) attActionsLabels.add(node.label); }
		 * 
		 * Set<Set<ADNode>> ASSet = new HashSet<Set<ADNode>>(); for (Set<String> witness
		 * : SetTools.powerSet(defActionsLabels)) {
		 * ASSet.addAll(T.root.getAttackStrategiesByWitness(witness)); } for
		 * (Set<ADNode> as : ASSet) { System.out.println(as); } //
		 */
		
		/*/

		System.out.println("Timestamp: " + (System.currentTimeMillis() - startTime) / 1000. + "s");
		System.out.println("========================Testing random DAG gen========================");

		// ADNode dag = ADDAG.generateRandomADDAG(400, 4, 0.5, 0.6, 5, Actor.A, 0);
		// double[] law = { 0, 0.1, 0.4, 0.4, 0.1 };
		double[] law = { 0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.5 };

		ADNode dag = ADDAG.generateRandomADDAG2(50, law, 10, 0.5);
		if (debug) {
			System.out.println("Generated dag has the following nodes:");
			dag.printTreeForm(0);

			System.out.println("DAG size:" + dag.getNodeSet().size());
		}
		T.root = dag;
		// */

		System.out.println("Timestamp: " + (System.currentTimeMillis() - startTime) / 1000. + "s");

		// --calcul de l'ensemble sémantique <clé,valeur>
		HashMap<Set<ADNode>, Set<Set<ADNode>>> defSem = computeDefenseSemantics(T.root);

		System.out.println("Timestamp: " + (System.currentTimeMillis() - startTime) / 1000. + "s");
		System.out.println("========================Testing the linear solver========================");
		double budget = Double.parseDouble(args[1]);

		HashSet<Set<ADNode>> defenses = new HashSet<Set<ADNode>>(), attacks = new HashSet<Set<ADNode>>();
		HashSet<ADNode> defBasicActions = new HashSet<ADNode>();

		// --on récupère ce qui correspond à l'attaque et à la defense
		for (Entry<Set<ADNode>, Set<Set<ADNode>>> entry : defSem.entrySet()) {
			attacks.add(entry.getKey());
			defenses.addAll(entry.getValue());
		}
		for (Set<ADNode> d : defenses) {
			defBasicActions.addAll(d);
		}

		// --conversion des hashset en list
		ArrayList<Set<ADNode>> defArray = new ArrayList<Set<ADNode>>(defenses);
		ArrayList<Set<ADNode>> attArray = new ArrayList<Set<ADNode>>(attacks);
		ArrayList<ADNode> basicArray = new ArrayList<ADNode>(defBasicActions);
		
		// --partie du choix des options + résolution : on pourra en rajouter ici
		double[] result=new double[0];
		try {
			switch(optGoal) {
			case COVERAGE:
				result=genAndSolveLPCoverage(defSem, budget, defArray, attArray, basicArray);
				break;
			case IMPACT:
				System.out.println("Optimization goal not yet implemented");
				break;
			case INVESTMENT:
				result=genAndSolveLPInvestment(defSem, budget, defArray, attArray, basicArray);
				break;
			default:
				System.out.println("Optimization goal missing");
				break;
			
			}
			
		} catch (LpSolveException e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
			e.printStackTrace();
		}

		// --output : à connecter avec ADTool
		System.out.println("Best defense for the "+optGoal.toString()+" problem, for budget " + budget + ":");
		for (int i = 0; i < basicArray.size(); i++) {
			if (result[i] == 1)
				System.out.println(basicArray.get(i));
		}
		System.out.println("Timestamp: " + (System.currentTimeMillis() - startTime) / 1000. + "s");
		// delete the problem and free memory
		
	}

	private static double[] genAndSolveLPCoverage(HashMap<Set<ADNode>, Set<Set<ADNode>>> defSem, double budget,
			ArrayList<Set<ADNode>> defArray, ArrayList<Set<ADNode>> attArray, ArrayList<ADNode> basicArray)
			throws LpSolveException {
		LpSolve solver=null;
		// Variables are [x1...xp, f1...fm, z1...zn]
		int p = basicArray.size();
		int n = attArray.size();
		int m = defArray.size();

		solver = LpSolve.makeLp(0, p + m + n);
		for (int i = 0; i < n + m + p; i++) {
			solver.setBinary(i + 1, true);// Make vars binary
		}
		// Objective function
		double[] obj = new double[p + n + m + 1];
		Arrays.fill(obj, m + p + 1, m + n + p + 1, 1);
		solver.setObjFn(obj);

		// add constraints

		// Budget constraint (4)
		double[] constraint = new double[p + n + m + 1];
		for (int k = 0; k < p; k++) {
			constraint[k + 1] = basicArray.get(k).cost;
		}
		// System.out.println("Budget constraint: " + Arrays.toString(constraint));
		solver.addConstraint(constraint, LpSolve.LE, budget);

		// Define fj: (5) and (6)

		for (int j = 0; j < m; j++) {
			double[] constraint1 = new double[p + n + m + 1], constraint2 = new double[p + n + m + 1];
			double constant = 0;
			for (int k = 0; k < p; k++) {
				double y = (defArray.get(j).contains(basicArray.get(k))) ? 1 : 0;
				constraint1[k + 1] = y / ((double) p);
				constraint2[k + 1] = y;
				constant += y;
			}
			constraint1[p + j + 1] = 1;
			constraint2[p + j + 1] = 1;
			solver.addConstraint(constraint1, LpSolve.GE, constant / ((double) p));
			// System.out.println(Arrays.toString(constraint1));
			solver.addConstraint(constraint2, LpSolve.LE, constant);
			// System.out.println(Arrays.toString(constraint2));
		}

		// Define Zi (7) and (8)
		for (int i = 0; i < n; i++) {
			double[] constraint1 = new double[p + n + m + 1], constraint2 = new double[p + n + m + 1];
			double sumP = 0;
			for (int j = 0; j < m; j++) {
				if (SetTools.containsReplacement(defSem.get(attArray.get(i)), defArray.get(j)))
					sumP++;
			}
			for (int j = 0; j < m; j++) {
				if (SetTools.containsReplacement(defSem.get(attArray.get(i)), defArray.get(j))) {
					constraint1[p + j + 1] = -1;
					constraint2[p + j + 1] = -1 / sumP;
				}
			}
			constraint1[p + m + i + 1] = 1;
			constraint2[p + m + i + 1] = 1;
			solver.addConstraint(constraint1, LpSolve.GE, 1 - sumP);
			// System.out.println(Arrays.toString(constraint1));
			solver.addConstraint(constraint2, LpSolve.LE, 0);
			// System.out.println(Arrays.toString(constraint2));

		}

		solver.solve();

		// debug
		// solver.printTableau();
		if (debug) {
			System.out.println("Printing linear problem for debugging");
			solver.printLp();
		}
		// print solution
		double[] var = solver.getPtrVariables();
		solver.deleteLp();
		return var;
		
	}

	private static double[] genAndSolveLPInvestment(HashMap<Set<ADNode>, Set<Set<ADNode>>> defSem, double budget,
			ArrayList<Set<ADNode>> defArray, ArrayList<Set<ADNode>> attArray, ArrayList<ADNode> basicArray)
			throws LpSolveException {
		LpSolve solver=null;
		// Variables are [x1...xp, f1...fm, z1...zn]
		int p = basicArray.size();
		int n = attArray.size();
		int m = defArray.size();

		solver = LpSolve.makeLp(0, p + m + n + 1);
		for (int i = 0; i < n + m + p; i++) {
			solver.setBinary(i + 1, true);// Make vars binary
		}
		solver.setInt(p + m + n + 1, false);
		// Objective function
		double[] obj = new double[p + n + m + 2];
		obj[p + m + n + 1]=-1;
		solver.setObjFn(obj);//min -C <==> max C

		// add constraints

		// Budget constraint (4)
		double[] budgetConstraint = new double[p + n + m + 2];
		for (int k = 0; k < p; k++) {
			budgetConstraint[k + 1] = basicArray.get(k).cost;
		}
		// System.out.println("Budget constraint: " + Arrays.toString(constraint));
		solver.addConstraint(budgetConstraint, LpSolve.LE, budget);

		// Define fj: (5) and (6)

		for (int j = 0; j < m; j++) {
			double[] constraint1 = new double[p + n + m + 2], constraint2 = new double[p + n + m + 2];
			double constant = 0;
			for (int k = 0; k < p; k++) {
				double y = (defArray.get(j).contains(basicArray.get(k))) ? 1 : 0;
				constraint1[k + 1] = y / ((double) p);
				constraint2[k + 1] = y;
				constant += y;
			}
			constraint1[p + j + 1] = 1;
			constraint2[p + j + 1] = 1;
			solver.addConstraint(constraint1, LpSolve.GE, constant / ((double) p));
			// System.out.println(Arrays.toString(constraint1));
			solver.addConstraint(constraint2, LpSolve.LE, constant);
			// System.out.println(Arrays.toString(constraint2));
		}

		// Define Zi (7) and (8)
		for (int i = 0; i < n; i++) {
			double[] constraint1 = new double[p + n + m + 2], constraint2 = new double[p + n + m + 2];
			double sumP = 0;
			for (int j = 0; j < m; j++) {
				if (SetTools.containsReplacement(defSem.get(attArray.get(i)), defArray.get(j)))
					sumP++;
			}
			for (int j = 0; j < m; j++) {
				if (SetTools.containsReplacement(defSem.get(attArray.get(i)), defArray.get(j))) {
					constraint1[p + j + 1] = -1;
					constraint2[p + j + 1] = -1 / sumP;
				}
			}
			constraint1[p + m + i + 1] = 1;
			constraint2[p + m + i + 1] = 1;
			solver.addConstraint(constraint1, LpSolve.GE, 1 - sumP);
			// System.out.println(Arrays.toString(constraint1));
			solver.addConstraint(constraint2, LpSolve.LE, 0);
			// System.out.println(Arrays.toString(constraint2));

		}

		//Add A's investment constraints
		
		double maxCost=0;
		double[] cost=new double[attArray.size()];
		for(int i=0;i<n;i++) {
			for(ADNode action:attArray.get(i)) {
				cost[i]+=action.cost;
			}
			if(cost[i]>maxCost) {maxCost=cost[i];}
		}
		
		for(int i=0;i<n;i++) {
			double[] constraint = new double[p + n + m + 2];
			constraint[p + m + i + 1]=maxCost-cost[i];
			constraint[p + m + n + 1]=1;
			solver.addConstraint(constraint, LpSolve.LE, maxCost);
		}
		
		solver.solve();

		// debug
		// solver.printTableau();
		if (debug) {
			System.out.println("Printing linear problem for debugging");
			solver.printLp();
		}
		// print solution
		double[] var = solver.getPtrVariables();
		solver.deleteLp();
		return var;
		
	}

	private static void initGUI() {
		JFrame window = new JFrame();
		window.setTitle("Best defense allocation on DAGs");
		window.setSize(800, 800);
		window.setLocationRelativeTo(null);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		optGoalBG = new ButtonGroup();
		JPanel rButtonLayout = new JPanel();
		rButtonLayout.setLayout(new BoxLayout(rButtonLayout, BoxLayout.PAGE_AXIS));
		rButtonLayout.add(new JLabel("Choose optimization goal:"));
		rButtonLayout.add(new JRadioButton("Coverage"));
		rButtonLayout.add(new JRadioButton("Max invest"));
		rButtonLayout.add(new JRadioButton("Impact"));
		optGoalBG.add((AbstractButton) rButtonLayout.getComponent(1));
		optGoalBG.add((AbstractButton) rButtonLayout.getComponent(2));
		optGoalBG.add((AbstractButton) rButtonLayout.getComponent(3));
		rButtonLayout.add(new JLabel("Choose budget:"));
		NumberFormat f = NumberFormat.getNumberInstance();
		budgetInputField = new JFormattedTextField(f);
		rButtonLayout.add(budgetInputField);

		JPanel upperRightLayout = new JPanel();
		upperRightLayout.setLayout(new BorderLayout());
		JButton treeLoadButton = new JButton("Loaded Tree:null");
		treeLoadButton.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent arg0) {
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new FileFilter() {
					@Override
					public boolean accept(File arg0) {
						return (arg0.getName().toLowerCase().endsWith(".xml")||arg0.isDirectory());
					}
					@Override
					public String getDescription() {
						return "XML file generated by ADTool";
					}});
				
				int result = fc.showOpenDialog(window);
				if(result==JFileChooser.APPROVE_OPTION) {
					XMLFile=fc.getSelectedFile();
					treeLoadButton.setText("Loaded Tree: "+fc.getSelectedFile().getAbsolutePath());
				}
			}

			public void mouseEntered(MouseEvent arg0) {}
			public void mouseExited(MouseEvent arg0) {}
			public void mousePressed(MouseEvent arg0) {}
			public void mouseReleased(MouseEvent arg0) {}
		});
		upperRightLayout.add(treeLoadButton, BorderLayout.NORTH);
		upperRightLayout.add(new JButton("Launch ADTool"), BorderLayout.WEST);
		upperRightLayout.add(new JButton("Start computation of defense semantics"), BorderLayout.CENTER);

		JPanel upperLayout = new JPanel();
		upperLayout.setLayout(new BoxLayout(upperLayout, BoxLayout.LINE_AXIS));
		upperLayout.add(rButtonLayout);
		upperLayout.add(upperRightLayout);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Results", new JTextArea("Results"));
		tabs.addTab("Impact", new JTextArea("Imp"));

		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setString("Idle");
		progressBar.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent arg0) {
				progressBar.setString(progressBarTitle + " - " + progressBar.getPercentComplete() * 100 + "%");
			}
		});

		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());
		main.add(upperLayout, BorderLayout.NORTH);
		main.add(tabs, BorderLayout.CENTER);
		main.add(progressBar, BorderLayout.SOUTH);

		window.setContentPane(main);
		window.setVisible(true);
	}

	private OptGoal getSelectedOptGoal() {
		
		//To use when the GUI is finished
		
		/*/
		
		Object[] t = optGoalBG.getSelection().getSelectedObjects();
		if (t.length == 0)
			return null;
		else
			switch (((JRadioButton) t[0]).getText()) {
			case "Coverage":
				return OptGoal.COVERAGE;
			case "Max invest":
				return OptGoal.INVESTMENT;
			case "Impact":
				return OptGoal.IMPACT;
			default:
				return null;
			}
			//*/
		
		return optGoal;
		
	}

	public static HashMap<Set<ADNode>, Set<Set<ADNode>>> computeDefenseSemantics(ADNode root) {

		System.out.println("========================Computation of leveled defense strategies========================");

		Set<Set<String>> defStrategies = root.computeLeveledDefenseStrategies();
		if (debug) {
			for (Set<String> ds : defStrategies) {
				System.out.println(ds);
			}
		}
		System.out.println("Timestamp: " + (System.currentTimeMillis() - startTime) / 1000. + "s");
		System.out.println(
				"========================Testing global AS computation (smart way using the previous def strat construction)========================");

		HashSet<Set<ADNode>> ASSetSmart = computeAttackStrategiesOld(root, defStrategies);

		System.out.println("Timestamp: " + (System.currentTimeMillis() - startTime) / 1000. + "s");
		System.out.println("========================Computation of the defense semantics========================");

		ADNode switchedDAG = root.getSwitchedClone();

		ADNode falseRoot = new ADNode(
				"This is a dummy node. I hope no user in their right mind will create a node with this label exactly, because it will probably cause some problems. (D6R27-H59P]",
				new ADNode[0], switchedDAG, Refinement.N, Actor.A);
		HashMap<Set<ADNode>, Set<Set<ADNode>>> defSem = new HashMap<Set<ADNode>, Set<Set<ADNode>>>();
		for (Set<ADNode> as : ASSetSmart) {
			Set<Set<ADNode>> defs = falseRoot.getAttackStrategiesByWitness(SetTools.convertToLabels(as));
			for (Set<ADNode> defense : defs) {
				defense.remove(falseRoot);
			}
			if (!defs.isEmpty())
				defSem.put(as, defs);
		}
		if (debug)
			for (Set<ADNode> as : defSem.keySet()) {
				System.out.println("Defenses countering AS: " + SetTools.convertToLabels(as));
				for (Set<ADNode> defense : defSem.get(as)) {
					System.out.println(defense);
				}
			}

		return defSem;
	}
	/*
	 * / private static HashSet<Set<ADNode>> computeAttackStrategies(ADNode root,
	 * Set<Set<String>> defStrategies) {
	 * 
	 * HashMap<ADNode, Set<ADNode>> supportMap = new HashMap<ADNode, Set<ADNode>>();
	 * computeSupport(root, supportMap); return computeAttackStrategies(root,
	 * defStrategies, supportMap); } //
	 */

	private static HashSet<Set<ADNode>> computeAttackStrategiesOld(ADNode root, Set<Set<String>> defStrategies) {
		HashSet<Set<ADNode>> as = new HashSet<Set<ADNode>>();
		for (Set<String> witness : defStrategies) {
			as.addAll(root.getAttackStrategiesByWitness(witness));
		}
		if (debug) {
			for (Set<ADNode> a : as) {
				System.out.println(a);
			}
		}
		return as;
	}

	private static Set<ADNode> computeSupport(ADNode root, HashMap<ADNode, Set<ADNode>> supportMap) {
		Set<ADNode> support = new HashSet<ADNode>();
		for (ADNode child : root.children) {
			support.addAll(supportMap.getOrDefault(child, computeSupport(child, supportMap)));
		}
		if (root.counter != null) {
			support.addAll(supportMap.getOrDefault(root.counter, computeSupport(root.counter, supportMap)));
		}
		if (root.ref == Refinement.N) {
			support.add(root);
		}
		supportMap.put(root, support);
		return support;
	}
	/*
	 * / private static HashSet<Set<ADNode>> computeAttackStrategies(ADNode root,
	 * Set<Set<String>> defStrategies, HashMap<ADNode, Set<ADNode>> support) { //
	 * Check if we can use the optimization (easy case) Set<Set<ADNode>> setsToCheck
	 * = new HashSet<Set<ADNode>>(); for (ADNode child : root.children) {
	 * setsToCheck.add(support.get(child)); } if (root.counter != null) {
	 * setsToCheck.add(support.get(root.counter)); }
	 * 
	 * if (SetTools.checkIntersection(setsToCheck)) {//Not the best we could do //
	 * Perform optimization Set<Set<Set<ADNode>>> childAttackStrategies = new
	 * HashSet<Set<Set<ADNode>>>(); for (ADNode child : root.children) {
	 * childAttackStrategies.add(computeAttackStrategies(child, defStrategies,
	 * support)); } switch(root.ref) { case AND: break; case N: break; case OR:
	 * break; default: break; }
	 * 
	 * 
	 * 
	 * } else {
	 * 
	 * HashSet<Set<ADNode>> attackStrategies = new HashSet<Set<ADNode>>(); for
	 * (Set<String> witness : defStrategies) {
	 * attackStrategies.addAll(root.getAttackStrategiesByWitness(witness)); } if
	 * (debug) for (Set<ADNode> as : attackStrategies) { System.out.println(as); }
	 * return attackStrategies; }
	 * 
	 * } //
	 */
}
