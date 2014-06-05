/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.unideb.ik.mpeg4head;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.example.android.opengl.R;

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
public class TriangleShaderLoaded {

    private String vertexShaderCode;
    private String fragmentShaderCode;
    
    private int vertexShaderHandle;
    private int fragmentShaderHandle;
    
    private int programHandle;

    private FloatBuffer vertexBuffer;
 
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    private int textureHandle;
    
    private int[] textures = new int[1];
    
    private Context ctx;
    private AssetManager assetMgr;


    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float triangleCoords[] = {
            // in counterclockwise order:
            0.0f,  0.622008459f, 0.0f,   // top
           -0.5f, -0.311004243f, 0.0f,   // bottom left
            0.5f, -0.311004243f, 0.0f    // bottom right
    };
    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * TypeSizes.BYTES_PER_FLOAT; // 4 bytes per vertex

    float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 0.0f };

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */   
    public TriangleShaderLoaded(Context context) {
    	ctx = context;
    	assetMgr = ctx.getAssets();

    	buildBuffer();
    	prepareShadersAndProgram("triangleVertexShader.glsl", "triangleFragmentShader.glsl");
        prepareHandles();
    }
    
    

	private void buildBuffer() {
    	ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length * TypeSizes.BYTES_PER_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(triangleCoords);
        vertexBuffer.position(0);
    }
    
	private String readShaderCodeFromFile(String fileName) {
		String line = null;
		BufferedReader reader = null;
		StringBuilder sb = new StringBuilder();
		try {
			reader = new BufferedReader(new InputStreamReader(
					assetMgr.open(fileName)));
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
    
    private void prepareShadersAndProgram(String vshFile, String fshFile) {
		vertexShaderCode = readShaderCodeFromFile(vshFile);
		fragmentShaderCode = readShaderCodeFromFile(fshFile);

		vertexShaderHandle = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		fragmentShaderHandle = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
		programHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(programHandle, vertexShaderHandle);
        GLES20.glAttachShader(programHandle, fragmentShaderHandle);
        GLES20.glLinkProgram(programHandle);
    }
    
    private void prepareHandles() {
    	 mPositionHandle = GLES20.glGetAttribLocation(programHandle, "vPosition");
    	 mColorHandle = GLES20.glGetUniformLocation(programHandle, "vColor");
         mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
    }
    
    private void prepareAttributes() {

        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);        
    	GLES20.glEnableVertexAttribArray(mPositionHandle);
    }
    
    float[] tester = new float[4];
    float[] testresult = new float[4];   
    private void logDrawnVertexCoordinates(float[] mvpMatrix) {
		for(int i = 0; i < 3; ++i)
				tester[i] = triangleCoords[i];
		tester[3] = 1.0f;
		Matrix.multiplyMV(testresult, 0, mvpMatrix, 0, tester, 0);
		Log.e("Triangle.draw() testvalues:", testresult[0] + "   " + testresult[1] + "   " +testresult[2] + "   " + testresult[3]);
	}
    
    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(programHandle);
        prepareAttributes();
       
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        MyGLRenderer.checkGlError("glGetUniformLocation");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");
         
        
        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

}

