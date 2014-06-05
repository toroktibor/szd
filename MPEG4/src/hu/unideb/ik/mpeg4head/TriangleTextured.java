package hu.unideb.ik.mpeg4head;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

/**
 * A two-dimensional triangle for use as a drawn object in OpenGL ES 2.0.
 */
public class TriangleTextured {

	private final String vertexShaderCode = 
			"uniform mat4 u_MVPMatrix;"
			+ "attribute vec4 a_Position;" 
			+ "attribute vec2 a_TexCoordinate;"
			+ "varying vec2 v_TexCoordinate;" 
			+ "void main() {"
			+ "v_TexCoordinate = a_TexCoordinate;"
			+ "  gl_Position = u_MVPMatrix * a_Position;" 
			+ "}";

	private final String fragmentShaderCode = "precision mediump float;"
			+ "uniform sampler2D u_TextureSampler;"
			+ "varying vec2 v_TexCoordinate;" 
			+ "void main() {"
			+ "gl_FragColor = texture2D(u_TextureSampler, v_TexCoordinate);"
			+ "}";

	private Context context;
	private AssetManager mgr;
	
	private FloatBuffer nonInterleavedBuffer;
	private FloatBuffer interleavedBuffer;

	private final int mProgram;
	private final int vertexShader;
	private final int fragmentShader;
	
	private int a_PositionHandle;
	private int a_TexcoordHandle;
	private int u_MVPMatrixHandle;
	private int u_TextureSamplerHandle;
	
	private static int[] textures = new int[1];
	private int[] buffers = new int[1];

	private static final boolean INTERLEAVED = false;
	// number of coordinates per vertex in this array
	static final int COORDS_PER_VERTEX = 3;
	static final int COORDS_PER_TEXCOORD = 2;

	static float triangleCoords[] = {
			0.0f, 0.622008459f, 0.0f, 	// top
			-0.5f, -0.311004243f, 0.0f, // bottom left
			0.5f, -0.311004243f, 0.0f 	// bottom right
	};

	static float triangleTexcoords[] = {
		0.33f, 0.0f,
		0.0f, 0.33f,
		0.0f, 0.66f };

	static float interleavedStuffz[] = { 
			0.0f, 0.622008459f, 0.0f, 	// coordinates of top vertex
			0.33f, 0.0f, 				// texcoords for top vertex
			-0.5f, -0.311004243f, 0.0f, // coordinates of bottom left vertex
			0.0f, 0.33f, 				// texcoords for bottom left vertex
			0.5f, -0.311004243f, 0.0f, 	// coordinates of bottom right
			0.0f, 0.66f 				// texcoords for bottom right vertex
	};

	/*Általános cucc, bármely rajzolási technikához: a kirajzolt vertexek száma. */
	private final int numberOfVertices = triangleCoords.length / COORDS_PER_VERTEX;

	/* Segédadatok a Non-Interleaved bufferes rajzoláshoz.
	 * stride: hány bájtonként kezdődik az újabb csomag adat, pl. újabb vertexkoordináta-hármas, vagy UV koordinátapár
	 * Mivel nincsenek keverve az adatok, ezért a bufferben elől lesznek a vertex koordináták, majd csak utánuk
	 * az UV koordináták, így 2 vertexkoordináta-hármas között pont annyi byte lesz, ahány
	 * byte-ot lefoglal egy ilyen értékhármas, és hasonlóan az UV koordinátáknál is.
	 * 
	 * offset: a bufferben hányadik bájton kezdődik az adott jellegű adat első előfordulása.
	 * Mivel a vertexkoordinátákkal kezdjük a tömböt, annak offset-je 0 lesz.
	 * Mivel minden vertexkoordináta után kezdődik az UV koordináta halmaz, ezért offset-je
	 * pontosa a kirajzolt vertexek száma szorozva a vertexenkénti koordináták számával, és az
	 * szorozva a koordinátákat tároló float elemekek által foglalt byte-okkal. */
	private final int vertexStrideNonInterleaved = COORDS_PER_VERTEX * TypeSizes.BYTES_PER_FLOAT;
	//private final int vertexOffsetNonInterleaved = 0;
	
	private final int texcoordStrideNonInterleaved = COORDS_PER_TEXCOORD * TypeSizes.BYTES_PER_FLOAT;
	//private final int texcoordOffsetNonInterleaved = numberOfVertices * COORDS_PER_VERTEX * TypeSizes.BYTES_PER_FLOAT;

