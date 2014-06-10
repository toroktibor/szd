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

public class MyGLRenderer implements GLSurfaceView.Renderer /*,
		SensorEventListener*/ {

	private static final String TAG = "MyGLRenderer";

	private Context context;
	private String modelname;
	private OBJParser objParser;

	//private Mesh mesh;
	//private Triangle t;
	//private TriangleShaderLoaded tX1;
	//private TriangleTextured t2;
	private MeshOnlyColored meshColored;

	private float[] mMVPMatrix = new float[16];
	private float[] mModelMatrix = new float[16];
	private float[] mProjectionMatrix = new float[16];
	private float[] mViewMatrix = new float[16];

	private float[] tempMatrix = new float[16];
	private float[] mTranslateMatrix = new float[16];
	private float[] mRotationMatrix = new float[16];
	private float[] mScaleMatrix = new float[16];
	
	/* A Triangle háromszögnél 5 volt az ideális scaleAmount!!!
	 * itt úgy néz ki, hogy itt 10 körül... */
	public float scaleAmount = 18.0f;
	
	private float xRotAngle = 0.0f;
	private float yRotAngle = 0.0f;
	private float zRotAngle = 0.0f;
	
	private static final float[] AxisX = { 1.0f, 0.0f, 0.0f };
	private static final float[] AxisY = { 0.0f, 1.0f, 0.0f };
	private static final float[] AxisZ = { 0.0f, 0.0f, 1.0f };

	public float eyeX = 0.0f;
	public float eyeY = 0.0f;
	/* TODO az eyeZ eredetileg -5 volt!!! */
	public float eyeZ = -5.0f;

	private float ctrX = 0.0f;
	private float ctrY = 0.0f;
	private float ctrZ = 0.0f;

	private float upX = AxisY[0];
	private float upY = AxisY[1];
	private float upZ = AxisY[2];

	public float near = 1.0f;
	public float far = 1000.0f;
	private float ratio;

	public float transX = 0.0f;
	public float transY = 0.0f;
	public float transZ = 0.0f;


	public MyGLRenderer(Context ctx) {
		context = ctx;
		modelname = "texture_face_final_meshlab.obj";
		//modelname = "dragon.obj";
	}

	public static void logMatrix(String logtag, float[] m) {
		Log.e("MyGLRenderer", "logMatrix()");
		int j;
		for(int i = 0; i < 4; ++i) {
			j = i * 4;
			Log.d(logtag + "[" + (j) + "-" + (j+3) + "]", m[j] + " "+ m[j+1] + " " + m[j+2] + " " +m[j+3]);
		}
	}
	
	public void makeModelMatrix() {
		Matrix.setIdentityM(mScaleMatrix, 0);

		Matrix.setIdentityM(mRotationMatrix, 0);
		Matrix.setIdentityM(mTranslateMatrix, 0);
		Matrix.setIdentityM(tempMatrix, 0);
		
		Matrix.scaleM(mScaleMatrix, 0, scaleAmount, scaleAmount, scaleAmount);
		//logMatrix("mScaleMatrix", mScaleMatrix);
		
		Matrix.rotateM(mRotationMatrix, 0, yRotAngle, AxisY[0], AxisY[1], AxisY[2]);
		Matrix.rotateM(tempMatrix, 0, mRotationMatrix, 0, xRotAngle, AxisX[0], AxisX[1], AxisX[2]);
		Matrix.rotateM(mRotationMatrix, 0, tempMatrix, 0, zRotAngle, AxisZ[0], AxisZ[1], AxisZ[2]);
	//logMatrix("mRotationMatrix", mRotationMatrix);
		//Log.e("TRANS", transX + " " + transY + " " + transZ);
		Matrix.translateM(mTranslateMatrix, 0, transX, transY, transZ);
		//logMatrix("mTranslateMatrix", mTranslateMatrix);
		Matrix.multiplyMM(tempMatrix, 0, mRotationMatrix, 0, mScaleMatrix, 0);
		Matrix.multiplyMM(mModelMatrix, 0, mTranslateMatrix, 0, tempMatrix, 0);
		//logMatrix("mModelMatrix", mModelMatrix);
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
		//logMatrix("mMVPMatrix", mMVPMatrix);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {

		//GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glClearColor(0.5f, 0.7f, 0.9f, 1.0f);

		Matrix.setIdentityM(mRotationMatrix, 0);
		Matrix.setIdentityM(mTranslateMatrix, 0);
		Matrix.setIdentityM(mScaleMatrix, 0);
		Matrix.setIdentityM(tempMatrix, 0);
		
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.setIdentityM(mViewMatrix, 0);
		Matrix.setIdentityM(mProjectionMatrix, 0);
		Matrix.setIdentityM(mMVPMatrix, 0);

		/**FONTOS! ITT KELL A RAJZOLANDÓ OBJEKTUMOKAT PÉLDÁNYSÍTANI
		 * ÉS BEOLVASNI A MESH ADATOKAT, AZ OPENGL ES RAJZOLÓ SZÁLON!!! */
		//t = new Triangle();
		//tX1 = new TriangleShaderLoaded(context);
		//t2 = new TriangleTextured(context);
		objParser = new OBJParser(context);
		//mesh = objParser.parseOBJ(modelname);
		meshColored = objParser.parseOBJToColoredMesh(modelname);
		
	}
	

	@Override
	public void onDrawFrame(GL10 unused) {

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT );

		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, ctrX, ctrY, ctrZ, upX, upY, upZ);

		//long time = SystemClock.uptimeMillis() % 4000L;
		//mAngle = 0.090f * ((int) time);
		//Log.d(TAG, "onDrawFrame - processed rotating angle:" + mAngle);
		//Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, 1.0f);
		//Log.d(TAG, "onDrawFrame - setRotateM");

		makeModelMatrix();
		makeMVPMatrix();
		
		/** Mesh vertex adatok kiíratása teszként: */
		/*float[] res = new float[4];
		float[] raw = { mesh.v.get(0), mesh.v.get(1), mesh.v.get(2), 0.0f};
		Matrix.multiplyMV(res, 0, mMVPMatrix, 0, raw, 0);
		Log.d(TAG, "x: " + res[0] + " y: " + res[1] + " z: " + res[2]);
		*/
		
		/** Triangle vertex adatok kiíratása teszként: */
		/*
		float[] res = new float[4];
		float[] raw = { Triangle.triangleCoords[0], Triangle.triangleCoords[1], Triangle.triangleCoords[2], 0.0f};
		Matrix.multiplyMV(res, 0, mMVPMatrix, 0, raw, 0);
		Log.d(TAG, "(" + res[0] + "; " + res[1] + "; " + res[2] + "; " + res[3] + ")");
		*/
		
		//t.draw(mMVPMatrix);
		//tX1.draw(mMVPMatrix);
		//t2.draw(mMVPMatrix);
		//mesh.draw(mMVPMatrix);
		meshColored.draw(mMVPMatrix);
		//Log.d(TAG, "onDrawFrame finish");
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		//Log.d(TAG, "onSurfaceChaned begin");
		GLES20.glViewport(0, 0, width, height);
		
		//Log.d(TAG, "onSurfaceChaned - glViewport");
		ratio = (float) width / height;

		Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, near, far);
		//Log.d(TAG, "onSurfaceChaned finish");
	}

	public static int loadShader(int type, String shaderCode) {

		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, shaderCode);
		GLES20.glCompileShader(shader);
		return shader;
	}

	public static void checkGlError(String glOperation) {
		int error;
		GLES20.glGetError();
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, glOperation + ": glError " + error);
			throw new RuntimeException(glOperation + ": glError " + error);
		}
	}

	public float getxRotAngle() {
		return xRotAngle;
	}

	public void setxRotAngle(float xRotAngle) {
		this.xRotAngle = xRotAngle;
	}

	public float getyRotAngle() {
		return yRotAngle;
	}

	public void setyRotAngle(float yRotAngle) {
		this.yRotAngle = yRotAngle;
	}

	public float getzRotAngle() {
		return zRotAngle;
	}

	public void setzRotAngle(float zRotAngle) {
		this.zRotAngle = zRotAngle;
	}


	
}