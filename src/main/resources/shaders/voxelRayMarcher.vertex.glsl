#version 120

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

attribute vec3 a_position;

varying vec2 v_position;

void main() {
    v_position = a_position.xy;

    gl_Position = vec4(a_position, 1.0);
}