	/* Segédadatok az Interleaved bufferes rajzoláshoz.
	 * stride: hány bájtonként kezdődik az újabb csomag adat, pl. újabb vertexkoordináta-hármas, vagy UV koordinátapár
	 * Mivel az adatok vertexenként vannak csoportosítva, egybefésülve, ezért minden vertexkoordináta-hármas illetve minden
	 * UV koordinátapár között pontosan annyi hely lesz, amennyi byte-ot lefoglal az egy vertexhez tartozó összes információ. 
	 * 
	 * offset: a bufferben hányadik bájton kezdődik az adott jellegű adat első előfordulása.
	 * Mivel egy vertexkoordinátával kezdjük a tömböt, annak offset-je 0 lesz.
	 * Mivel az első vertexkoordináta után jön a hozzá tartozó UV koordinátapár, ezért offset-je
	 * a vertexenkénti koordináták száma szorozva a koordinátákat tároló float elemekek által foglalt byte-okkal. */
	private final int vertexStrideInterleaved = (COORDS_PER_VERTEX + COORDS_PER_TEXCOORD) * TypeSizes.BYTES_PER_FLOAT;
	//private final int vertexOffsetInterleaved = 0;
	
	private final int texcoordStrideInterleaved = (COORDS_PER_VERTEX + COORDS_PER_TEXCOORD) * TypeSizes.BYTES_PER_FLOAT;
	//private final int texcoordOffsetInterleaved = COORDS_PER_VERTEX	* TypeSizes.BYTES_PER_FLOAT;

	float[] tester = new float[4];
	float[] testresult = new float[4];
	
	public TriangleTextured(Context ctx) {
		this.context = ctx;
		this.mgr = context.getAssets();

		vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

		mProgram = GLES20.glCreateProgram(); 
		GLES20.glAttachShader(mProgram, vertexShader); 
		GLES20.glAttachShader(mProgram, fragmentShader); 
		GLES20.glLinkProgram(mProgram); 
		GLES20.glUseProgram(mProgram);
		Log.e("glGetProgramInfo:", GLES20.glGetProgramInfoLog(mProgram));
		prepareHandles();
		loadTextureFromAssets();
		
		GLES20.glGenBuffers(1, buffers, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);		
		
		if (INTERLEAVED) {
			buildInterleavedBuffer();
			prepareAttributesForInterleavedData();
			
		} else {
			buildNonInterleavedBuffers();
			prepareAttributesForNonInterleavedData();
		}		
	}
	
	public void prepareHandles() {
		a_PositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
		//MyGLRenderer.checkGlError("glGetAttribLocation");
		
		a_TexcoordHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
		//MyGLRenderer.checkGlError("glGetAttribLocation");
		
		u_MVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
		//MyGLRenderer.checkGlError("glGetUniformLocation");
		
		u_TextureSamplerHandle = GLES20.glGetUniformLocation(mProgram, "u_TextureSampler");
		//MyGLRenderer.checkGlError("glGetUniformLocation");
	}

	private void buildInterleavedBuffer() {
		ByteBuffer bb = ByteBuffer.allocateDirect(interleavedStuffz.length
				* TypeSizes.BYTES_PER_FLOAT);
		bb.order(ByteOrder.nativeOrder());
		interleavedBuffer = bb.asFloatBuffer();
		interleavedBuffer.put(interleavedStuffz);
		interleavedBuffer.position(0);
	}
	
	private void buildNonInterleavedBuffers() {
		ByteBuffer bb = ByteBuffer.allocateDirect((triangleCoords.length + triangleTexcoords.length)
				* TypeSizes.BYTES_PER_FLOAT);
		bb.order(ByteOrder.nativeOrder());
		nonInterleavedBuffer = bb.asFloatBuffer();
		nonInterleavedBuffer.put(triangleCoords);
		nonInterleavedBuffer.put(triangleTexcoords);
		nonInterleavedBuffer.position(0);
	}
	
