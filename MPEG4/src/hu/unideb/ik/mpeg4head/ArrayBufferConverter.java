package hu.unideb.ik.mpeg4head;

import java.util.Vector;

public class ArrayBufferConverter {

	public static final float[] getFloatArrayFromFloatVector(Vector<Float> vector){
		float[] f;
		f=new float[vector.size()];
		for (int i=0; i<vector.size(); i++){
			f[i]=vector.get(i);
		}
		return f;
	}
	
	public static final int[] getIntArrayFromIntVector(Vector<Integer> vector){
		int[] values;
		values = new int[vector.size()];
		for (int i=0; i<vector.size(); i++){
			values[i]=vector.get(i);
		}
		return values;
	}
	
	public static final short[] getShortArrayFromShortVector(Vector<Short> vector){
		short[] values;
		values = new short[vector.size()];
		for (int i=0; i<vector.size(); i++){
			values[i]=vector.get(i);
		}
		return values;
	}
	
	
}
