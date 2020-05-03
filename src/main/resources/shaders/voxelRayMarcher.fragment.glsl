#version 120

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

// Range of x and y is -1 .. 1 (one screen edge to other) (z is 0).
varying vec2 v_position;

void main() {
    // TODO: Shoot off ray in the direction of this pixel.  Use u_projViewTrans to determine direction

    // TODO: Get signed depth data from cascading 3D textures

    // TODO: Need uniform telling word center position & size & size scale per level & num levels  for the depth data

    gl_FragColor = vec4(
        v_position.x * 0.5 + 0.5,
        v_position.y * 0.5 + 0.5,
        0.0,
        1.0);
}