	private void prepareAttributesForInterleavedData() {
		GLES20.glUseProgram(mProgram);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
		
		interleavedBuffer.position(0);
		GLES20.glVertexAttribPointer(a_PositionHandle, 
									COORDS_PER_VERTEX, 
									GLES20.GL_FLOAT, 
									false, 
									vertexStrideInterleaved, 
									interleavedBuffer);
		GLES20.glEnableVertexAttribArray(a_PositionHandle);		
		
		interleavedBuffer.position(COORDS_PER_VERTEX * numberOfVertices);
		GLES20.glVertexAttribPointer(a_TexcoordHandle, 
									COORDS_PER_TEXCOORD, 
									GLES20.GL_FLOAT, 
									false, 
									texcoordStrideInterleaved, 
									interleavedBuffer);
		GLES20.glEnableVertexAttribArray(a_TexcoordHandle);
	}
	
	private void prepareAttributesForNonInterleavedData() {
		GLES20.glUseProgram(mProgram);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
		
		nonInterleavedBuffer.position(0);
		GLES20.glVertexAttribPointer(a_PositionHandle, 
									COORDS_PER_VERTEX, 
									GLES20.GL_FLOAT, 
									false, 
									vertexStrideNonInterleaved, 
									nonInterleavedBuffer);
		GLES20.glEnableVertexAttribArray(a_PositionHandle);		
		
		nonInterleavedBuffer.position(COORDS_PER_VERTEX);
		GLES20.glVertexAttribPointer(a_TexcoordHandle, 
									COORDS_PER_TEXCOORD, 
									GLES20.GL_FLOAT, 
									false, 
									texcoordStrideNonInterleaved, 
									nonInterleavedBuffer);
		GLES20.glEnableVertexAttribArray(a_TexcoordHandle);
	}
	
	private void loadTextureFromAssets() {
		//Get the texture from the Android resource directory
		
		InputStream is = null;
		try {
			String textureFileName = "0.png";
			is = mgr.open(textureFileName);
			Log.e("Triangle2", "Loading texture file: " + textureFileName);
		} catch (IOException e1) {
			Log.e("Triangle2", "Failed to open the texture file!!!");
			e1.printStackTrace();
		}

		Bitmap bitmap = null;
		try {
			//BitmapFactory is an Android graphics utility for images
			bitmap = BitmapFactory.decodeStream(is);

		} finally {
			//Always clear and close
			try {
				is.close();
				is = null;
			} catch (IOException e) {
			}
		}

		// Generate one texture pointer...
				GLES20.glGenTextures(1, textures, 0);
				u_TextureSamplerHandle = textures[0];
				// ...and bind it to our array
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, u_TextureSamplerHandle);
				
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

			    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
			    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
				
			    // Create Nearest Filtered Texture
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

				// Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

				// Use the Android GLUtils to specify a two-dimensional texture image
				// from our bitmap
				//Log.e("Mesh", "Bitmag width × height = " + bitmap.getWidth() + " × " + bitmap.getHeight());
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

				// Clean up
				bitmap.recycle();
			}

	private void logDrawnVertexCoordinates(float[] mvpMatrix) {
		if(INTERLEAVED) {
			for(int i = 0; i < 3; ++i)
				tester[i] = interleavedBuffer.get(i);
			}
		else {
			for(int i = 0; i < 3; ++i)
				tester[i] = nonInterleavedBuffer.get(i);
		}
		tester[3] = 1.0f;
		Matrix.multiplyMV(testresult, 0, mvpMatrix, 0, tester, 0);
		Log.e("Triangle2.draw() testvalues:", testresult[0] + "   " + testresult[1] + "   " +testresult[2] + "   " + testresult[3]);
	}
	
	public void draw(float[] mvpMatrix) {
		GLES20.glUseProgram(mProgram);

		//GLES20.glEnable(GLES20.GL_TEXTURE_2D);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		
		//MyGLRenderer.checkGlError("glUniformMatrix4fv");
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, u_TextureSamplerHandle);
		//MyGLRenderer.checkGlError("glBindTexture");
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glUniform1i(u_TextureSamplerHandle, 0);
		
		GLES20.glUniformMatrix4fv(u_MVPMatrixHandle, 1, false, mvpMatrix, 0);
		
		//MyGLRenderer.logMatrix("mvpMatrix", mvpMatrix);
		
		//logDrawnVertexCoordinates(mvpMatrix);
		
		// Draw the triangle
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numberOfVertices);
		//MyGLRenderer.checkGlError("glDrawArrays");
	}
}