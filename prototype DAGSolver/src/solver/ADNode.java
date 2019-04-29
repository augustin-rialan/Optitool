package solver;

import java.util.*;
import java.util.Map.Entry;

public class ADNode {
	public final String label;
	public ADNode[] children;// Not including countermeasure
	public ADNode counter = null;
	public Refinement ref = Refinement.N;
	public Actor type;
	public double cost = -1;// Default value for refined nodes
	public double prob = -1;// Default value for refined nodes
	public double impact=-1;
	public static boolean printLabelsOnly = false;
	int rank;// Only used to generate random DAGs


	// Should do getters & setters with proper checks

	public ADNode(String label, ADNode[] children, ADNode counter, Refinement ref, Actor owner) {
		super();
		this.label = label;
		this.children = children;
		this.counter = counter;
		this.ref = ref;
		this.type = owner;

		checkLegality();
	}

	public ADNode(String label, ADNode[] children, Refinement ref, Actor owner) {
		this(label, children, null, ref, owner);
	}

	private void checkLegality() {
		for (ADNode child : children) {
			if (child.type != this.type)
				throw new IllegalArgumentException("Child of wrong type in node " + this);
		}
		if ((counter != null) && counter.type == this.type)
			throw new IllegalArgumentException("Counter of wrong type in node " + this);

		if (this.ref == Refinement.N && !(children == null || children.length == 0))
			throw new IllegalArgumentException("Node " + this + " is unrefined, yet has children");

	}

	public String toString() {
		if (printLabelsOnly)
			return label;
		else
			return "(" + label + "," + ref + "," + type + ",cost=" + cost + " prob=" + prob + ")";
	}

	public boolean deepEquals(ADNode node2) {
		if (!this.label.equals(node2.label))
			return false;
		if (this.ref != node2.ref)
			return false;
		if (this.type != node2.type)
			return false;
		if ((this.counter == null && node2.counter != null) || (this.counter != null && node2.counter == null)) {
			return false;
		} else if (this.counter != null && node2.counter != null)
			if (!this.counter.deepEquals(node2.counter))
				return false;

		if (this.children.length == node2.children.length)
			for (int i = 0; i < this.children.length; i++) {
				if (!this.children[i].deepEquals(node2.children[i]))
					return false;
			}
		else {
			return false;
		}

		return true;
	}

	public Set<Set<ADNode>> computePartialSetSemantics() {

		Set<Set<Set<ADNode>>> childSem = new HashSet<Set<Set<ADNode>>>();
		for (ADNode child : this.children) {
			childSem.add(child.computePartialSetSemantics());
		}
		Set<Set<ADNode>> counterSem = (counter == null ? null : counter.computePartialSetSemantics());

		switch (this.ref) {
		case AND:
			switch (this.type) {
			case A:// AND^a
				return counterSem == null ? SetTools.oTimes(childSem)
						: SetTools.oTimes(counterSem, SetTools.oTimes(childSem));
			case D:
				return counterSem == null ? SetTools.union(childSem)
						: SetTools.union(counterSem, SetTools.union(childSem));
			default:
				throw new IllegalArgumentException("Unknown actor");
			}
		case N:
			switch (this.type) {
			case A:// N^a
				Set<Set<ADNode>> v = new HashSet<Set<ADNode>>();
				v.add(new HashSet<ADNode>(Collections.singleton(this)));
				return counterSem == null ? v : SetTools.oTimes(v, counterSem);
			case D:
				return counterSem == null ? new HashSet<Set<ADNode>>() : counterSem;
			default:
				throw new IllegalArgumentException("Unknown actor");
			}
		case OR:
			switch (this.type) {
			case A:// OR^a
				return counterSem == null ? SetTools.union(childSem)
						: SetTools.oTimes(counterSem, SetTools.union(childSem));
			case D:
				return counterSem == null ? SetTools.oTimes(childSem)
						: SetTools.union(counterSem, SetTools.oTimes(childSem));
			default:
				throw new IllegalArgumentException("Unknown actor");
			}
		default:
			throw new IllegalArgumentException("Unknown refinement");

		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ADNode)
			return label.equals(((ADNode) obj).label);
		else
			return false;
	}

