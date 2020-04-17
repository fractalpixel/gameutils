#ifdef GL_ES
precision mediump float;
#endif



// World-position of this fragment
uniform vec4 u_color;

void main() {

    gl_FragColor = u_color;
}

