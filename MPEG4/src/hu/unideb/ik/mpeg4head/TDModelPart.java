package hu.unideb.ik.mpeg4head;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Vector;

public class TDModelPart {
	
	private static final int COORDS_PER_VERTEX = 3;

	private Vector<Integer> faces;
	private Vector<Short> vtPointer;
	private Vector<Short> vnPointer;
	private Material material;
	private FloatBuffer normalBuffer;
	private IntBuffer faceBuffer;
	
	public TDModelPart(Vector<Integer> faces, Vector<Short> vtPointer,
			Vector<Short> vnPointer, Material material, Vector<Float> vn) {
		super();
		this.faces = faces;
		this.vtPointer = vtPointer;
		this.vnPointer = vnPointer;
		this.material = material;
		
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(vnPointer.size() * TypeSizes.BYTES_PER_FLOAT * COORDS_PER_VERTEX);
		byteBuf.order(ByteOrder.nativeOrder());
		normalBuffer = byteBuf.asFloatBuffer();
		for(int i=0; i<vnPointer.size(); i++){
			float x=vn.get(vnPointer.get(i)*3);
			float y=vn.get(vnPointer.get(i)*3+1);
			float z=vn.get(vnPointer.get(i)*3+2);
			normalBuffer.put(x);
			normalBuffer.put(y);
			normalBuffer.put(z);
		}
		normalBuffer.position(0);
		

		ByteBuffer fBuf = ByteBuffer.allocateDirect(faces.size() * TypeSizes.BYTES_PER_INTEGER);
		fBuf.order(ByteOrder.nativeOrder());
		faceBuffer = fBuf.asIntBuffer();
		faceBuffer.put(ArrayBufferConverter.getIntArrayFromIntVector(faces));
		faceBuffer.position(0);
	}
	public String toString(){
		String str=new String();
		if(material!=null)
			str+="Material name:"+material.getName();
		else
			str+="Material not defined!";
		str+="\nNumber of faces:"+faces.size();
		str+="\nNumber of vnPointers:"+vnPointer.size();
		str+="\nNumber of vtPointers:"+vtPointer.size();
		return str;
	}
	public IntBuffer getFaceBuffer(){
		return faceBuffer;
	}
	public FloatBuffer getNormalBuffer(){
		return normalBuffer;
	}	
	public int getFacesCount(){
		return faces.size();
	}
	public Material getMaterial(){
		return material;
	}
	
	
}
