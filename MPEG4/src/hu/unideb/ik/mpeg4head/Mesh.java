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
	Vector<TDModelPart> parts;
	FloatBuffer vertexBuffer;
	FloatBuffer texcoordsBuffer;
	IntBuffer indexBuffer;
	
	private int[] textures = new int[1];
	private int indicesHandle;

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
		Log.d("readShaderCodeFromFile", sb.toString());
		return sb.toString();
	}

	public static int loadShader(int type, String shaderCode){

	    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
	    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
	    int shader = GLES20.glCreateShader(type);

	    // add the source code to the shader and compile it
	    GLES20.glShaderSource(shader, shaderCode);
	    GLES20.glCompileShader(shader);
	    Log.d("loadShader", "finishing");
	    return shader;
	}
	
	private void getAllHandles() {
		a_PositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
		a_TexCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
		u_MVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
		GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
	}
	
	public Mesh(Context ctx, Vector<Float> v, Vector<Float> vn, Vector<Float> vt,
			Vector<TDModelPart> parts, Vector<Integer> faces, Material mat) {
		super();
		context = ctx;	
		mgr = context.getAssets();
		this.v = v;
		this.vn = vn;
		this.vt = vt;
		this.faces = faces;
		this.parts = parts;
		this.myMaterial = mat;
		
		vertexShaderCode = readShaderCodeFromFile("vertexShaderNew.vsh");
		fragmentShaderCode = readShaderCodeFromFile("fragmentShaderNew.fsh");
		
		vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
		
		mProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(mProgram, vertexShaderHandle);
		GLES20.glAttachShader(mProgram, fragmentShaderHandle);
		GLES20.glLinkProgram(mProgram);
		getAllHandles();
		
	}
	
	
	public void draw(GL10 gl, float[] mMVP) {
		GLES20.glUseProgram(mProgram);
		
		GLES20.glEnableVertexAttribArray(a_PositionHandle);
	    GLES20.glVertexAttribPointer(a_PositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, vertexBuffer);
	    
	    GLES20.glEnableVertexAttribArray(a_TexCoordinateHandle);
	    GLES20.glVertexAttribPointer(a_TexCoordinateHandle, COORDS_PER_TEXCOORDS, GLES20.GL_FLOAT, false, 0, texcoordsBuffer);
	    
		GLES20.glUniformMatrix4fv(u_MVPMatrixHandle, 1, false, mMVP, 0);
		
		/* TEXTÚRA BETÖLTÉSE ÉS ÁDATÁSA A UNIFORM-NAK!!!!!!! */ 
		/* NE FELEJTSEM EL ÁTÍRNI A FRAGMENT SHADERT KONSTANS SZÍNRŐL TEXTÚRA KEZELÉSÉRE!!!!!!!!!!!!!!!! */
		/*loadTextureFromAssets(gl);
		// Set the active texture unit to texture unit 0.
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		// Bind the texture to this unit.
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

		// Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
		GLES20.glUniform1i(u_TextureSamplerHandle, 0); 
		*/
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, a_PositionHandle);
		GLES20.glGenBuffers(indicesHandle, indexBuffer);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesHandle);

		GLES20.glDrawElements(GLES20.GL_TRIANGLES, faces.size(), GLES20.GL_INT, indexBuffer); 
	    //GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, v.size());
	}
	
	public void loadTextureFromAssets(GL10 gl) {
		//Get the texture from the Android resource directory
		
		InputStream is = null;
		try {
			is = mgr.open(myMaterial.getTextureFile());
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
	
	public void buildBuffers(){
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
