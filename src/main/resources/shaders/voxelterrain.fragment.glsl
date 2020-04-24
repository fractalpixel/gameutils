/*
#version 420 core
out vec4 FragColor;
layout (depth_greater) out float gl_FragDepth;
*/
#ifdef GL_ES
precision mediump float;
#endif

// Need camera pos for level of detail
uniform vec3 u_cameraWorldPosition;

// Near and far level of detail fadeout start and end distances (manhattan distances from camera?)
uniform vec4 u_lodFadeDistances;


// World-position of this fragment
varying vec3 v_worldPosition;
varying vec3 v_normal;
uniform vec4 u_color;

// Maps value from source range to target range.  Target range should not be empty.
float map(float value, float sourceStart, float sourceEnd, float targetStart, float targetEnd) {
    return targetStart + (targetEnd - targetStart) * (value - sourceStart) / (sourceEnd - sourceStart);
}

// Maps value from source range to target range, clamping to target range.  Target range should not be empty.
float mapClamp(float value, float sourceStart, float sourceEnd, float targetStart, float targetEnd) {
    return targetStart + (targetEnd - targetStart) * clamp((value - sourceStart) / (sourceEnd - sourceStart), 0., 1.);
}

float worldMap(float p, float scale) {
    return 1.0 / (1.0 + abs(mod(p, scale)) / scale);
}

void main() {
//    vec3 normal = v_normal;

    // TODO: Use lights to light scene

    vec4 diffuse = vec4(0.3, 0.8, 0.1, 1.0);

    // Get fade distances
    float fadeInStart = u_lodFadeDistances.x;
    float fadeInEnd = u_lodFadeDistances.y;
    float fadeOutStart = u_lodFadeDistances.z;
    float fadeOutEnd = u_lodFadeDistances.w;

    // DEBUG: Color based on world pos
    float scale = 100.;
    float fadeX = worldMap(v_worldPosition.x, scale);
    float fadeY = worldMap(v_worldPosition.y, scale);
    float fadeZ = worldMap(v_worldPosition.z, scale);
    vec3 positionColor = vec3(fadeX, fadeY, fadeZ);

    vec3 toCamera = vec3(v_worldPosition) - u_cameraWorldPosition;
    float distance = max(max(abs(toCamera.x), abs(toCamera.y)), abs(toCamera.z));
    float distColor = mod((1.0 - distance / (distance + 500.0)) * 10.,  1.0);

    float fadeIn = mapClamp(distance, fadeInStart, fadeInEnd, 0., 1.);
    float fadeOut = mapClamp(distance, fadeOutStart, fadeOutEnd, 1., 0.);
    float fade = min(fadeOut, fadeIn);

    gl_FragColor.rgb = mix(u_color.rgb, v_normal.xzy * 0.5 + vec3(0.5), 0.5);
    /*
    gl_FragColor.r = 1.0;
    gl_FragColor.g = fadeIn;
    gl_FragColor.b = fadeIn;
    */
//    gl_FragColor.a = 1.0;

//    if (distance >= fadeOutStart && distance <= fadeOutEnd) gl_FragColor.r = 1.0;
//    if (distance >= fadeInStart && distance <= fadeInEnd) gl_FragColor.g = 1.0;

    // Fade in over previous level
    gl_FragColor.a = fadeIn;

    // TODO: Fade last detail level edges to sky / background

/*
    if (distance >= fadeOutStart) {
        // Move outer part z-buffer slightly further away, to make sure next level is drawn on top.
        gl_FragDepth = gl_FragDepth * 1.01;

        // DEBUG
        // gl_FragColor.r = 1.0;
    }
    else {
        gl_FragDepth = gl_FragDepth;
    }
    */

    //gl_FragColor.a = 1.0;
}

