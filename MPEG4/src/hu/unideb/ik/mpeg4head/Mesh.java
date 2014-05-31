package hu.unideb.ik.mpeg4head;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

public class Mesh {
	
	/* Number of coordinates per vertex. */
	private static final int COORDS_PER_VERTEX = 3;
	private static final int COORDS_PER_TEXCOORDS = 2;
	private static final int STRIDE_OF_ATTRIBS = (COORDS_PER_TEXCOORDS + COORDS_PER_VERTEX)
			* TypeSizes.BYTES_PER_FLOAT;
	private static final int VERTEX_OFFSET = 0;
	private static final int TEXCOORD_OFFSET = COORDS_PER_VERTEX
			* TypeSizes.BYTES_PER_FLOAT;
	private static final String TAG = "Mesh";

	/*
	 * Helpers to get the files from assets containing the source code of the
	 * shaders.
	 */
	private AssetManager mgr;
	private Context context;
	private Material myMaterial;

	/* These strings will be containing the source code of the shaders. */
	private String vertexShaderCode;
	private String fragmentShaderCode;

	/* Handles for the shaders and the program. */
	private int vertexShaderHandle;
	private int fragmentShaderHandle;
	private int mProgram;

	/* Handles for the attributes, uniforms of the program. */
	private int a_PositionHandle;
	private int a_TexCoordinateHandle;
	private int u_MVPMatrixHandle;
	private int u_TextureSamplerHandle;

	Vector<Float> v;
	Vector<Float> vt;
	Vector<Integer> faces;
	Vector<Short> vtPointer;

	FloatBuffer vertexBuffer;
	FloatBuffer texcoordsBuffer;
	FloatBuffer vboBuffer;

	private int[] textures = new int[1];
	private int[] vbo = new int[1];
	
	
	float[] temp = new float[4];
	float[] res = new float[4];

	public Mesh(Context ctx, Vector<Float> v, Vector<Float> vt,
			Vector<Integer> faces, Vector<Short> vtPointer, Material mat) {
		super();
		context = ctx;
		mgr = context.getAssets();
		this.v = v;
		this.vt = vt;
		this.faces = faces;
		this.vtPointer = vtPointer;
		this.myMaterial = mat;

		GLES20.glGenBuffers(1, vbo, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);

		buildOneInterleavedBuffer();
		prepareShaders("vertexShaderNew.vsh", "fragmentShaderNew.fsh");
		prepareHandles();
		prepareAttributes();
		loadTextureFromAssets();
	}

/*	public void buildDifferentBuffers() {
		ByteBuffer vBuf = ByteBuffer.allocateDirect(v.size()
				* TypeSizes.BYTES_PER_FLOAT);
		vBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = vBuf.asFloatBuffer();
		vertexBuffer.put(ArrayBufferConverter.getFloatArrayFromFloatVector(v));
		vertexBuffer.position(0);

		ByteBuffer tcBuf = ByteBuffer.allocateDirect(vt.size()
				* TypeSizes.BYTES_PER_FLOAT);
		tcBuf.order(ByteOrder.nativeOrder());
		texcoordsBuffer = tcBuf.asFloatBuffer();
		texcoordsBuffer.put(ArrayBufferConverter
				.getFloatArrayFromFloatVector(vt));
		texcoordsBuffer.position(0);
	}

	public void buildOneBuffer() {
		ByteBuffer vAndVtBuf = ByteBuffer.allocateDirect(v.size()
				* TypeSizes.BYTES_PER_FLOAT + vt.size()
				* TypeSizes.BYTES_PER_FLOAT);
		vAndVtBuf.order(ByteOrder.nativeOrder()); // csak egy nagy buffert
													// készítünk
		vboBuffer = vAndVtBuf.asFloatBuffer();
		vboBuffer.put(ArrayBufferConverter.getFloatArrayFromFloatVector(v)); // beletöltjük
																				// először
																				// a
																				// vertex
																				// koodrinátákat
		vboBuffer.put(ArrayBufferConverter.getFloatArrayFromFloatVector(vt)); // aztán
																				// a
																				// textúra
																				// koordinátákat
		vboBuffer.position(0);
	}
*/

