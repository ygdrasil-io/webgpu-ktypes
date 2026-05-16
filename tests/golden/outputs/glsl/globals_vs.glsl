#version 450 core
precision highp float;
precision highp int;

 int global_0;

vec4 vs_main(vec3 pos) {
    global_0 = (global_0 + 1);
    return vec4(pos, 1.0f);
}

void main() {
    gl_Position = vs_main(vec3(0.0));
}
