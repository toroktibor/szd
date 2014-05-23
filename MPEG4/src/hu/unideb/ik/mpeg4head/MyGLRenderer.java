package hu.unideb.ik.mpeg4head;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "MyGLRenderer";

    private Mesh mesh;
    private Context context;

    private float[] mMVPMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];

    private float[] tempMatrix = new float[16];
    private float[] mTranslateMatrix = new float[16];
    private float[] mRotationMatrix = new float[16];
    private float[] mScaleMatrix = new float[16];
    
    private float scaleAmount = 2.0f;
    private float mAngle;
    private float theta;
    
    private static final float[] AxisX = { 1.0f, 0.0f, 0.0f };
    private static final float[] AxisY = { 0.0f, 1.0f, 0.0f };
    private static final float[] AxisZ = { 0.0f, 0.0f, 1.0f };
    
    /* 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f */
    private float eyeX= 0.0f;
    private float eyeY= 0.0f;
    private float eyeZ= -5.0f;
    
    private float ctrX= 0.0f;
    private float ctrY= 0.0f;
    private float ctrZ= 0.0f;
    
    private float upX = 0.0f;
    private float upY = 1.0f;
    private float upZ = 0.0f;

    public float near = 2.0f; 
    public float far = 200.0f;
    private float ratio;
    
    public float transX = 0.0f;
    public float transY = 0.0f;
    public float transZ = -0.0f;
    
	public MyGLRenderer(Context ctx) {
    	context = ctx;
    }
	
    public void makeModelMatrix() {
        Matrix.multiplyMM(tempMatrix, 0, mRotationMatrix, 0, mScaleMatrix, 0);
        Matrix.multiplyMM(mModelMatrix, 0, mTranslateMatrix	, 0, tempMatrix, 0);
    }
    
    public void makeMVPMatrix() {
    	Matrix.multiplyMM(tempMatrix, 0, mViewMatrix, 0, mProjectionMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mModelMatrix, 0, tempMatrix, 0);
    }
    
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

    	Log.d(TAG, "onSurfaceCreated begin");
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        theta = 0.0f;
        scaleAmount = 1.0f;
    	    
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.setIdentityM(mProjectionMatrix, 0);
        Matrix.setIdentityM(mViewMatrix, 0);
        
        Matrix.setIdentityM(mRotationMatrix, 0);
        
        Matrix.setIdentityM(mTranslateMatrix, 0);
        Matrix.setIdentityM(mScaleMatrix, 0);
        Matrix.setIdentityM(tempMatrix, 0);
        
        eyeX = 0.0f;	eyeY = 0.0f;	eyeZ = 1.0f;
        ctrX = 0.0f;	ctrY = 0.0f;	ctrZ = 0.0f;
        upX = 0.0f;		upY = 1.0f;     upZ = 0.0f;
        Log.d(TAG, "onSurfaceCreated - identify matrices, set primitive values");       
        Matrix.frustumM(mProjectionMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, 0.01f, 100.0f);
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, ctrX, ctrY, ctrZ, upX, upY, upZ);
        Matrix.scaleM(mScaleMatrix, 0, scaleAmount, scaleAmount, scaleAmount);
     	Log.d(TAG, "onSurfaceCreated - frustrum, setLookAt, scaleM matrices");    
     	
        float[] yAxis = { 0.0f, 1.0f, 0.0f };
        
        /* Now we are only rotating around Y axis..... */
        Matrix.rotateM(mRotationMatrix, 0, theta, yAxis[0], yAxis[1], yAxis[2]);
        Matrix.translateM(mTranslateMatrix, 0, transX, transY, transZ);
        Log.d(TAG, "onSurfaceCreated - rotateM, translate matrices");       
        
        makeModelMatrix();
        Log.d(TAG, "onSurfaceCreated - makeModelMatrix");
        makeMVPMatrix();
        Log.d(TAG, "onSurfaceCreated - makeMVPMatrix");  
        
        OBJParser parser = new OBJParser(context);
        String modelname = "texture_face_final_meshlab.obj";
        //String modelname = "dragon.obj";
        mesh = parser.parseOBJ(modelname);
        Log.d(TAG, "loaded the modell '" + modelname + "'");
        Log.e(TAG, mesh.toString());
    }

    @Override
    public void onDrawFrame(GL10 unused) {
    	 Log.d(TAG, "onDrawFrame begin");
    	 
        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
   	 	Log.d(TAG, "onDrawFrame - glClear");
   	 	
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
   	 	Log.d(TAG, "onDrawFrame - setLookAtM");
   	 	
        // Create a rotation for the triangle
        long time = SystemClock.uptimeMillis() % 4000L;
        mAngle = 0.090f * ((int) time);
        Log.d(TAG, "onDrawFrame - processed rotating angle");
        Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, 1.0f);
        Log.d(TAG, "onDrawFrame - setRotateM");

        
        // Calculate the modified Model and MVP matrix
        makeModelMatrix();
        makeMVPMatrix();
   	 	Log.d(TAG, "makeModel, makeMVPMatrix"); 
        /* Draw the mesh... */
        mesh.draw(unused, mMVPMatrix);
        Log.d(TAG, "mesh.draw()");
   	 	Log.d(TAG, "onDrawFrame finish");
    }
    
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
    	Log.d(TAG, "onSurfaceChaned begin");
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);
        Log.d(TAG, "onSurfaceChaned - glViewport");
        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    	Log.d(TAG, "onSurfaceChaned - frustrumM");
    	Log.d(TAG, "onSurfaceChaned finish");

    }

    /**
     * Utility method for compiling a OpenGL shader.
     *
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
    * Utility method for debugging OpenGL calls. Provide the name of the call
    * just after making it:
    *
    * <pre>
    * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
    * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
    *
    * If the operation is not successful, the check throws an error.
    *
    * @param glOperation - Name of the OpenGL call to check.
    */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    /**
     * Returns the rotation angle of the triangle shape (mTriangle).
     *
     * @return - A float representing the rotation angle.
     */
    public float getAngle() {
        return mAngle;
    }

    /**
     * Sets the rotation angle of the triangle shape (mTriangle).
     */
    public void setAngle(float angle) {
        mAngle = angle;
    }

}