	public void buildOneInterleavedBuffer() {
		/*
		 * bufferItems-ben tároljuk az indexek számát. a buffer lefoglalásához
		 * szükség van arra, hogy ezt felszorozzuk annyival, amennyi byte
		 * információ van egy vertexről. ez magában foglalja a 3 floatot, az x,
		 * y és z vertex koordinátákat továbbá 2 floatot, az u és v textúra
		 * koordinátákat
		 */
		final int bufferItems = faces.size();
		//Log.d("buildOneInterleavedBuffer", "faces.size()=" + faces.size());

		final int bufferSize = bufferItems
				* (COORDS_PER_TEXCOORDS + COORDS_PER_VERTEX)
				* TypeSizes.BYTES_PER_FLOAT;
		Log.d("buildOneInterleavedBuffer",
				"Összesen ennyi bájtot foglalunk le: " + bufferSize);

		ByteBuffer vAndVtBuf = ByteBuffer.allocateDirect(bufferSize);
		vAndVtBuf.order(ByteOrder.nativeOrder());
		vboBuffer = vAndVtBuf.asFloatBuffer();
		float[] temp1 = ArrayBufferConverter.getFloatArrayFromFloatVector(v);
		float[] temp2 = ArrayBufferConverter.getFloatArrayFromFloatVector(vt);
		// Log.d(TAG, "1. érték: " + temp1[faces.get(0)]);
		int actualVertexIndex;
		int actualTexcoordIndex;
		for (int i = 0; i < bufferItems / 3; ++i) {
			actualVertexIndex = i * COORDS_PER_VERTEX;
			//Log.d(TAG, "actualVertexIndex=" + actualVertexIndex);
			vboBuffer.put(temp1[faces.get(actualVertexIndex)]);
			//Log.d(TAG, "actualVertexIndex=" + (actualVertexIndex + 1));
			vboBuffer.put(temp1[faces.get(actualVertexIndex + 1)]);
			//Log.d(TAG, "actualVertexIndex=" + (actualVertexIndex + 2));
			vboBuffer.put(temp1[faces.get(actualVertexIndex + 2)]);

			actualTexcoordIndex = i * COORDS_PER_TEXCOORDS;
			//Log.d(TAG, "actualTexcoordIndex=" + actualTexcoordIndex);
			vboBuffer.put(temp2[vtPointer.get(actualTexcoordIndex)]);
			//Log.d(TAG, "actualTexcoordIndex=" + (actualTexcoordIndex + 1));
			vboBuffer.put(temp2[vtPointer.get(actualTexcoordIndex + 1)]);
		}
		Log.d(TAG, "Loading vboBuffer finished.");
		vboBuffer.position(0);
		temp[0] = vboBuffer.get(); temp[1] = vboBuffer.get();
		temp[2] = vboBuffer.get();	temp[3] = 0.0f;
		vboBuffer.position(0);
	}

	private String readShaderCodeFromFile(String fileName) {
		String line = null;
		BufferedReader reader = null;
		StringBuilder sb = new StringBuilder();
		try {
			reader = new BufferedReader(new InputStreamReader(
					mgr.open(fileName)));
		} catch (IOException e) {
			Log.e("Mesh.java->readShaderCodeFromFile()",
					"Unsuccessful file reading.");
			e.printStackTrace();
		}
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Log.d("readShaderCodeFromFile", sb.toString());
		return sb.toString();
	}

	public static int loadShader(int type, String shaderCode) {

		int shader = GLES20.glCreateShader(type);

		GLES20.glShaderSource(shader, shaderCode);
		MyGLRenderer.checkGlError("glShaderSource");
		
		GLES20.glCompileShader(shader);
		MyGLRenderer.checkGlError("glCompileShader");
		
		return shader;
	}

	private void prepareShaders(String vshFile, String fshFile) {
		vertexShaderCode = readShaderCodeFromFile(vshFile);
		fragmentShaderCode = readShaderCodeFromFile(fshFile);

		vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
		
		mProgram = GLES20.glCreateProgram();
		MyGLRenderer.checkGlError("glCreateProgram");
		
		GLES20.glAttachShader(mProgram, vertexShaderHandle);
		MyGLRenderer.checkGlError("glAttachShader");
		
		GLES20.glAttachShader(mProgram, fragmentShaderHandle);
		MyGLRenderer.checkGlError("glAttachShader");
		
		GLES20.glLinkProgram(mProgram);
		
		Log.e("glGetShaderInfoLog:", GLES20.glGetShaderInfoLog(fragmentShaderHandle));
		/*
		int[] params = new int[] { 100 };
		GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, params, 0);
		if(params[0] == GLES20.GL_TRUE) {
			Log.e(TAG, "NINCS BAJ A LINKELÉSSEL!");
			Log.e(TAG, "GLES20.GL_TRUE=" + GLES20.GL_TRUE);
			Log.e(TAG, "params[0]=" + params[0]);
		}
		else if(params[0] == GLES20.GL_FALSE) {
			Log.e(TAG, "BAJ VAN A LINKELÉSSEL!");
			Log.e(TAG, "GLES20.GL_FALSE=" + GLES20.GL_FALSE);
			Log.e(TAG, "params[0]=" + params[0]);
		}
		Log.e("glGetProgramInfo:", GLES20.glGetProgramInfoLog(mProgram));*/
	}

