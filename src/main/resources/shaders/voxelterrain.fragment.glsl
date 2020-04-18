#ifdef GL_ES
precision mediump float;
#endif


// Need camera pos for level of detail
// uniform vec3 u_cameraWorldPosition;

// Near and far level of detail fadeout start and end distances (manhattan distances from camera?)
//uniform vec4 u_lodFadeDistances;


// World-position of this fragment
varying vec3 v_worldPosition;
varying vec3 v_normal;
uniform vec4 u_color;

float worldMap(float p, float scale) {
    return 1.0 / (1.0 + abs(mod(p, scale)) / scale);
}

void main() {
//    vec3 normal = v_normal;

    // TODO: Use lights to light scene

    vec4 diffuse = vec4(0.3, 0.8, 0.1, 1.0);

    // DEBUG: Color based on world pos
    float scale = 100.;
    float fadeX = worldMap(v_worldPosition.x, scale);
    float fadeY = worldMap(v_worldPosition.y, scale);
    float fadeZ = worldMap(v_worldPosition.z, scale);
    vec3 positionColor = vec3(fadeX, fadeY, fadeZ);

    gl_FragColor.rgb = mix(u_color.rgb, v_normal.xzy * 0.5 + vec3(0.5), 0.5);
    gl_FragColor.a = 1.0;
}

