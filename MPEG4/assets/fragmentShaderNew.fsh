precision mediump float;       		// Set the default precision to medium. We don't need as high of a 
									// precision in the fragment shader.

uniform sampler2D u_TextureSampler;   
varying vec2 v_TexCoordinate;   	// triangle per fragment.
			  
void main()                    		// The entry point for our fragment shader.
{                              
	gl_FragColor = vec4(1.0, 1.0, 1.0, 0.0);
	//gl_FragColor = texture2D(u_TextureSampler, v_TexCoordinate);		  
}         