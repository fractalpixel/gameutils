
// Need camera pos for level of detail
//uniform vec3 u_cameraWorldPosition;
uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

attribute vec3 a_position;
attribute vec3 a_normal;

// For passing position of current pixel in the world to the fragment shader
varying vec3 v_worldPosition;
varying vec3 v_normal;

// Used for the logarithmic depth field
// TODO: Maybe pass these as uniform parameters?
const float C = 0.001;
const float Far = 100000.0;

void main() {


    v_normal = a_normal;

    vec4 pos = u_worldTrans * vec4(a_position, 1.0);

    // Pixels need their world position for level of detail things
    v_worldPosition = pos.xyz;

    gl_Position = u_projViewTrans * pos;

    // TODO: For some reason this differs from the value in the modified default shader..
    // Use a logarithmic depth value instead of the unfortunate z/w default in the OpenGL pipeline
    // See e.g. https://www.gamasutra.com/blogs/BranoKemen/20090812/85207/Logarithmic_Depth_Buffer.php  or  https://outerra.blogspot.com/2012/11/maximizing-depth-buffer-range-and.html
    gl_Position.z = log(C * gl_Position.z + 1.0) / log(C * Far + 1.0) * gl_Position.w; // Pre-multiply with gl_Position.w as the pipeline divides with gl_Position.w


}
