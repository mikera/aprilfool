package foolish;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BLEND;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_NEAREST;
import static com.jogamp.opengl.GL.GL_ONE_MINUS_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import mikera.vectorz.Vector4;

public class App implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

	// private final boolean DEBUG = false;
	protected JFrame jframe;
	protected GLCanvas glCanvas;
	protected GLProfile glProfile;
	protected Animator animator;
	protected int[] windowSize = new int[] { 1600, 1200 };
	protected FloatBuffer clearColor = Buffers.newDirectFloatBuffer(4), clearDepth = Buffers.newDirectFloatBuffer(1);

	private final String SHADERS_ROOT = "shaders";
	private final String VERT_SHADER_SOURCE = "vertex-shader";
	private final String FRAG_SHADER_SOURCE = "fragment-shader";
	private int width = 10;
	private int height = 10;

	float DRAW_SCALE = 4.0f;

	private int scrollX = -400;
	private int scrollY = -200;
	private double scrollPosX = scrollX;
	private double scrollPosY = scrollY;
	private int scrollTargetX = scrollX;
	private int scrollTargetY = scrollY;
	public int cursorX = 0;
	public int cursorY = 0;
	public int cursorZ = 10;
	private boolean mouseDown = false;
	
	private boolean running=true;
	public boolean replLaunched=false;

	long baseTime = System.currentTimeMillis();
	double lastTime = 0.0f;

	public Object renderGameState = null;

	private final int ATTR_POSITION = 0;
	private final int ATTR_TEXTURE = 1;
	private final int ATTR_COLOUR = 2;

	private int theProgram;
	private IntBuffer positionBufferObject = Buffers.newDirectIntBuffer(1);
	private IntBuffer vao = Buffers.newDirectIntBuffer(1);
	// private float[] vertexPositions = {
	// +0.75f, +0.75f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,1.0f,1.0f,1.0f,
	// +0.75f, -0.75f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,2.0f,1.0f,1.0f,
	// -0.75f, +0.75f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f,1.0f,1.0f,0.0f,
	// -0.75f, -0.75f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,1.0f,1.0f,0.0f
	// };

	int numQuads = 0;

	FloatBuffer vertexBuffer = Buffers.newDirectFloatBuffer(new float[] {});

	private Texture texture;
	static {
	}

	public App(String title) {
		initGL(title);
	}
	
	public boolean isRunning() {
		return running;
	}

	private void initGL(String title) {

		glProfile = GLProfile.get(GLProfile.GL3);
		GLCapabilities glCapabilities = new GLCapabilities(glProfile);
		glCanvas = new GLCanvas(glCapabilities);
		// System.out.println("Is child window? : " +glWindow.isChildWindow());

		glCanvas.addGLEventListener(this);
		glCanvas.addKeyListener(this);
		glCanvas.addMouseListener(this);
		glCanvas.addMouseMotionListener(this);
		glCanvas.addMouseWheelListener(this);

		animator = new Animator();
		animator.add(glCanvas);
		animator.start();

		jframe = new JFrame("Stronghold Swing Frame");
		jframe.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowevent) {
				running=false;
				jframe.dispose();
				glCanvas.destroy();
				shutDown();
			}
		});

		URL iconURL = getClass().getResource("/images/icon.png");
		if (iconURL != null) {
			jframe.setIconImage(new ImageIcon(iconURL).getImage());
		}

		jframe.addKeyListener(this);
		jframe.getContentPane().add(glCanvas, BorderLayout.CENTER);
		jframe.setSize(windowSize[0], windowSize[1]);
		jframe.setVisible(true);

	}

	@Override
	public final void init(GLAutoDrawable drawable) {

		GL3 gl3 = drawable.getGL().getGL3();

		init(gl3);

	}

	protected void init(GL3 gl3) {
		initializeProgram(gl3);
		initializeVertexBuffer(gl3);
		initialiseTextures(gl3);

		gl3.glEnable(GL_DEPTH_TEST);
		gl3.glDepthMask(true);
		gl3.glDepthFunc(GL_LEQUAL);
		gl3.glDepthRange(0.0f, 1.0f);
	}

	private void initializeVertexBuffer(GL3 gl3) {
		gl3.glGenBuffers(1, positionBufferObject);
		// System.out.println("Position buffer = " +
		// positionBufferObject.get(0));

		gl3.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
		gl3.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
		gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	private void initializeProgram(GL3 gl3) {

		ShaderProgram shaderProgram = new ShaderProgram();

		ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
				VERT_SHADER_SOURCE, "vert", null, true);
		ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
				FRAG_SHADER_SOURCE, "frag", null, true);

		shaderProgram.add(vertShaderCode);
		shaderProgram.add(fragShaderCode);

		shaderProgram.link(gl3, System.out);

		theProgram = shaderProgram.program();

		vertShaderCode.destroy(gl3);
		fragShaderCode.destroy(gl3);
	}

	private void initialiseTextures(GL3 gl3) {
		texture = AWTTextureIO.newTexture(glProfile, Assets.spriteImage, false);

	}

	@Override
	public final void display(GLAutoDrawable drawable) {

		GL3 gl3 = drawable.getGL().getGL3();

		display(gl3);
	}

	protected void ensureBuffer(int size) {
		int oldSize = vertexBuffer.capacity();
		if (oldSize < size) {
			int newSize = Math.max(size, oldSize * 2);
			FloatBuffer newBuffer = Buffers.newDirectFloatBuffer(newSize);
			int pos = vertexBuffer.position();
			vertexBuffer.position(0);
			newBuffer.put(vertexBuffer);
			vertexBuffer = newBuffer;
			vertexBuffer.position(pos);
			// System.out.println(vertexBuffer.get(38));
		}
	}

	protected void addVertex(float x, float y, float z, float t, float tx, float ty, float r, float g, float b,
			float a) {
		ensureBuffer(vertexBuffer.position() + 10);
		vertexBuffer.put(x);
		vertexBuffer.put(y);
		vertexBuffer.put(z);
		vertexBuffer.put(t);
		vertexBuffer.put(tx);
		vertexBuffer.put(ty);
		vertexBuffer.put(r);
		vertexBuffer.put(g);
		vertexBuffer.put(b);
		vertexBuffer.put(a);
	}

	public void drawSprite(float x, float y, float w, float h, float z, int tx, int ty, int tw, int th, Vector4 col) {
		float r = (float) col.x;
		float g = (float) col.y;
		float b = (float) col.z;
		float a = (float) col.t;

		drawSprite(x, y, w, h, z, tx, ty, tw, th, r, g, b, a);
	}

	public void drawSprite(float x, float y, float w, float h, float z, int tx, int ty, int tw, int th, float r,
			float g, float b, float a) {
		float texScale = 1.0f / 1024.0f;

		x = DRAW_SCALE * x - scrollX;
		y = DRAW_SCALE * y - scrollY;
		w = DRAW_SCALE * w;
		h = DRAW_SCALE * h;

		float tx0 = tx * texScale;
		float tx1 = (tx + tw) * texScale;
		float ty0 = (ty + th) * texScale;
		float ty1 = (ty) * texScale;

		float wScale = (2.0f) / width;
		float hScale = (2.0f) / height;

		float x0 = (x) * wScale - 1;
		float x1 = (x + w) * wScale - 1;
		// flip y, so [0,0] renders to top left
		float y0 = 1.0f - (y + h) * hScale;
		float y1 = 1.0f - (y) * hScale;

		addVertex(x0, y0, z, 1, tx0, ty0, r, g, b, a);
		addVertex(x0, y1, z, 1, tx0, ty1, r, g, b, a);
		addVertex(x1, y0, z, 1, tx1, ty0, r, g, b, a);
		addVertex(x0, y1, z, 1, tx0, ty1, r, g, b, a);
		addVertex(x1, y0, z, 1, tx1, ty0, r, g, b, a);
		addVertex(x1, y1, z, 1, tx1, ty1, r, g, b, a);
		numQuads++;
	}

	private IFn renderFn = loadClojureFn("foolish.render", "render-sprites");


	protected void display(GL3 gl3) {
		// GL2 gl = (GL2) gl3;

		gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 1.0f));
		gl3.glClearBufferfv(GL_DEPTH, 0, clearColor.put(0, 1.0f));

		gl3.glUseProgram(theProgram);

		// setup texture
		texture.bind(gl3);
		gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

		// set up vertex attribute arrays
		numQuads = 0;
		vertexBuffer.position(0);

		double t = getTime();
		double dt = t - lastTime;
		lastTime = t;
		double scrollFrac = Math.min(1.0, 1.0 - Math.exp(-dt));

		scrollPosX += scrollFrac * (scrollTargetX - scrollPosX);
		scrollPosY += scrollFrac * (scrollTargetY - scrollPosY);
		scrollX = (int) Math.round(scrollPosX);
		scrollY = (int) Math.round(scrollPosY);

		if (this.renderGameState!=null) {
			renderFn.invoke(this.renderGameState, this);
		}
		// testRender(t);
		// drawSprite(0,0,16,16,0.0f,0,0,16,16,Colours.WHITE);
		// drawSprite(32,32,32,32,32,32,32,32,Colours.WHITE);
		// drawSprite(64,64,32,32,32,32,32,32,Colours.DARK_BROWN);
		vertexBuffer.position(0);

		gl3.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
		gl3.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);

		gl3.glEnableVertexAttribArray(ATTR_POSITION);
		gl3.glVertexAttribPointer(ATTR_POSITION, 4, GL_FLOAT, false, 10 * 4, 0);
		gl3.glEnableVertexAttribArray(ATTR_TEXTURE);
		gl3.glVertexAttribPointer(ATTR_TEXTURE, 2, GL_FLOAT, false, 10 * 4, 4 * 4);
		gl3.glEnableVertexAttribArray(ATTR_COLOUR);
		gl3.glVertexAttribPointer(ATTR_COLOUR, 4, GL_FLOAT, false, 10 * 4, 6 * 4);

		// enable alpha blending
		gl3.glEnable(GL_BLEND);
		gl3.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		gl3.glDrawArrays(GL_TRIANGLES, 0, numQuads * 6);

		gl3.glDisableVertexAttribArray(ATTR_POSITION);

		gl3.glUseProgram(0);
	}

	/**
	 * Gets the real time in seconds since the launch of the App
	 * Used for animation
	 * 
	 * @return
	 */
	public double getTime() {
		return (System.currentTimeMillis() - baseTime) * 0.001;
	}

	private final static IFn require = Clojure.var("clojure.core", "require");

	public static IFn loadClojureFn(String ns, String var) {
		if (require == null)
			throw new Error("Clojure not loaded?");
		require.invoke(Clojure.read(ns)); // note: requires a symbol
		return Clojure.var(ns, var);
	}

	@Override
	public final void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

		GL3 gl3 = drawable.getGL().getGL3();

		reshape(gl3, width, height);
	}

	protected void reshape(GL3 gl3, int width, int height) {
		GL2 gl = (GL2) gl3; // get the OpenGL 2 graphics context

		if (height == 0)
			height = 1; // prevent divide by zero
		// float aspect = (float) width / height;

		// Set the view port (display area) to cover the entire window
		gl.glViewport(0, 0, width, height);

		this.width = width;
		this.height = height;

		// Setup perspective projection, with aspect ratio matches viewport
		// gl.glMatrixMode(GL_PROJECTION); // choose projection matrix
		// gl.glLoadIdentity(); // reset projection matrix

		// Enable the model-view transform
		// gl.glMatrixMode(GL_MODELVIEW);
		// gl.glLoadIdentity(); // reset
		// gl.glOrtho(0, width, height, 0, 1, -1); // orthographic view
	}

	@Override
	public final void dispose(GLAutoDrawable drawable) {
		GL3 gl3 = glCanvas.getGL().getGL3();

		// BufferUtils.destroyDirectBuffer(clearColor);
		// BufferUtils.destroyDirectBuffer(clearDepth);
		// BufferUtils.destroyDirectBuffer(matBuffer);
		// BufferUtils.destroyDirectBuffer(vecBuffer);

		gl3.glDeleteProgram(theProgram);
		gl3.glDeleteBuffers(1, positionBufferObject);
		gl3.glDeleteVertexArrays(1, vao);
		// animator.stop();
	}

	public boolean goingLeft=false;
	public boolean goingRight=false;
	public boolean jumping=false;
	
	@Override
	public void keyPressed(KeyEvent e) {

		switch (e.getKeyCode()) {
		  case KeyEvent.VK_ESCAPE:
			  jframe.dispose();
			  break;
			  
		  case KeyEvent.VK_LEFT:
			  goingLeft=true;
			  break;
			  
		  case KeyEvent.VK_RIGHT:
			  goingRight=true;
			  break;

		  case KeyEvent.VK_SPACE:
			  jumping=true;
			  break;
		}
	}

	private void shutDown() {
		running=false;
		animator.stop();
		
		if (!replLaunched) {
			// System.out.println("Shutting down agents...");
			// loadClojureFn("clojure.core","shutdown-agents").invoke();
			System.out.println("Exiting...");
			System.exit(0);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		
		switch (e.getKeyCode()) {			  
		  case KeyEvent.VK_LEFT:
			  goingLeft=false;
			  break;
			  
		  case KeyEvent.VK_RIGHT:
			  goingRight=false;
			  break;
			  
		  case KeyEvent.VK_SPACE:
			  jumping=false;
			  break;

		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		updateMouseLocation(e.getX(), e.getY());

	}

	private static final ArrayList<Object> events = new ArrayList<Object>();

	private void addGameEvent(Object ev) {
		synchronized (events) {
			events.add(ev);
		}
		// System.out.println("Event logged: "+ev.toString());
	}

	public List<Object> retrieveEvents() {
		ArrayList<Object> al = new ArrayList<Object>();
		synchronized (events) {
			al.addAll(events);
			events.clear();
		}
		// if (!al.isEmpty()) System.out.println("Events retrieved:
		// "+al.toString());
		return al;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		mouseDown = false;

	}

	@Override
	public void mouseExited(MouseEvent e) {
		mouseDown = false;

	}

	private void addClickEvent(int x, int y, int z) {
		IPersistentMap ev = PersistentHashMap.EMPTY;
		ev = ev.assoc(Keywords.EVENT, Keywords.CLICK);
		ev = ev.assoc(Keywords.X, cursorX);
		ev = ev.assoc(Keywords.Y, cursorY);
		ev = ev.assoc(Keywords.Z, cursorZ);
		addGameEvent(ev);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		updateMouseLocation(e.getX(), e.getY());

		if (e.getButton() == 1) {
			mouseDown = true;
			addClickEvent(cursorX, cursorY, cursorZ);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == 1) {
			mouseDown = false;
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		updateMouseLocation(e.getX(), e.getY());
	}

	int lastX = 0;
	int lastY = 0;

	private boolean updateMouseLocation(int sx, int sy) {
		lastX = sx;
		lastY = sy;
		// compute pixel co-ordinates relative to slice origin (0,0,z)
		int px = (int) ((sx + scrollX) / DRAW_SCALE);
		int py = (int) ((sy + scrollY) / DRAW_SCALE) + 32 * cursorZ;
		int newCursorX = px;
		int newCursorY = py;

		boolean moved = (newCursorX != cursorX) || (newCursorY != cursorY);

		if (moved) {
			cursorX = newCursorX;
			cursorY = newCursorY;
		}
		return moved;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		int dx = x - lastX;
		int dy = y - lastY;
		boolean moved = updateMouseLocation(x, y);

		if (mouseDown) {
			if (moved) {
				addClickEvent(cursorX, cursorY, cursorZ);
			}
		} else {
			// update scroll target with some acceleration
			scrollPosX -= dx;
			scrollPosY -= dy;
			scrollTargetX -= 2 * dx;
			scrollTargetY -= 2 * dy;
		}
	}

	public static void main(String[] args) {
		IFn clojureMain = loadClojureFn("foolish.main", "-main");
		clojureMain.invoke("Launched from Java");
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int notches = e.getWheelRotation();
		int x=e.getX();
		int y=e.getY();
		if (e.isControlDown()) {
			double scale=1.0;
			if ((notches < 0) && (DRAW_SCALE < 4.0f))
				scale *= 1.2;
			if ((notches > 0) && (DRAW_SCALE > 0.25f))
				scale /= 1.2;
			DRAW_SCALE*=scale;
			scrollPosX=(scrollPosX+x)*scale-x;
			scrollPosY=(scrollPosY+y)*scale-y;
			scrollTargetX=(int) scrollPosX;
			scrollTargetY=(int) scrollPosY;
			
		} else {
			setLevel(cursorZ - notches);
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	public void setLevel(int newZ) {
		int dz = newZ - cursorZ;
		cursorZ += dz;
		scrollTargetY -= (dz * 32 * DRAW_SCALE);

	}
}