	// */
	@Override
	public int hashCode() {
		return label.hashCode();
	}

	// */
	public ADNode prunedClone(Set<String> def, boolean remove) {// Not very efficient
		HashMap<String, ADNode> newNodes = new HashMap<String, ADNode>();

		return prunedClone(def, newNodes, remove);
	}

	private ADNode prunedClone(Set<String> def, HashMap<String, ADNode> newNodes, boolean remove) {// Removes def
		if (newNodes.containsKey(label))
			return newNodes.get(label);

		ADNode clone = this.clone();
		if (remove && def.contains(label)
				|| (!remove && !def.contains(label) && type == Actor.D && ref == Refinement.N))
			return null;
		else if (ref == Refinement.OR) {
			ArrayList<ADNode> newChildren = new ArrayList<ADNode>();
			boolean kept = false;
			for (ADNode child : children) {
				ADNode newChild = child.prunedClone(def, newNodes, remove);

				if (newChild != null) {
					kept = true;
					newChildren.add(newChild);
				}
			}
			if (!kept)
				return null;
			clone.children = newChildren.toArray(new ADNode[0]);
		} else if (ref == Refinement.AND) {
			ArrayList<ADNode> newChildren = new ArrayList<ADNode>();

			for (ADNode child : children) {
				ADNode newChild = child.prunedClone(def, newNodes, remove);
				newChildren.add(newChild);
				if (newChild == null)
					return null;
			}
			clone.children = newChildren.toArray(new ADNode[0]);
		}

		if (counter != null)
			clone.counter = counter.prunedClone(def, newNodes, remove);

		newNodes.put(label, clone);
		return clone;
	}

	public ADNode clone() {
		ADNode clone = new ADNode(label, children.clone(), counter, ref, type);
		clone.cost = cost;
		clone.prob = prob;
		return clone;
	}

	public Set<ADNode> getNodeSet() {
		HashSet<ADNode> ret = new HashSet<ADNode>();
		ret.add(this);
		for (ADNode child : children) {
			ret.addAll(child.getNodeSet());
		}
		if (counter != null)
			ret.addAll(counter.getNodeSet());
		return ret;
	}

	public Set<Set<ADNode>> getAttackStrategiesByWitness(Set<String> def) {
		return SetTools.removeNonMinimal(this.prunedClone(def, false).computePartialSetSemantics());
	}

	public Set<Set<String>> computeLeveledDefenseStrategies() {
		HashSet<Set<ADNode>> leveledStrategies = new HashSet<Set<ADNode>>();
		HashMap<ADNode, Integer> levels = new HashMap<ADNode, Integer>();
		HashMap<ADNode, ADNode> pred = new HashMap<ADNode, ADNode>();
		getRootedNodesLevels(this, 0, levels, pred, null);
		HashMap<ADNode, Set<Set<ADNode>>> defVectors = new HashMap<ADNode, Set<Set<ADNode>>>();
		for (ADNode node : pred.keySet()) {
			if (node != null)
				defVectors.put(node, node.computeDefenseVectors());
		}

		int maxLevel = 0;
		for (Entry<ADNode, Integer> entry : levels.entrySet()) {
			maxLevel = Math.max(maxLevel, entry.getValue());
		}

		HashSet<Set<ADNode>> levelNStrategies = new HashSet<Set<ADNode>>();
		levelNStrategies.add(new HashSet<ADNode>());
		leveledStrategies.addAll(levelNStrategies);
		for (int n = 0; n < maxLevel; n++) {
			HashSet<Set<ADNode>> nextStrategies = new HashSet<Set<ADNode>>();

			for (Set<ADNode> defStrat : levelNStrategies) {
				HashSet<Set<ADNode>> nextDefVectors = new HashSet<Set<ADNode>>();
				for (ADNode node : pred.keySet()) {
					if (levels.get(node) == n + 1 && (n == 0 || defStrat.contains(pred.get(node))))
						nextDefVectors.addAll(defVectors.get(node));
				}

				for (Set<Set<ADNode>> defSet : SetTools.powerSet(nextDefVectors)) {
					HashSet<ADNode> clone = new HashSet<ADNode>();
					clone.addAll(defStrat);
					clone.addAll(SetTools.union(defSet));
					nextStrategies.add(clone);
				}
			}
			if (nextStrategies.isEmpty())
				break;
			levelNStrategies = nextStrategies;
			leveledStrategies.addAll(levelNStrategies);
		}

		Set<Set<String>> ret = new HashSet<Set<String>>();
		for (Set<ADNode> strat : leveledStrategies) {
			HashSet<String> equiv = new HashSet<String>();
			for (ADNode elem : strat) {
				if(elem.ref==Refinement.N) {
					equiv.add(elem.label);
				}
			}
			ret.add(equiv);
		}

		return ret;

	}