	private void prepareHandles() {
		/*if( GLES20.glIsProgram(mProgram) != true) {
			Log.e(TAG, "Ez a program nem program! Hiba történt-->" + GLES20.glIsProgram(mProgram));
		}
		else {
			Log.e(TAG, "Ez a program jó program! Nem történt hiba.");
		}*/
		a_PositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
		a_TexCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
		u_MVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
		u_TextureSamplerHandle = GLES20.glGetUniformLocation(mProgram, "u_TextureSampler");
	}

	private void prepareAttributes() {

/*		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				(COORDS_PER_VERTEX + COORDS_PER_TEXCOORDS)
						* TypeSizes.BYTES_PER_FLOAT, vboBuffer,
				GLES20.GL_STATIC_DRAW);

		GLES20.glVertexAttribPointer(a_PositionHandle, COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, false, STRIDE_OF_ATTRIBS, VERTEX_OFFSET);

		GLES20.glVertexAttribPointer(a_TexCoordinateHandle,
				COORDS_PER_TEXCOORDS, GLES20.GL_FLOAT, false,
				STRIDE_OF_ATTRIBS, TEXCOORD_OFFSET);
*/
		GLES20.glUseProgram(mProgram);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);
		/*
		vboBuffer.position(0);
		for(int i = 0; i < 100;++i) {
			Log.d("Vertex#" + i, vboBuffer.get() + " " + vboBuffer.get() + " " + vboBuffer.get());
			Log.d("Texcoord#" + i, vboBuffer.get() + " " + vboBuffer.get());
		}*/
		
		vboBuffer.position(0);
		GLES20.glVertexAttribPointer(a_PositionHandle, 
									COORDS_PER_VERTEX, 
									GLES20.GL_FLOAT, 
									false, 
									STRIDE_OF_ATTRIBS, 
									vboBuffer);
		GLES20.glEnableVertexAttribArray(a_PositionHandle);		
		
		vboBuffer.position(COORDS_PER_VERTEX);
		GLES20.glVertexAttribPointer(a_TexCoordinateHandle, 
									COORDS_PER_TEXCOORDS, 
									GLES20.GL_FLOAT, 
									false, 
									STRIDE_OF_ATTRIBS, 
									vboBuffer);
		GLES20.glEnableVertexAttribArray(a_TexCoordinateHandle);
	}

	public void loadTextureFromAssets() {
		// Get the texture from the Android resource directory

		InputStream is = null;
		try {
			String textureFileName = myMaterial.getTextureFile();
			is = mgr.open(textureFileName);
			Log.e(TAG, "Loading texture file: " + textureFileName);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// InputStream is =
		// context.getResources().openRawResource(R.drawable.face_texture);
		Bitmap bitmap = null;
		try {
			// BitmapFactory is an Android graphics utility for images
			bitmap = BitmapFactory.decodeStream(is);

		} finally {
			// Always clear and close
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
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

		// Clean up
		bitmap.recycle();
	}

	public void draw(float[] mMVP) {
		GLES20.glUseProgram(mProgram);
		/*if(	(a_PositionHandle == -1) || 
			(a_TexCoordinateHandle == -1) || 
			(u_MVPMatrixHandle == -1) ||
			(u_TextureSamplerHandle == -1)) {
			Log.e(TAG, "Attribute vagy Uniform nem lett helyesen megtalálva a shaderprogramban!!!!");
		}*/

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		//MyGLRenderer.checkGlError("glEnable");
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);
		//MyGLRenderer.checkGlError("glBindBuffer");
		
	    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, u_TextureSamplerHandle);
		GLES20.glUniform1i(u_TextureSamplerHandle, 0);
		//MyGLRenderer.checkGlError("glBindTexture");
		
		GLES20.glUniformMatrix4fv(u_MVPMatrixHandle, 1, false, mMVP, 0);

		/* Egy kis ellenőrzés.... */
		//Matrix.multiplyMV(res, 0, mMVP, 0, temp, 0);
		//Log.e("MVP * vertices[0]=", res[0] + " " + res[1] + " " + res[2]);
 		/* Ellenőrzés vége. */
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, faces.size() / COORDS_PER_VERTEX);
		//MyGLRenderer.checkGlError("glDrawArrays");
	}

	public String toString() {
		String str = new String();
		// str+="Number of parts: "+parts.size();
		str += "\nNumber of vertexes: " + v.size();
		// str+="\nNumber of vns: "+vn.size();
		str += "\nNumber of vts: " + vt.size();
		/*
		 * str+="\n/////////////////////////\n"; for(int i=0; i<parts.size();
		 * i++){ str+="Part "+i+'\n'; str+=parts.get(i).toString();
		 * str+="\n/////////////////////////"; }
		 */
		return str;
	}
}
