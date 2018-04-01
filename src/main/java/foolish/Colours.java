package foolish;

import mikera.util.Rand;
import mikera.vectorz.Vector4;

public class Colours {
	public static Vector4 create(double r, double g, double b) {
		return Vector4.of(r,g,b,1.0);
	}
	
	public static Vector4 create(double r, double g, double b, double a) {
		return Vector4.of(r,g,b,a);
	}

	public static final Vector4 WHITE =create(1.0,1.0,1.0,1.0);
	public static final Vector4 BLACK = create(0.0,0.0,0.0,1.0);
	public static final Vector4 RED = create(1.0,0.0,0.0,1.0);
	public static final Vector4 GREEN = create(0.0,1.0,0.0,1.0);
	public static final Vector4 BLUE = create(0.0,0.0,1.0,1.0);
	public static final Vector4 CYAN = create(0.0,1.0,1.0,1.0);
	public static final Vector4 MAGENTA = create(1.0,0.0,1.0,1.0);
	public static final Vector4 YELLOW = create(1.0,1.0,0.0,1.0);
	
	public static final Vector4 GREY_90 = create(0.90,0.90,0.90,1.0);
	public static final Vector4 GREY_75 = create(0.75,0.75,0.75,1.0);
	public static final Vector4 GREY_50 = create(0.50,0.50,0.50,1.0);
	public static final Vector4 GREY_25 = create(0.50,0.50,0.50,1.0);

	public static final Vector4 LIGHT_BROWN = create(0.70,0.45,0.30,1.0);
	public static final Vector4 BROWN = create(0.60,0.40,0.20,1.0);
	public static final Vector4 DARK_BROWN = create(0.40,0.25,0.10,1.0);
	public static final Vector4 RED_BROWN = create(0.70,0.30,0.15,1.0);
	public static final Vector4 GREY_BROWN = create(0.70,0.40,0.30,1.0);
	public static final Vector4 YELLOW_BROWN = create(0.70,0.55,0.30,1.0);
	public static final Vector4 SANDSTONE = create(0.80,0.70,0.30,1.0);
	
	public static final Vector4 GRASS_GREEN = create(0.3,0.6,0.2,1.0);
	public static final Vector4 LIGHT_GRASS_GREEN = create(0.5,0.8,0.3,1.0);
	public static final Vector4 STRAW = create(0.9,0.8,0.6,1.0);
	
	private static final double DOUBLE_SCALE_FACTOR=1.0/Math.pow(2,63);
	
	/**
	 * Adds noise to a colour vector, using a given seed
	 * @param c
	 * @param amount
	 */
	public static void addNoise(Vector4 c, double amount, long seed) {
		double base=1-amount;
		double scale=DOUBLE_SCALE_FACTOR*2.0*amount;
		
		seed=Rand.xorShift64(seed);
		c.x*=(base+(seed>>>1)*scale);
		seed=Rand.xorShift64(seed);
		c.y*=(base+(seed>>>1)*scale);	
		seed=Rand.xorShift64(seed);
		c.z*=(base+(seed>>>1)*scale);
	}
	
	public static long hash(int x, int y, int z) {
		return Rand.xorShift64(x*5875857L + y* 1764571L+ z*5858753L);
	}
	
	public static long hash(long seed) {
		return Rand.xorShift64(seed*2425875857L);
	}

}
