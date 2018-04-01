package foolish;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Class for storing relevant assets
 * @author Mike
 *
 */
public class Assets {

	public static BufferedImage spriteImage;

	static {
		try {
			Assets.spriteImage = ImageIO.read(Thread.currentThread().getClass().getResource("/images/sprites.png"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
