package hu.unideb.ik.mpeg4head;

import com.example.android.opengl.R;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class OpenGLES20Activity extends Activity implements SensorEventListener, OnClickListener {

    private MyGLSurfaceView mGLView;
    private static final String TAG = "OPENGLES20Activity";
    
	private Sensor accelerometer;
	private SensorManager sensorMgr;

	private static float[] gravity = new float[3];
	
	private float diffGravityX;
	private float diffGravityY;
	private float diffGravityZ;

	private float originGravityX;
	private float originGravityY;
	private float originGravityZ;

	private float newGravityX;
	private float newGravityY;
	private float newGravityZ;
	
	private float previousGravityX;
	private float previousGravityY;
	private float previousGravityZ;
	
	private float xRotAngle;
	private float yRotAngle;
	private float zRotAngle;
	
	private static final float epsilon = 0.15f;
	private static final float alpha = 0.8f;
	private static final float GRAVITY_TO_DEGREES_HELPER =  (180.0f / (9.81f * 2.0f));

	private boolean newOrientation;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity
        mGLView = new MyGLSurfaceView(this);
        LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        View v = li.inflate(R.layout.main, null);
        Button resetButton = (Button) v.findViewById(R.id.resetButton);
        resetButton.setOnClickListener(this);
        setContentView(mGLView);
        addContentView(v, lp);
        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
		//Log.e(TAG, "Accelerometer is: " + accelerometer.getName());
		newOrientation = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The following call pauses the rendering thread.
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
        sensorMgr.unregisterListener(this);
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The following call resumes a paused rendering thread.
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
        mGLView.onResume();
        sensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
		
    }
    

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d(TAG, "onSensorChanged accuracy = " + accuracy);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		/*Log.d(TAG, "onSensorChanged begin");
*/
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:

			gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
			gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
			gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

			
			if (newOrientation) {
				originGravityX = gravity[0];
				originGravityY = gravity[1];
				originGravityZ = gravity[2];
				newOrientation = false;
			}

			newGravityX = gravity[0];
			newGravityY = gravity[1];
			newGravityZ = gravity[2];
			diffGravityX = newGravityX - previousGravityX;
			diffGravityY = newGravityY - previousGravityY;
			diffGravityZ = newGravityZ - previousGravityZ;
			if ((Math.abs(diffGravityX) > epsilon) || (Math.abs(diffGravityY) > epsilon)
					|| (Math.abs(diffGravityZ) > epsilon)) {

				Log.d("Differences:", diffGravityX + " " + diffGravityY + " " + diffGravityZ);

				xRotAngle = - GRAVITY_TO_DEGREES_HELPER * (originGravityZ -newGravityZ);
				mGLView.getRenderer().setxRotAngle(xRotAngle);
				
				zRotAngle = GRAVITY_TO_DEGREES_HELPER * (newGravityX - originGravityX);
				mGLView.getRenderer().setzRotAngle(zRotAngle);

			}
			previousGravityX = newGravityX;
			previousGravityY = newGravityY;
			previousGravityZ = newGravityZ;

			break;
		default:
			break;
		}
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.resetButton) {
			newOrientation = true;
			mGLView.requestRender();
		}
	}
}