import java.util.Arrays;
import java.util.HashMap;

public class Solver {

	private final HashMap<State, HashMap<String, Double>> solution;
	private final HashMap<State, double[]> dealer_cache;


	public Solver() {
		this.solution = new HashMap<>();
		this.dealer_cache = new HashMap<>();
	}


	private double eval(Cards player, int dealer) {
		if (player.is_blackjack()) return dealer==0 ? 0 : 1.5;
		if (dealer == 0 || player.sum() > 21) return -1;
		if (dealer > 21) return 1;
		return (player.sum() >= dealer?1:0) - (dealer >= player.sum()?1:0);
	}


	private void dealer_score_rec(Cards hand, Cards deck, double weight, double[] res) {
		for (int i = 1; i <= 10; i++) {
			if (deck.contains(i) == 0) continue;
			double adj_weight = weight * deck.contains(i) / deck.size();
			hand.add(i);
			if (hand.sum() > 16) {
				if (hand.is_blackjack()) res[0] += adj_weight;
				else if (hand.is_bust()) res[22] += adj_weight;
				else res[hand.sum()] += adj_weight;
			} else {
				deck.remove(i);
				dealer_score_rec(hand, deck, adj_weight, res);
				deck.add(i);
			}
			hand.remove(i);
		}
	}


	private double[] dealer_score(int card, Cards deck) {
		Cards hand = new Cards();
		hand.add(card);

		State state = new State(hand, 0, deck);
		if (dealer_cache.containsKey(state))
			return dealer_cache.get(state);

		double[] res = new double[23];
		Arrays.fill(res, 0.0);

		dealer_score_rec(hand, deck, 1.0, res);

		dealer_cache.put(state, res);
		return res;
	}


	private double stand(Cards player, int dealer, Cards deck) {
		double res = 0;
		double[] a = dealer_score(dealer, deck);
		for (int i = 0; i <= 22; i++)
			res += eval(player, i) * a[i];
		return res;
	}


	private double optimal(HashMap<String, Double> actions) {
		return actions.values().stream().max(Double::compare).get();
	}


	public double compute(Cards player, int dealer, Cards deck) {
		State state = new State(player, dealer, deck);
		if (solution.containsKey(state))
			return optimal(solution.get(state));

		HashMap<String, Double> res = new HashMap<>();

		// BUST
		if (player.sum() > 21)
			return -1;

		// STAND
		res.put("STAND", stand(player, dealer, deck));

		// HIT
		double ev = 0;
		for (int i = 1; i <= 10; i++) {
			if (deck.contains(i) == 0) continue;
			double weight = 1.0 * deck.contains(i) / deck.size();
			player.draw(deck, i);
			ev += compute(player, dealer, deck) * weight;
			deck.draw(player, i);
		}
		res.put("HIT", ev);

		// SPLIT
		if (player.is_splitted() == 0 && player.is_splittable() > 0) {
			ev = 0;
			Cards player_splitted = new Cards(player);
			player_splitted.do_split();
			for (int i = 1; i <= 10; i++) {
				if (deck.contains(i) == 0) continue;
				double weight = 1.0 * deck.contains(i) / deck.size();
				player_splitted.draw(deck, i);
				// STAND AFTER SPLIT ACES
				if (player.contains(1) > 0)
					ev += 2 * stand(player_splitted, dealer, deck) * weight;
				else
					ev += 2 * compute(player_splitted, dealer, deck) * weight;
				deck.draw(player_splitted, i);
			}
			res.put("SPLIT", ev);
		}

		// DOUBLE
		if (player.size() == 2) {
			ev = 0;
			for (int i = 1; i <= 10; i++) {
				if (deck.contains(i) == 0) continue;
				double weight = 1.0 * deck.contains(i) / deck.size();
				player.draw(deck, i);
				ev += 2 * stand(player, dealer, deck) * weight;
				deck.draw(player, i);
			}
			res.put("DOUBLE", ev);
		}

		solution.put(state, res);
		return optimal(res);
	}


	public double compute_ev(Cards deck) {

		double weight = 1;
		double ev = 0;

 		Cards player = new Cards();
		for (int i = 1; i <= 10; i++) {
			if (deck.contains(i) == 0) continue;
			weight *= 1.0 * deck.contains(i) / deck.size();
			player.draw(deck, i);

			for (int j = 1; j <= i; j++) {
				if (deck.contains(j) == 0) continue;
				weight *= 1.0 * deck.contains(j) / deck.size();
				if (j != i) weight *= 2;
				player.draw(deck, j);

				for (int k = 1; k <= 10; k++) {
					if (deck.contains(k) == 0) continue;
					weight *= 1.0 * deck.contains(k) / deck.size();
					deck.remove(k);
					ev += compute(player, k, deck) * weight;
					// System.out.println("" + player + " - " + (k==1?"A":k==10?"T":""+k) + " = " + (compute(player, k, deck)>0?"+":"") + compute(player, k, deck));
					// System.out.print("" + player + " - " + (k==1?"A":k==10?"T":""+k) + " = ");
					// System.out.println(solution.get(new State(player, k, deck)));
					deck.add(k);
					weight /= 1.0 * deck.contains(k) / deck.size();
				}

				deck.draw(player, j);
				if (j != i) weight /= 2;
				weight /= 1.0 * deck.contains(j) / deck.size();
			}

			deck.draw(player, i);
			weight /= 1.0 * deck.contains(i) / deck.size();
		}
		return ev;
	}
}
