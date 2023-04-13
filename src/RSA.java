import java.math.BigInteger;
import java.util.Random;

class RSA {

	public BigInteger e;
	public BigInteger d;
	public BigInteger N;

	public RSA() {
		key_generation(25);
	}
	//N = generatorPrime(n): returns a n-bit prime number N
	public static BigInteger generatorPrime(int len) {
		BigInteger N = BigInteger.probablePrime(len, new Random());
		return N;
	}

	// c = modexp(p, q, N): returns c = p^q (mod N)	
	public static BigInteger modexp(BigInteger p, BigInteger q, BigInteger N) {
		BigInteger c;
		c = p.modPow(q, N);
		return c;
	}

	
	public BigInteger encrypt(int n) {
		return encrypt(BigInteger.valueOf(n));
	}

	
	public String encrypt(String str) {

		String res = "";
		for (char c : str.toCharArray()) {
			BigInteger enc = encrypt(getValueForChar(c));
			res += enc + " ";
		}
		return res.trim();
	}

	
	public String decrypt(String m, BigInteger n1, BigInteger n2) {
		String[] list = m.split(" ");
		String str = "";
		for (String s : list) {
			BigInteger b = new BigInteger(s);
			b = decrypt(b, n1, n2);
			str += getCharForNumber(b);
		}
		return str;
	}

	
	public BigInteger encrypt(BigInteger m) {
		BigInteger c = m.modPow(d, N);
		return c;
	}
	
	//for an integer c < N, use the private key to return the decrypted message
	// m = c^d(mod N)
	
	public BigInteger decrypt(BigInteger c, BigInteger private_key, BigInteger bN) {
		BigInteger m = c.modPow(private_key, bN);
		return m;
	}

	
	public void key_generation(int key) {
		BigInteger p, q, bi1, bi2, Phi;
		p = generatorPrime(key);
		do {
			q = generatorPrime(key);
		} while (p.equals(q));
		N = p.multiply(q);
		bi1 = new BigInteger("1");
		bi2 = new BigInteger("-1");
		Phi = (p.subtract(bi1).multiply(q.subtract(bi1)));
		do {
			e = new BigInteger(key, new Random());
			if (e.gcd(Phi).equals(bi1) && e.compareTo(Phi) < 0 && !e.equals(bi1))
				break;
		} while (true);
		d = modexp(e, bi2, Phi);
	}

	
	private BigInteger getValueForChar(char c) {
		return BigInteger.valueOf((int) c);
	}

	
	@SuppressWarnings("unused")
	private String getCharForNumber(BigInteger check) {
		String r = "";
		if (check.compareTo(BigInteger.valueOf(127)) <= 0) {
			char c = (char) (check.intValue());
			return Character.toString(c);
		}
		return check.toString();
	}
}