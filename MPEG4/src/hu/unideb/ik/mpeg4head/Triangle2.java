package hu.unideb.ik.mpeg4head;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * A two-dimensional triangle for use as a drawn object in OpenGL ES 2.0.
 */
public class Triangle2 {

	private final String vertexShaderCode = "uniform mat4 u_MVPMatrix;"
			+ "attribute vec4 a_Position;" + "attribute vec2 a_TexCoordinate;"
			+ "varying vec2 v_TexCoordinate;" + "void main() {"
			+ "v_TexCoordinate = a_TexCoordinate;"
			+ "  gl_Position = uMVPMatrix * vPosition;" + "}";

	private final String fragmentShaderCode = "precision mediump float;"
			+ "uniform sampler2D u_TextureSampler;"
			+ "varying vec2 v_TexCoordinate;" + "void main() {"
			+ "gl_FragColor = texture2D(u_TextureSampler, v_TexCoordinate);"
			+ "}";

	private Context context;
	private AssetManager mgr;
	
	private FloatBuffer vertexBuffer;
	private FloatBuffer texcoordBuffer;
	private FloatBuffer interleavedBuffer;

	private final int mProgram;
	private int mPositionHandle;
	private int mTexcoordHandle;
	private int mMVPMatrixHandle;
	private int textureHandle;
	private static int[] textures = new int[1];
	private int[] buffers = new int[2];

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
	private final int vertexStrideNonIterleaved = COORDS_PER_VERTEX * TypeSizes.BYTES_PER_FLOAT;
	private final int vertexOffsetNonInterleaved = 0;
	
	private final int texcoordStrideNonIterleaved = COORDS_PER_TEXCOORD * TypeSizes.BYTES_PER_FLOAT;
	private final int texcoordOffsetNonInterleaved = numberOfVertices * COORDS_PER_VERTEX * TypeSizes.BYTES_PER_FLOAT;

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
	private final int vertexOffsetInterleaved = 0;
	
	private final int texcoordStrideInterleaved = (COORDS_PER_VERTEX + COORDS_PER_TEXCOORD) * TypeSizes.BYTES_PER_FLOAT;
	private final int texcoordOffsetInterleaved = COORDS_PER_VERTEX	* TypeSizes.BYTES_PER_FLOAT;

	private void buildDifferentBuffers() {
		ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length
				* TypeSizes.BYTES_PER_FLOAT);
		bb.order(ByteOrder.nativeOrder());
		vertexBuffer = bb.asFloatBuffer();
		vertexBuffer.put(triangleCoords);
		vertexBuffer.position(0);

		ByteBuffer bb2 = ByteBuffer.allocateDirect(triangleTexcoords.length
				* TypeSizes.BYTES_PER_FLOAT);
		bb2.order(ByteOrder.nativeOrder());
		texcoordBuffer = bb2.asFloatBuffer();
		texcoordBuffer.put(triangleTexcoords);
		texcoordBuffer.position(0);
	}

	private void buildInterleavedBuffer() {
		ByteBuffer bb = ByteBuffer.allocateDirect(interleavedStuffz.length
				* TypeSizes.BYTES_PER_FLOAT);
		bb.order(ByteOrder.nativeOrder());
		interleavedBuffer = bb.asFloatBuffer();
		interleavedBuffer.put(interleavedStuffz);
		interleavedBuffer.position(0);
	}

	public Triangle2(Context ctx) {
		this.context = ctx;
		this.mgr = context.getAssets();

		int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

		mProgram = GLES20.glCreateProgram(); 
		GLES20.glAttachShader(mProgram, vertexShader); 
		GLES20.glAttachShader(mProgram, fragmentShader); 
		GLES20.glLinkProgram(mProgram); 
		
		GLES20.glUseProgram(mProgram);
		
		if (INTERLEAVED) {
			buildInterleavedBuffer();
		} else {
			buildDifferentBuffers();
			GLES20.glGenBuffers(1, buffers, 0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 
					numberOfVertices * (COORDS_PER_TEXCOORD + COORDS_PER_VERTEX) * TypeSizes.BYTES_PER_FLOAT, 
					null, 
					GLES20.GL_STATIC_DRAW);
			GLES20.glBufferSubData(	GLES20.GL_ARRAY_BUFFER, 
									vertexOffsetNonInterleaved, 
									numberOfVertices * COORDS_PER_VERTEX * TypeSizes.BYTES_PER_FLOAT, 
									vertexBuffer);
			
			GLES20.glBufferSubData(	GLES20.GL_ARRAY_BUFFER, 
									texcoordOffsetNonInterleaved, 
									numberOfVertices * COORDS_PER_TEXCOORD * TypeSizes.BYTES_PER_FLOAT, 
									texcoordBuffer);
		}

		getHandles();
		loadTextureFromAssets();
		
		if (INTERLEAVED) {
			// TODO!
		} else {/*
			GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
					GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
			MyGLRenderer.checkGlError("glVertexAttribPointer");
			
			GLES20.glVertexAttribPointer(mTexcoordHandle, COORDS_PER_TEXCOORD,
					GLES20.GL_FLOAT, false, texcoordStride, texcoordBuffer);
			MyGLRenderer.checkGlError("glVertexAttribPointer");
			*/
			/* ÚJ MEGOLDÁSI MÓDSZER!!! */
			
		}
		
		
	}
	
	public void getHandles() {
		mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
		//MyGLRenderer.checkGlError("glGetAttribLocation");
		
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		//MyGLRenderer.checkGlError("glEnableVertexAttribArray");
		
		mTexcoordHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
		//MyGLRenderer.checkGlError("glGetAttribLocation");
		
		GLES20.glEnableVertexAttribArray(mTexcoordHandle);
		//MyGLRenderer.checkGlError("glEnableVertexAttribArray");
		
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
		//MyGLRenderer.checkGlError("glGetUniformLocation");
		
		textureHandle = GLES20.glGetUniformLocation(mProgram, "u_TextureSampler");
		//MyGLRenderer.checkGlError("glGetUniformLocation");
	}

	public void draw(float[] mvpMatrix) {
		GLES20.glUseProgram(mProgram);

		GLES20.glEnable(GLES20.GL_TEXTURE_2D);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
		MyGLRenderer.checkGlError("glUniformMatrix4fv");
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
		MyGLRenderer.checkGlError("glBindTexture");
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glUniform1i(textureHandle, 0);
		
		// Draw the triangle
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numberOfVertices);
		MyGLRenderer.checkGlError("glDrawArrays");
	}
	
	public void loadTextureFromAssets() {
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

		//Generate one texture pointer...
		GLES20.glGenTextures(1, textures, 0);
		MyGLRenderer.checkGlError("glGenTextures");
		
		//...and bind it to our array
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
		MyGLRenderer.checkGlError("glBindTexture");
		
		//Create Nearest Filtered Texture
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		//Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
		
		//Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		
		//Clean up
		bitmap.recycle();
	}

}