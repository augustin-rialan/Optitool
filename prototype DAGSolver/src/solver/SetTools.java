package solver;

import java.util.*;

public class SetTools {
	public static <U> Set<Set<U>> removeNonMinimal(Set<Set<U>> f) {
		Set<Set<U>> remove = new HashSet<Set<U>>();
		for (Set<U> x : f)
			for (Set<U> y : f) {
				if (x.equals(y))
					continue;
				if (x.size() <= y.size()) {
					continue;
				}
				// check if y \subset x
				boolean ySubsetx = true;
				for (U elem : y) {
					if (!x.contains(elem)) {
						ySubsetx = false;
						break;
					}
				}
				if (ySubsetx)
					remove.add(x);
			}
		f.removeAll(remove);
		return f;
	}

	public static <U> Set<Set<U>> powerSet(Set<U> param) {
		Set<Set<U>> ret = new HashSet<Set<U>>();
		ret.add(new HashSet<U>());
		for (U elem : param) {

			HashSet<Set<U>> A = new HashSet<Set<U>>();
			A.add(new HashSet<U>());
			A.add(singleton(elem));
			ret = oTimes(ret, A);

		}

		return ret;
	}

	public static <U> Set<U> singleton(U param) {
		HashSet<U> A = new HashSet<U>();
		A.add(param);
		return A;
	}

	public static <U> Set<Set<U>> oTimes(Set<Set<U>> set1, Set<Set<U>> set2) {
		Set<Set<U>> res = new HashSet<Set<U>>();

		for (Set<U> a : set1)
			for (Set<U> b : set2) {
				Set<U> c = new HashSet<U>(a);
				c.addAll(b);
				res.add(c);
			}

		return res;
	}

	public static <U> Set<Set<U>> oTimes(Set<Set<Set<U>>> parameters) {

		Iterator<Set<Set<U>>> it = parameters.iterator();
		Set<Set<U>> res = it.next();
		while (it.hasNext()) {
			res = oTimes(res, it.next());
		}

		return res;
	}

	public static <U> Set<U> union(Set<U> set1, Set<U> set2) {
		Set<U> res = new HashSet<U>(set1);
		res.addAll(set2);
		return res;
	}

	public static <U> Set<U> union(Set<Set<U>> parameters) {
		if (parameters.isEmpty())
			return new HashSet<U>();
		Iterator<Set<U>> it = parameters.iterator();
		Set<U> res = it.next();
		while (it.hasNext()) {
			res = union(res, it.next());
		}

		return res;
	}

	public static Set<String> convertToLabels(Set<ADNode> s) {
		HashSet<String> ret = new HashSet<String>();
		for (ADNode node : s) {
			ret.add(node.label);
		}
		return ret;
	}

	public static <U> boolean containsReplacement(Set<U> s, U e) {// Check if e \in s. Terribly inefficient
		// TODO rewrite this hack

		for (U elem : s) {
			if (elem.equals(e))
				return true;
		}
		return false;

	}

	public static boolean checkIntersection(Set<Set<ADNode>> setsToCheck) {
		// Returns true iff there is two elements of setsToCheck that have a nonempty
		// intersection
		for (Set<ADNode> set1 : setsToCheck) {
			for (Set<ADNode> set2 : setsToCheck) {
				if (set1 == set2)
					continue;
				else {
					/*
					 * HashSet<ADNode> inter = new HashSet<ADNode>(set1); inter.retainAll(set2); if
					 * (!inter.isEmpty()) return false;
					 */
					if (!Collections.disjoint(set1, set2))
						return false;
				}
			}
		}

		return true;
	}

}
