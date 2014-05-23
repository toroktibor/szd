package hu.unideb.ik.mpeg4head;

import hu.unideb.ik.mpeg4head.TDModelPart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;


import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.util.Log;

public class Mesh {

	/* Number of coordinates per vertex. */
	private static final int COORDS_PER_VERTEX = 3;
	private static final int COORDS_PER_TEXCOORDS = 2;
	private static final int BYTES_PER_FLOAT = 4;
	
	/* Helpers to get the files from assets containing the source code of the shaders. */
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
	private int a_TexCoordinateHandle;
	private int u_MVPMatrixHandle;
	private int u_TextureSamplerHandle;
	
	Vector<Float> v;
	Vector<Float> vn;
	Vector<Float> vt;
	Vector<TDModelPart> parts;
	FloatBuffer vertexBuffer;
	FloatBuffer texcoordsBuffer;

	private String readShaderCodeFromFile(String fileName) {
		String line = null;
		BufferedReader reader = null;
		StringBuilder sb = new StringBuilder();
		try {
			reader = new BufferedReader(new InputStreamReader(mgr.open(fileName)));		
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
			Vector<TDModelPart> parts) {
		super();
		context = ctx;	
		mgr = context.getAssets();
		this.v = v;
		this.vn = vn;
		this.vt = vt;
		this.parts = parts;
		
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
		
				
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, a_PositionHandle);
	    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, v.size());
	}
	
	public void buildBuffers(){
		ByteBuffer vBuf = ByteBuffer.allocateDirect(v.size() * BYTES_PER_FLOAT);
		vBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = vBuf.asFloatBuffer();
		vertexBuffer.put(toPrimitiveArrayF(v));
		vertexBuffer.position(0);
		
		ByteBuffer tcBuf = ByteBuffer.allocateDirect(vt.size() * BYTES_PER_FLOAT);
		tcBuf.order(ByteOrder.nativeOrder());
		texcoordsBuffer = tcBuf.asFloatBuffer();
		texcoordsBuffer.put(toPrimitiveArrayF(vt));
		texcoordsBuffer.position(0);
	}

	private static float[] toPrimitiveArrayF(Vector<Float> vector){
		float[] f;
		f=new float[vector.size()];
		for (int i=0; i<vector.size(); i++){
			f[i]=vector.get(i);
		}
		return f;
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
