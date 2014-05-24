package hu.unideb.ik.mpeg4head;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class must
 * override the OpenGL ES drawing lifecycle methods:
 * <ul>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class MyGLRenderer implements GLSurfaceView.Renderer,
		SensorEventListener {

	private static final String TAG = "MyGLRenderer";

	private Mesh mesh;
	private Context context;

	private Sensor gravity;
	private SensorManager sensorMgr;

	private float[] mMVPMatrix = new float[16];
	private float[] mModelMatrix = new float[16];
	private float[] mProjectionMatrix = new float[16];
	private float[] mViewMatrix = new float[16];

	private float[] tempMatrix = new float[16];
	private float[] mTranslateMatrix = new float[16];
	private float[] mRotationMatrix = new float[16];
	private float[] mScaleMatrix = new float[16];

	private float scaleAmount = 0.8f;
	private float mAngle = 0.0f;
	private float theta;

	private static final float[] AxisX = { 1.0f, 0.0f, 0.0f };
	private static final float[] AxisY = { 0.0f, 1.0f, 0.0f };
	private static final float[] AxisZ = { 0.0f, 0.0f, 1.0f };

	/* 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f */
	public float eyeX = 0.0f;
	public float eyeY = 0.0f;
	public float eyeZ = -3.0f;

	private float ctrX = 0.0f;
	private float ctrY = 0.0f;
	private float ctrZ = 20.0f;

	private float upX = 0.0f;
	private float upY = 1.0f;
	private float upZ = 0.0f;

	public float near = 0.1f;
	public float far = 200.0f;
	private float ratio;

	public float transX = 0.0f;
	public float transY = 0.0f;
	public float transZ = -5.0f;

	private float diffGravityX;
	private float diffGravityY;
	private float diffGravityZ;

	private float originGravityX;
	private float originGravityY;
	private float originGravityZ;

	private float newGravityX;
	private float newGravityY;
	private float newGravityZ;

	private static final float epsilon = 0.45f;

	private boolean newOrientation = true;

	public MyGLRenderer(Context ctx) {
		context = ctx;
		sensorMgr = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		gravity = sensorMgr.getDefaultSensor(Sensor.TYPE_GRAVITY);
		OBJParser parser = new OBJParser(context);
		String modelname = "texture_face_final_meshlab.obj";
		// String modelname = "dragon.obj";
		mesh = parser.parseOBJ(modelname);
		Log.d(TAG, "loaded the modell '" + modelname + "'");
		// Log.e(TAG, mesh.toString());
	}

	public void makeModelMatrix() {
		Matrix.setIdentityM(mScaleMatrix, 0);
		Matrix.setIdentityM(mRotationMatrix, 0);
		Matrix.setIdentityM(mTranslateMatrix, 0);
		Matrix.setIdentityM(tempMatrix, 0);
		
		Matrix.scaleM(mScaleMatrix, 0, scaleAmount, scaleAmount, scaleAmount);
		Matrix.rotateM(mRotationMatrix, 0, theta, AxisY[0], AxisY[1], AxisY[2]);
		Matrix.translateM(mTranslateMatrix, 0, transX, transY, transZ);
		
		Matrix.multiplyMM(tempMatrix, 0, mRotationMatrix, 0, mScaleMatrix, 0);
		Matrix.multiplyMM(mModelMatrix, 0, mTranslateMatrix, 0, tempMatrix, 0);
	}
	
	public void makeModelMatrix2() {
		Matrix.setIdentityM(mScaleMatrix, 0);
		Matrix.setIdentityM(mRotationMatrix, 0);
		Matrix.setIdentityM(mTranslateMatrix, 0);
		Matrix.setIdentityM(tempMatrix, 0);
		
		Matrix.scaleM(mScaleMatrix, 0, scaleAmount, scaleAmount, scaleAmount);
		//Matrix.rotateM(mRotationMatrix, 0, theta, AxisY[0], AxisY[1], AxisY[2]);
		Matrix.translateM(mTranslateMatrix, 0, transX, transY, transZ);
		
		Matrix.multiplyMM(tempMatrix, 0, mRotationMatrix, 0, mTranslateMatrix, 0);
		Matrix.multiplyMM(mModelMatrix, 0,mScaleMatrix, 0 , tempMatrix, 0);
	}

	public void makeMVPMatrix() {
		Matrix.multiplyMM(tempMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, tempMatrix, 0);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {

		//Log.d(TAG, "onSurfaceCreated begin");
		// Set the background frame color
		
		GLES20.glEnable(GL10.GL_TEXTURE_2D);			//Enable Texture Mapping ( NEW )
		GLES20.glEnable(GL10.GL_DEPTH_TEST); 			//Enables Depth Testing
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

		eyeX = 0.0f;
		eyeY = 0.0f;
		eyeZ = -2.0f;
		ctrX = 0.0f;
		ctrY = 0.0f;
		ctrZ = 0.0f;
		upX = 0.0f;
		upY = 1.0f;
		upZ = 0.0f;
		//Log.d(TAG, "onSurfaceCreated - identify matrices, set primitive values");
		//Matrix.frustumM(mProjectionMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, 0.01f, 100.0f);
		Matrix.frustumM(mProjectionMatrix, 0, -5, 5, -5, 5, near, far);
		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, ctrX, ctrY, ctrZ,
				upX, upY, upZ);

		//Log.d(TAG, "onSurfaceCreated - makeModelMatrix, makeMVPMatrix");
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		//Log.d(TAG, "onDrawFrame begin");

		// Draw background color
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		//Log.d(TAG, "onDrawFrame - glClear");

		// Set the camera position (View matrix)
		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, ctrX, ctrY, ctrZ, upX, upY, upZ);
		//Log.d(TAG, "onDrawFrame - setLookAtM");

		// Create a rotation for the triangle
		//long time = SystemClock.uptimeMillis() % 4000L;
		//mAngle = 0.090f * ((int) time);
		//Log.d(TAG, "onDrawFrame - processed rotating angle:" + mAngle);
		//Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, 1.0f);
		//Log.d(TAG, "onDrawFrame - setRotateM");

		// Calculate the modified Model and MVP matrix
		makeModelMatrix();
		makeMVPMatrix();
		//Log.d(TAG, "makeModel, makeMVPMatrix");
		/* Draw the mesh... */
		/*for(int i = 0; i < 16; ++i) {
			Log.d("MVP["+i+"]", " " + mMVPMatrix[i] );
		}*/
		
		//Log.d("TESTVERTEXVALUES", "COM'ON! :)");
		float[] res = new float[4];
		float[] raw = { mesh.v.get(0), mesh.v.get(1), mesh.v.get(2), 0.0f};
		Matrix.multiplyMV(res, 0, mMVPMatrix, 0, raw, 0);
		Log.d(TAG, "x: " + res[0] + " y: " + res[1] + " z: " + res[2]);
		
		mesh.draw(unused, mMVPMatrix);
		//Log.d(TAG, "mesh.draw()");
		//Log.d(TAG, "onDrawFrame finish");
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		//Log.d(TAG, "onSurfaceChaned begin");
		// Adjust the viewport based on geometry changes,
		// such as screen rotation
		GLES20.glViewport(0, 0, width, height);
		//Log.d(TAG, "onSurfaceChaned - glViewport");
		float ratio = (float) width / height;

		// this projection matrix is applied to object coordinates
		// in the onDrawFrame() method
		//Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, near, far);
		Matrix.frustumM(mProjectionMatrix, 0, -20, 20, -20, 20, near, far);
		//Log.d(TAG, "onSurfaceChaned - frustrumM");
		//Log.d(TAG, "onSurfaceChaned finish");

	}

	/**
	 * Utility method for compiling a OpenGL shader.
	 * 
	 * <p>
	 * <strong>Note:</strong> When developing shaders, use the checkGlError()
	 * method to debug shader coding errors.
	 * </p>
	 * 
	 * @param type
	 *            - Vertex or fragment shader type.
	 * @param shaderCode
	 *            - String containing the shader code.
	 * @return - Returns an id for the shader.
	 */
	public static int loadShader(int type, String shaderCode) {

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
	 * mColorHandle = GLES20.glGetUniformLocation(mProgram, &quot;vColor&quot;);
	 * MyGLRenderer.checkGlError(&quot;glGetUniformLocation&quot;);
	 * </pre>
	 * 
	 * If the operation is not successful, the check throws an error.
	 * 
	 * @param glOperation
	 *            - Name of the OpenGL call to check.
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

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d(TAG, "onSensorChanged accuracy = " + accuracy);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		Log.d(TAG, "onSensorChanged begin");

		switch (event.sensor.getType()) {
		case Sensor.TYPE_GRAVITY:
			Log.d(TAG,
					"onSensorChanged - gravity sensor values coming, Baby :)");
			if (newOrientation) {
				originGravityX = event.values[0];
				originGravityY = event.values[1];
				originGravityZ = event.values[2];
				newOrientation = false;
			}

			newGravityX = event.values[0];
			newGravityY = event.values[1];
			newGravityZ = event.values[2];
			diffGravityX = originGravityX - newGravityX;
			diffGravityY = originGravityY - newGravityY;
			diffGravityZ = originGravityZ - newGravityZ;
			if ((diffGravityX > epsilon) || (diffGravityY > epsilon)
					|| (diffGravityZ > epsilon)) {
				Log.d(TAG, "onSensorChanged - some changes greater then epsilon...");
			}

			break;
		default:
			break;
		}

		Log.d(TAG, "onSensorChanged finish");
	}

}