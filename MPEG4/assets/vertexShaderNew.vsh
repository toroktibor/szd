uniform mat4 u_MVPMatrix;      		// A constant representing the combined model/view/projection matrix.
			
attribute vec4 a_Position;     		// Per-vertex position information we will pass in. 
attribute vec2 a_TexCoordinate;

varying vec2 v_TexCoordinate;

void main()                    		// The entry point for our vertex shader.
{
	 			   
	v_TexCoordinate = a_TexCoordinate;	// It will be interpolated across the triangle.
   	gl_Position = u_MVPMatrix    		// gl_Position is a special variable used to store the final position.
               * a_Position;        	// Multiply the vertex by the matrix to get the final point in 			                                            			 
}  