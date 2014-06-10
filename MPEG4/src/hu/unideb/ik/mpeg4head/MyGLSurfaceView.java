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

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
public class MyGLSurfaceView extends GLSurfaceView {

    public final MyGLRenderer mRenderer;
    private static final String TAG = "MyGLRSurfaceView";
    
    public MyGLRenderer getRenderer() {
    	return mRenderer;
    }

    public MyGLSurfaceView(Context context) {
    	super(context);
    	Log.d(TAG, "constructor begin");
        
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
        Log.d(TAG, "setEGLContextClientVersion(2)");
       
        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new MyGLRenderer(context);
        Log.d(TAG, "new MyGLRenderer(context)");

        setRenderer(mRenderer);
        //Log.d(TAG, "setRenderer(mRenderer);");
        //Log.d(TAG, "constructor finish");
        // Render the view only when there is a change in the drawing data
        //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
  
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
            	Log.d("ScaleAmount", "" + x);
                mRenderer.scaleAmount= x / 420.0f;
                break;
            case MotionEvent.ACTION_DOWN:
            	if(( y < getHeight() / 3) && (x < getWidth() / 2 )){
                	//mRenderer.eyeX -= 1.0f;
                	//Log.d(TAG, "1. Eye moved to left: " + mRenderer.eyeX);
            		mRenderer.transX -= 2.0f;
            		Log.d(TAG, "1. TransX: " + mRenderer.transX);
                }
                else if(( y < getHeight() / 3) && (x > getWidth() / 2 )){
                	//mRenderer.eyeX += 1.0f;
                	//Log.d(TAG, "2. Eye moved to right: " +  mRenderer.eyeX);
                	mRenderer.transX += 2.0f;
                	Log.d(TAG, "2. TransX: " + mRenderer.transX);
                }
                else if(( y < getHeight() / 3 *2) && (x < getWidth() / 2 )){
                	//mRenderer.eyeY -= 1.0f;
                	//Log.d(TAG, "3. Eye moved up:" + mRenderer.eyeY);
                	mRenderer.transY -= 2.0f;
                	Log.d(TAG, "3. TransY: " + mRenderer.transY);
                }
                else if(( y < getHeight() / 3 * 2) && (x > getWidth() / 2 )){
                	//mRenderer.eyeY += 1.0f;
                	//Log.d(TAG, "4. Eye moved down:" + mRenderer.eyeY);
                	mRenderer.transY += 2.0f;
                	Log.d(TAG, "4. TransY: " + mRenderer.transY);
                }
                else if(x < getWidth() / 2 ){
                	//mRenderer.eyeZ -= 1.0f;
                	//Log.d(TAG, "5.  Eye zoomed out:" + mRenderer.eyeZ);	
                	mRenderer.transZ -= 2.0f;
                	Log.d(TAG, "5. TransZ: " + mRenderer.transZ);
                }
                else if(x > getWidth() / 2 ){
                	//mRenderer.eyeZ += 1.0f;
                	//Log.d(TAG, "6. Eye zoomed in:" + mRenderer.eyeZ);
                	mRenderer.transZ += 2.0f;
                	Log.d(TAG, "6. TransZ: " + mRenderer.transZ);
                	
                }/*
            	if(y < getHeight() / 2) {
            		mRenderer.scaleAmount += 0.5;
            	}
            	else if(y > getHeight() / 2) {
            		mRenderer.scaleAmount -= 0.5;
            	}*/
            	requestRender();
            	break;
        }

    	//Log.d(TAG, "onTouchEvent finish");
        return true;
    }

}
