package es.tododev.stocks;

public class Pair<N, M> {

	private final N n;
	private final M m;
	public Pair(N n, M m) {
		this.n = n;
		this.m = m;
	}
	public N getN() {
		return n;
	}
	public M getM() {
		return m;
	}
	
}
