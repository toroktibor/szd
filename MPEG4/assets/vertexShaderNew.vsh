attribute vec4 a_Position;
attribute vec2 a_TexCoordinate;

uniform mat4 u_MVPMatrix;

varying vec2 v_TexCoordinate;

void main() {	 			   
	v_TexCoordinate = a_TexCoordinate;
   	gl_Position = u_MVPMatrix * a_Position; 			                                            			 
}  