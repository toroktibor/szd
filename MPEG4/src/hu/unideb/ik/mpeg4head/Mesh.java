package hu.unideb.ik.mpeg4head;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;

import com.example.android.opengl.R;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class Mesh {

	/* Number of coordinates per vertex. */
	private static final int COORDS_PER_VERTEX = 3;
	private static final int COORDS_PER_TEXCOORDS = 2;
	private static final int STRIDE_OF_ATTRIBS = 5 * TypeSizes.BYTES_PER_FLOAT;
	private static final int VERTEX_OFFSET = 0;
	private static final int TEXCOORD_OFFSET = COORDS_PER_VERTEX * TypeSizes.BYTES_PER_FLOAT;
	private static final String TAG = "Mesh";
	
	
	/* Helpers to get the files from assets containing the source code of the shaders. */
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
	Vector<Float> vn;
	Vector<Float> vt;
	Vector<Integer> faces;
	Vector<Short> vtPointer;
	
	FloatBuffer vertexBuffer;
	FloatBuffer texcoordsBuffer;
	FloatBuffer vboBuffer;
	IntBuffer indexBuffer;
	
	private int[] textures = new int[1];
	private int indicesHandle;
	private int[] vbo = new int[1];

	public Mesh(Context ctx, Vector<Float> v, Vector<Float> vt, Vector<Integer> faces, Vector<Short> vtPointer, Material mat) {
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
		//buildDifferentBuffers();
		//buildOneBuffer();
		buildOneInterleavedBuffer();
		prepareShaders("vertexShaderNew.vsh", "fragmentShaderNew.fsh");
		prepareHandles();
		prepareAttributes();
	}

	public void buildDifferentBuffers(){
		ByteBuffer vBuf = ByteBuffer.allocateDirect(v.size() * TypeSizes.BYTES_PER_FLOAT);
		vBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = vBuf.asFloatBuffer();
		vertexBuffer.put(ArrayBufferConverter.getFloatArrayFromFloatVector(v));
		vertexBuffer.position(0);
		
		ByteBuffer tcBuf = ByteBuffer.allocateDirect(vt.size() * TypeSizes.BYTES_PER_FLOAT);
		tcBuf.order(ByteOrder.nativeOrder());
		texcoordsBuffer = tcBuf.asFloatBuffer();
		texcoordsBuffer.put(ArrayBufferConverter.getFloatArrayFromFloatVector(vt));
		texcoordsBuffer.position(0);
		
		ByteBuffer idxBuf = ByteBuffer.allocateDirect(faces.size() * TypeSizes.BYTES_PER_INTEGER);
		idxBuf.order(ByteOrder.nativeOrder());
		indexBuffer = idxBuf.asIntBuffer();
		indexBuffer.put(ArrayBufferConverter.getIntArrayFromIntVector(faces));
		indexBuffer.position(0);
	}
	
	public void buildOneBuffer() {
		ByteBuffer vAndVtBuf = 
				ByteBuffer.allocateDirect(	v.size() * TypeSizes.BYTES_PER_FLOAT +
											vt.size() * TypeSizes.BYTES_PER_FLOAT);
		vAndVtBuf.order(ByteOrder.nativeOrder());									//csak egy nagy buffert készítünk
		vboBuffer = vAndVtBuf.asFloatBuffer();
		vboBuffer.put(ArrayBufferConverter.getFloatArrayFromFloatVector(v));		//beletöltjük először a vertex koodrinátákat
		vboBuffer.put(ArrayBufferConverter.getFloatArrayFromFloatVector(vt));		//aztán a textúra koordinátákat
		vboBuffer.position(0);
	}
	
	public void buildOneInterleavedBuffer() {
		/* bufferItems-ben tároljuk az indexek számát.
		 * a buffer lefoglalásához szükség van arra, hogy ezt felszorozzuk
		 * annyival, amennyi byte információ van egy vertexről.
		 * ez magában foglalja a 3 floatot, az x, y és z vertex koordinátákat
		 * továbbá 2 floatot, az u és v textúra koordinátákat
		 * */
		final int bufferItems = faces.size();		
		Log.d("buildOneInterleavedBuffer", "faces.size()=" + faces.size());
		
		final int bufferSize = bufferItems * (COORDS_PER_TEXCOORDS + COORDS_PER_VERTEX) * TypeSizes.BYTES_PER_FLOAT;
		Log.d("buildOneInterleavedBuffer", "Összesen ennyi bájtot foglalunk le: " + bufferSize);
		
		ByteBuffer vAndVtBuf = 	ByteBuffer.allocateDirect(	bufferSize);
		vAndVtBuf.order(ByteOrder.nativeOrder());
		vboBuffer = vAndVtBuf.asFloatBuffer();
		float[] temp1 = ArrayBufferConverter.getFloatArrayFromFloatVector(v);
		float[] temp2 = ArrayBufferConverter.getFloatArrayFromFloatVector(vt);
		//Log.d(TAG, "1. érték: " + temp1[faces.get(0)]);
		int actualVertexIndex;
		int actualTexcoordIndex;
		for(int i = 0; i < bufferItems / 3; ++i) {
			actualVertexIndex = i * COORDS_PER_VERTEX;
			Log.d(TAG, "actualVertexIndex=" + actualVertexIndex);
			vboBuffer.put(temp1[faces.get(actualVertexIndex)]);
			Log.d(TAG, "actualVertexIndex=" + (actualVertexIndex+1));
			vboBuffer.put(temp1[faces.get(actualVertexIndex+1)]);
			Log.d(TAG, "actualVertexIndex=" + (actualVertexIndex+2));
			vboBuffer.put(temp1[faces.get(actualVertexIndex+2)]);
			
			actualTexcoordIndex = i * COORDS_PER_TEXCOORDS;
			Log.d(TAG, "actualTexcoordIndex=" + actualTexcoordIndex);
			vboBuffer.put(temp2[vtPointer.get(actualTexcoordIndex)]);
			Log.d(TAG, "actualTexcoordIndex=" + (actualTexcoordIndex+1));
			vboBuffer.put(temp2[vtPointer.get(actualTexcoordIndex+1)]);			
		}
	
	}
	
	private String readShaderCodeFromFile(String fileName) {
		String line = null;
		BufferedReader reader = null;
		StringBuilder sb = new StringBuilder();
		try {
			reader = new BufferedReader(new InputStreamReader(mgr.open(fileName)));		
		} catch (IOException e) {
			Log.e("Mesh.java->readShaderCodeFromFile()", "Unsuccessful file reading.");
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
	
	public static int loadShader(int type, String shaderCode){

	    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
	    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
	    int shader = GLES20.glCreateShader(type);

	    // add the source code to the shader and compile it
	    GLES20.glShaderSource(shader, shaderCode);
	    GLES20.glCompileShader(shader);
	    //Log.d("loadShader", "finishing");
	    return shader;
	}
	
	private void prepareShaders(String vshFile, String fshFile) {
		vertexShaderCode = readShaderCodeFromFile(vshFile);
		fragmentShaderCode = readShaderCodeFromFile(fshFile);
		
		vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
		
		mProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(mProgram, vertexShaderHandle);
		GLES20.glAttachShader(mProgram, fragmentShaderHandle);
		GLES20.glLinkProgram(mProgram);
	}
	
	private void prepareHandles() {
		a_PositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
		a_TexCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
		u_MVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
	}
	
	private void prepareAttributes() {
		
		GLES20.glBufferData(	
				GLES20.GL_ARRAY_BUFFER, 
				(COORDS_PER_VERTEX + COORDS_PER_TEXCOORDS ) * TypeSizes.BYTES_PER_FLOAT,
				vboBuffer,
				GLES20.GL_STATIC_DRAW);
		
		GLES20.glVertexAttribPointer(	
				a_PositionHandle,		
				COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, 
				false, 
				STRIDE_OF_ATTRIBS, 
				VERTEX_OFFSET);
		
		GLES20.glVertexAttribPointer(	
				a_TexCoordinateHandle,		
				COORDS_PER_TEXCOORDS,
				GLES20.GL_FLOAT, 
				false, 
				STRIDE_OF_ATTRIBS, 
				TEXCOORD_OFFSET);
		GLES20.glEnableVertexAttribArray(a_PositionHandle);
		GLES20.glEnableVertexAttribArray(a_TexCoordinateHandle);		
	}

	public void loadTextureFromAssets(GL10 gl) {
		//Get the texture from the Android resource directory
		
		InputStream is = null;
		try {
			String textureFileName = myMaterial.getTextureFile();
			is = mgr.open(textureFileName);
			Log.e(TAG, "Loading texture file: " + textureFileName);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//InputStream is = context.getResources().openRawResource(R.drawable.face_texture);
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
		gl.glGenTextures(1, textures, 0);
		//...and bind it to our array
		gl.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
		
		//Create Nearest Filtered Texture
		gl.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		//Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
		gl.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
		gl.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
		
		//Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		
		//Clean up
		bitmap.recycle();
	}

	public void draw(GL10 gl, float[] mMVP) {
		GLES20.glUseProgram(mProgram);
		/* Itt a nagy magic. A híres neves "Interleaved VBO" technika. */
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
		GLES20.glUniform4fv(u_MVPMatrixHandle, 1, mMVP, 0);
		/*GLES20.glGenBuffers(indicesHandle, indexBuffer);
		Log.d(TAG, "genBuffer ELEMENTS");
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesHandle);
		Log.d(TAG, "indices[] -> GL_ELEMENT_ARRAY");

		GLES20.glDrawElements(GLES20.GL_TRIANGLES, faces.size(), GLES20.GL_INT, indexBuffer); 
		Log.d(TAG, "DrawElements...");
	    */
	    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, faces.size()/3); //???
	}
	
	public String toString(){
		String str=new String();
		//str+="Number of parts: "+parts.size();
		str+="\nNumber of vertexes: "+v.size();
		//str+="\nNumber of vns: "+vn.size();
		str+="\nNumber of vts: "+vt.size();
		/*str+="\n/////////////////////////\n";
		for(int i=0; i<parts.size(); i++){
			str+="Part "+i+'\n';
			str+=parts.get(i).toString();
			str+="\n/////////////////////////";
		}*/
		return str;
	}
}
