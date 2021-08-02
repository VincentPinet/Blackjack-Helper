public class Cards {

	private long cards;
	private int size;
	private int count;

	public Cards() {
		this.cards = 0;
		this.size = 0;
		this.count = 0;
	}

	public Cards(int n) {
		init(n);
	}

	public Cards(Cards o) {
		this.cards = o.cards;
		this.size = o.size;
		this.count = o.count;
	}

	public void init(int n) {
		assert(n >= 0);
		assert(n <= 15);
		this.cards = 4 * 4 * n;
		for (int i = 1; i < 10; i++)
			this.cards = (this.cards << 6) + 4 * n;
		this.size = 52 * n;
		this.count = (10*4 + 9*10/2) * 4 * n;
	}

	public int size() {
		return this.size;
	}

	public long contains(int rank) {
		assert(rank >= 1);
		assert(rank <= 10);
		return this.cards >> (rank - 1) * 6 & (rank!=10 ? 0b111111 : 0b11111111);
	}

	public void remove(int rank) {
		assert(rank >= 1);
		assert(rank <= 10);
		assert(contains(rank) > 0);
		this.cards -= 1L << (rank - 1) * 6;
		this.size -= 1;
		this.count -= rank;
	}

	public void add(int rank) {
		assert(rank >= 1);
		assert(rank <= 10);
		this.cards += 1L << (rank - 1) * 6;
		this.size += 1;
		this.count += rank;
	}

	public void draw(Cards deck, int card) {
		this.add(card);
		deck.remove(card);
	}

	public int sum() {
		if (this.contains(1) > 0 && this.count <= 11) return this.count + 10;
		return this.count;
	}

	public boolean is_bust() {
		return this.sum() > 21;
	}

	public boolean is_soft() {
		return this.contains(1) > 0 && this.count <= 11;
	}

	public int is_splittable() {
		if (this.size != 2) return 0;
		for (int i = 0; i <= 10; i++)
			if (contains(i) == 2)
				return i;
		return 0;
	}

	public long is_splitted() {
		return this.cards >> 62;
	}

	public boolean is_blackjack() {
		return sum() == 21 && size == 2 && is_splitted() == 0;
	}

	public void do_split() {
		int card = is_splittable();
		remove(card);
		this.cards += 1L << 62;
	}

	public long raw() {
		return cards;
	}

	@Override
	public String toString() {
		String res = "";
		for (int i = 1; i <= 10; i++)
			for(int j = 0; j < (this.cards >> 6 * (i - 1) & 0b111111); j++)
				if (i == 1) res += "A";
				else if (i == 10) res += "T";
				else res += "" + i;
		res += " (" + this.sum() + (this.sum()<10?" ":"") + ")";
		return res;
	}
}
