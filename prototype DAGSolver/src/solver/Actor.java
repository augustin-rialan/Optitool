package solver;

public enum Actor {
	A, D;

	public static Actor other(Actor a) {
		switch (a) {
		case A:
			return D;
		case D:
			return A;
		default:
			assert false;
			return a;
		}

	}

	public Actor other() {
		return Actor.other(this);
	}
}