	public static void getRootedNodesLevels(ADNode adNode, int i, HashMap<ADNode, Integer> levels,
			HashMap<ADNode, ADNode> pred, ADNode last) {
		Actor currentType = adNode.type;
		for (ADNode child : adNode.children) {
			getRootedNodesLevels(child, i, levels, pred, last);
		}
		if (adNode.counter != null) {
			if (currentType == Actor.A) {
				if (levels.containsKey(adNode.counter)) {
					if (i + 1 < levels.get(adNode.counter)) {
						levels.put(adNode.counter, i + 1);
						pred.put(adNode.counter, last);
						getRootedNodesLevels(adNode.counter, i + 1, levels, pred, last);
					}
				} else {
					levels.put(adNode.counter, i + 1);
					pred.put(adNode.counter, last);
					getRootedNodesLevels(adNode.counter, i + 1, levels, pred, last);
				}
			} else
				getRootedNodesLevels(adNode.counter, i, levels, pred, adNode);
		}
	}

	private Set<Set<ADNode>> computeDefenseVectors() {
		if (this.type == Actor.A)
			throw new IllegalArgumentException();

		Set<Set<Set<ADNode>>> childSem = new HashSet<Set<Set<ADNode>>>();
		for (ADNode child : this.children) {
			childSem.add(child.computeDefenseVectors());
		}
		Set<Set<ADNode>> v;
		switch (this.ref) {
		case AND:
			v = SetTools.oTimes(childSem);
			for (Set<ADNode> defVect : v) {
				defVect.add(this);
			}
			return v;

		case N:
			v = new HashSet<Set<ADNode>>();
			v.add(new HashSet<ADNode>(Collections.singleton(this)));
			return v;
		case OR:
			v = SetTools.union(childSem);
			for (Set<ADNode> defVect : v) {
				defVect.add(this);
			}
			return v;
		default:
			throw new IllegalArgumentException("Unknown refinement");

		}

	}

	public ADNode getSwitchedClone() {
		return getSwitchedClone(new HashMap<String, ADNode>());
	}

	private ADNode getSwitchedClone(HashMap<String, ADNode> newNodes) {
		if (newNodes.containsKey(label))
			return newNodes.get(label);

		ADNode clone = this.clone();
		clone.type = clone.type.other();
		clone.children = children.clone();
		for (int i = 0; i < children.length; i++) {
			clone.children[i] = children[i].getSwitchedClone(newNodes);
		}
		if (counter != null)
			clone.counter = counter.getSwitchedClone(newNodes);

		newNodes.put(label, clone);
		return clone;
	}

	public void printTreeForm(int n) {
		String s = String.join("", Collections.nCopies(n, "|    "));
		System.out.println(s+this.toString());
		for(ADNode child:children) {
			child.printTreeForm(n+1);
		}
		if(counter!=null) {
		System.out.println(s+"Counter: ");
		counter.printTreeForm(n+1);
		}
		
	}
	
}
