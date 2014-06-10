package hu.unideb.ik.mpeg4head;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class MeshOnlyColored {
	
	/* Number of coordinates per vertex. */
	private static final int COORDS_PER_VERTEX = 3;
	private static final int STRIDE_OF_ATTRIBS = COORDS_PER_VERTEX	* TypeSizes.BYTES_PER_FLOAT;
	
	private static final String TAG = "MeshOnlyColored";

	/*
	 * Helpers to get the files from assets containing the source code of the
	 * shaders.
	 */
	private AssetManager mgr;
	private Context context;

	/* These strings will be containing the source code of the shaders. */
	private String vertexShaderCode;
	private String fragmentShaderCode;

	/* Handles for the shaders and the program. */
	private int vertexShaderHandle;
	private int fragmentShaderHandle;
	private int mProgram;

	/* Handles for the attributes, uniforms of the program. */
	private int a_PositionHandle;
	private int u_MVPMatrixHandle;	
	private int u_ColorHandle;

	Vector<Float> v;
	Vector<Integer> faces;

	//FloatBuffer vertexBuffer;
	FloatBuffer vboBuffer;

	private int[] vbo = new int[1];
	
	
	float[] temp = new float[4];
	float[] res = new float[4];


	public MeshOnlyColored(Context ctx, Vector<Float> v, Vector<Integer> faces) {
		super();
		context = ctx;
		mgr = context.getAssets();
		this.v = v;
		this.faces = faces;

		GLES20.glGenBuffers(1, vbo, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);

		buildBuffer();
		prepareShadersAndProgram("vertexShaderMeshColored.glsl", "fragmentShaderMeshColored.glsl");
		prepareHandles();
		
		GLES20.glUseProgram(mProgram);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, a_PositionHandle);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboBuffer.capacity(), vboBuffer, GLES20.GL_STATIC_DRAW);
	}

	// MŰKÖDIK!!!
	public void buildBuffer() {
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
				* (COORDS_PER_VERTEX)
				* TypeSizes.BYTES_PER_FLOAT;
		Log.d("buildBuffer", "Összesen ennyi bájtot foglalunk le: " + bufferSize);

		ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(bufferSize);
		vertexByteBuffer.order(ByteOrder.nativeOrder());
		vboBuffer = vertexByteBuffer.asFloatBuffer();
		vboBuffer.put(ArrayBufferConverter.getFloatArrayFromFloatVector(v));

		Log.d(TAG, "Loading vboBuffer finished.");

		vboBuffer.position(0);
		temp[0] = vboBuffer.get(); temp[1] = vboBuffer.get();
		temp[2] = vboBuffer.get();	temp[3] = 1.0f;
		vboBuffer.position(0);
	}
	// JÓL MŰKÖDIK!!!
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
			e.printStackTrace();
		}
		//Log.d("readShaderCodeFromFile", sb.toString());
		return sb.toString();
	}
	// JÓL MŰKÖDIK!!!
	public static int loadShader(int type, String shaderCode) {

		int shader = GLES20.glCreateShader(type);

		GLES20.glShaderSource(shader, shaderCode);
		MyGLRenderer.checkGlError("glShaderSource");
		
		GLES20.glCompileShader(shader);
		MyGLRenderer.checkGlError("glCompileShader");
		
		return shader;
	}
	//JÓL MŰKÖDIK!!!
	private void prepareShadersAndProgram(String vshFile, String fshFile) {
		vertexShaderCode = readShaderCodeFromFile(vshFile);
		fragmentShaderCode = readShaderCodeFromFile(fshFile);

		vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
		
		mProgram = GLES20.glCreateProgram();
		//MyGLRenderer.checkGlError("glCreateProgram");
		
		GLES20.glAttachShader(mProgram, vertexShaderHandle);
		//MyGLRenderer.checkGlError("glAttachShader");
		
		GLES20.glAttachShader(mProgram, fragmentShaderHandle);
		//MyGLRenderer.checkGlError("glAttachShader");
		
		GLES20.glLinkProgram(mProgram);
		
		//Log.e("glGetShaderInfoLog:", GLES20.glGetShaderInfoLog(fragmentShaderHandle));
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
		}*/
		Log.e("glGetProgramInfo:", GLES20.glGetProgramInfoLog(mProgram));
	}
	// JÓL MŰKÖDIK!!!
	private void prepareHandles() {
		/*if( GLES20.glIsProgram(mProgram) != true) {
			Log.e(TAG, "Ez a program nem program! Hiba történt-->" + GLES20.glIsProgram(mProgram));
		}
		else {
			Log.e(TAG, "Ez a program jó program! Nem történt hiba.");
		}*/
		a_PositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
		u_MVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
		u_ColorHandle = GLES20.glGetUniformLocation(mProgram, "u_Color");
	}
	// FONTOS! MINDEN DRAW-BAN MEG KELL HÍVNI, KÜLÖNBEN NEM FOGUNK LÁTNI LÓSZERSZÁMOT SEM!!!
	private void prepareAttributes() {
		
		vboBuffer.position(0);
		GLES20.glVertexAttribPointer(a_PositionHandle, 
									COORDS_PER_VERTEX, 
									GLES20.GL_FLOAT, 
									false, 
									STRIDE_OF_ATTRIBS, 
									vboBuffer);
		GLES20.glEnableVertexAttribArray(a_PositionHandle);
	}


	public void draw(float[] mMVP) {
		GLES20.glUseProgram(mProgram);
		prepareAttributes();
	
		//MyGLRenderer.checkGlError("glBindTexture");
		
		GLES20.glUniformMatrix4fv(u_MVPMatrixHandle, 1, false, mMVP, 0);
		GLES20.glUniform4f(u_ColorHandle, 1.0f, 0.5f, 0.2f, 0.5f);

		/* Egy kis ellenőrzés.... */
		//Matrix.multiplyMV(res, 0, mMVP, 0, temp, 0);
		//Log.e("MVP * vertices[0]=", res[0] + " " + res[1] + " " + res[2]);
 		/* Ellenőrzés vége. */
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, faces.size() / COORDS_PER_VERTEX);
		//MyGLRenderer.checkGlError("glDrawArrays");
	}

	public String toString() {
		String str = new String();
		str += "\nNumber of vertexes: " + v.size();
		return str;
	}
}
